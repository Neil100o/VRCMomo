package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FriendActivityTrackerTest {
    @Test
    fun `bio diff reports removed and added lines`() {
        val diff = friendBioDiff("Hello\nOld line\nSame", "Hello\nNew line\nSame")

        assertEquals(listOf("Old line", "New line"), diff.map { it.text })
        assertEquals(listOf(false, true), diff.map { it.added })
    }

    @Test
    fun `same instance starts one observed meeting and accumulates only observed duration`() {
        val tracker = FriendActivityTracker()
        val instance = "wrld_home:12345~private(abc)"

        tracker.updateSelfInstance(instance, nowMillis = 1_000)
        tracker.observeFriends(
            friends = listOf(observation(location = instance, status = "active")),
            nowMillis = 2_000,
        )
        tracker.observeFriends(
            friends = listOf(observation(location = instance, status = "active")),
            nowMillis = 5_000,
        )
        tracker.updateSelfInstance("wrld_elsewhere:67890~private(def)", nowMillis = 8_000)

        val stats = tracker.snapshot.getValue("usr_friend")
        assertEquals(1, stats.meetingCount)
        assertEquals(6_000, stats.togetherDurationMillis)
        assertEquals(8_000, stats.lastSeenTogetherAtMillis)
        assertNull(stats.activeTogetherSinceMillis)
    }

    @Test
    fun `same world in different instances is not recorded as a meeting`() {
        val tracker = FriendActivityTracker()
        tracker.updateSelfInstance("wrld_home:11111~private(a)", nowMillis = 1_000)

        tracker.observeFriends(
            friends = listOf(observation(location = "wrld_home:22222~private(b)", status = "active")),
            nowMillis = 2_000,
        )

        val stats = tracker.snapshot.getValue("usr_friend")
        assertEquals(0, stats.meetingCount)
        assertEquals(0, stats.togetherDurationMillis)
    }

    @Test
    fun `first offline snapshot is a baseline while later transitions record timestamps`() {
        val tracker = FriendActivityTracker()
        tracker.observeFriends(
            friends = listOf(observation(location = "offline", status = "offline")),
            nowMillis = 1_000,
        )
        assertNull(tracker.snapshot.getValue("usr_friend").lastOfflineAtMillis)

        tracker.observeFriends(
            friends = listOf(observation(location = "private", status = "online")),
            nowMillis = 2_000,
        )
        tracker.observeFriends(
            friends = listOf(observation(location = "offline", status = "offline")),
            nowMillis = 3_000,
        )

        val stats = tracker.snapshot.getValue("usr_friend")
        assertEquals(2_000, stats.lastOnlineAtMillis)
        assertEquals(3_000, stats.lastOfflineAtMillis)
        assertEquals(3_000, stats.lastActivityAtMillis)
    }


    @Test
    fun `untrusted cached offline snapshot does not create a restart transition`() {
        val tracker = FriendActivityTracker(
            initialStats = mapOf(
                "usr_friend" to FriendActivityStats(
                    userId = "usr_friend",
                    lastObservedLocation = "wrld_home:123",
                    lastObservedStatus = "active",
                ),
            ),
        )

        tracker.observeFriends(
            friends = listOf(observation(location = "offline", status = "offline")),
            nowMillis = 1_000,
            isTrustedSnapshot = false,
        )
        tracker.observeFriends(
            friends = listOf(observation(location = "wrld_home:123", status = "active")),
            nowMillis = 2_000,
        )

        assertEquals("wrld_home:123", tracker.snapshot.getValue("usr_friend").lastObservedLocation)
        assertEquals("active", tracker.snapshot.getValue("usr_friend").lastObservedStatus)
        assertEquals(emptyList(), tracker.eventLog)
    }


    @Test
    fun `first trusted in-game observation establishes a baseline without inventing a location change`() {
        val tracker = FriendActivityTracker()

        tracker.observeFriends(
            friends = listOf(observation(location = "wrld_home:123", status = "active")),
            nowMillis = 1_000,
        )

        assertEquals(emptyList(), tracker.eventLog)
    }

    @Test
    fun `restored runtime meeting state is discarded rather than counting downtime`() {
        val tracker = FriendActivityTracker(
            initialStats = mapOf(
                "usr_friend" to FriendActivityStats(
                    userId = "usr_friend",
                    togetherDurationMillis = 42_000,
                    activeTogetherSinceMillis = 1_000,
                    activeTogetherInstanceId = "wrld_home:123",
                ),
            ),
        )

        tracker.updateSelfInstance("wrld_elsewhere:456", nowMillis = 100_000)
        tracker.observeFriends(
            friends = listOf(observation(location = "offline", status = "offline")),
            nowMillis = 100_000,
        )

        val stats = tracker.snapshot.getValue("usr_friend")
        assertEquals(42_000, stats.togetherDurationMillis)
        assertNull(stats.activeTogetherSinceMillis)
    }


    @Test
    fun `website activity ends a game presence session even when status is active`() {
        val tracker = FriendActivityTracker()
        tracker.observeFriends(
            friends = listOf(observation(location = "offline", status = "active")),
            nowMillis = 1_000,
        )
        tracker.observeFriends(
            friends = listOf(observation(location = "private", status = "active")),
            nowMillis = 2_000,
        )
        tracker.observeFriends(
            friends = listOf(observation(location = "offline", status = "active")),
            nowMillis = 3_000,
        )

        val stats = tracker.snapshot.getValue("usr_friend")
        assertEquals(2_000, stats.lastOnlineAtMillis)
        assertEquals(3_000, stats.lastOfflineAtMillis)
    }
    @Test
    fun `legacy offline and ask me casing does not create a new activity event`() {
        val tracker = FriendActivityTracker(
            initialStats = mapOf(
                "usr_friend" to FriendActivityStats(
                    userId = "usr_friend",
                    lastObservedLocation = "Offline",
                    lastObservedStatus = "AskMe",
                ),
            ),
        )

        assertTrue(
            !tracker.observeFriends(
                listOf(observation(location = "offline", status = "ask me")),
                nowMillis = 1_000L,
            ),
        )
        assertTrue(tracker.eventLog.isEmpty())
    }

    @Test
    fun `status description whitespace does not create a duplicate change`() {
        val tracker = FriendActivityTracker(
            initialStats = mapOf(
                "usr_friend" to FriendActivityStats(
                    userId = "usr_friend",
                    lastObservedLocation = "offline",
                    lastObservedStatus = "offline · 休息",
                ),
            ),
        )

        tracker.observeFriends(
            listOf(
                FriendActivityObservation(
                    userId = "usr_friend",
                    location = "offline",
                    status = "Offline",
                    statusDescription = "  休息  ",
                    lastActivityAtMillis = null,
                ),
            ),
            nowMillis = 2_000L,
        )

        assertTrue(tracker.eventLog.isEmpty())
    }

    @Test
    fun `legacy no-op status events are removed when archive is loaded`() {
        val tracker = FriendActivityTracker(
            initialEvents = listOf(
                FriendActivityEvent(
                    userId = "usr_friend",
                    displayName = "Friend",
                    type = FriendActivityEventType.StatusChanged,
                    occurredAtMillis = 1_000L,
                    previousValue = "Offline ·  休息",
                    currentValue = "offline·休息",
                ),
            ),
        )

        assertTrue(tracker.eventLog.isEmpty())
    }

    private fun observation(
        location: String,
        status: String,
    ) = FriendActivityObservation(
        userId = "usr_friend",
        location = location,
        status = status,
        lastActivityAtMillis = null,
    )
}

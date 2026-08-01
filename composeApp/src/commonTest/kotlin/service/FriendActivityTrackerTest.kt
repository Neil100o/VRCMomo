package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

package io.github.vrcmteam.vrcm.storage

import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import io.github.vrcmteam.vrcm.storage.data.FriendActivityDiffLine
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEventType
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RoomFriendActivityMirrorTest {
    @Test
    fun `mirrored snapshot preserves momo specific activity fields`() = runTest {
        val dao = FakeSnapshotDao()
        val indexDao = FakeIndexDao()
        val mirror = RoomFriendActivityMirror(dao, indexDao)
        val source = FriendActivityCache(
            statsByFriendId = mapOf(
                "usr_friend" to FriendActivityStats(
                    userId = "usr_friend",
                    meetingCount = 4,
                    togetherDurationMillis = 12_345L,
                    lastObservedStatus = "active",
                ),
            ),
            schemaVersion = FriendActivityCache.CURRENT_SCHEMA_VERSION,
            importedVrcxEventKeys = setOf("vrcx:1"),
            activityEvents = listOf(
                FriendActivityEvent(
                    userId = "usr_friend",
                    displayName = "Friend",
                    type = FriendActivityEventType.ProfileChanged,
                    occurredAtMillis = 123L,
                    diffLines = listOf(FriendActivityDiffLine(added = true, text = "new bio")),
                ),
            ),
        )

        mirror.mirror("usr_owner", source)
        val restored = assertNotNull(mirror.load("usr_owner"))

        assertEquals(source, restored)
        assertEquals(1, indexDao.events.size)
        assertEquals(4, indexDao.summaries.getValue("usr_friend").meetingCount)
    }

    private class FakeIndexDao : FriendActivityIndexDao {
        val summaries = mutableMapOf<String, FriendActivitySummaryEntity>()
        val events = mutableListOf<FriendActivityEventEntity>()

        override suspend fun deleteSummaries(ownerUserId: String) {
            summaries.entries.removeAll { it.value.ownerUserId == ownerUserId }
        }

        override suspend fun deleteEvents(ownerUserId: String) {
            events.removeAll { it.ownerUserId == ownerUserId }
        }

        override suspend fun deleteAllSummaries() {
            summaries.clear()
        }

        override suspend fun deleteAllEvents() {
            events.clear()
        }

        override suspend fun insertEvents(events: List<FriendActivityEventEntity>) {
            this.events += events
        }

        override suspend fun upsertSummaries(summaries: List<FriendActivitySummaryEntity>) {
            summaries.forEach { this.summaries[it.friendUserId] = it }
        }
    }

    private class FakeSnapshotDao : FriendActivitySnapshotDao {
        private val snapshots = mutableMapOf<String, FriendActivitySnapshotEntity>()

        override suspend fun snapshot(ownerUserId: String): FriendActivitySnapshotEntity? = snapshots[ownerUserId]

        override suspend fun upsert(snapshot: FriendActivitySnapshotEntity) {
            snapshots[snapshot.ownerUserId] = snapshot
        }

        override suspend fun delete(ownerUserId: String) {
            snapshots.remove(ownerUserId)
        }

        override suspend fun deleteAll() {
            snapshots.clear()
        }
    }
}

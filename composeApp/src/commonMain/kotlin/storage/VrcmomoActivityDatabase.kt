package io.github.vrcmteam.vrcm.storage

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.serialization.json.Json

/**
 * SQLite foundation for VRCMomo's account-scoped activity archive.
 *
 * The first migration keeps the existing cache payload intact in one durable row per account.
 * This deliberately preserves every VRCMomo-only field (timeline diff lines, VRCX keys,
 * relationship statistics and future extensions) before later versions normalise it into tables.
 */
@Entity(tableName = "momo_friend_activity_snapshots")
internal data class FriendActivitySnapshotEntity(
    @PrimaryKey val ownerUserId: String,
    val schemaVersion: Int,
    val payloadJson: String,
    val updatedAtMillis: Long,
)

@Dao
internal interface FriendActivitySnapshotDao {
    @Query("SELECT * FROM momo_friend_activity_snapshots WHERE ownerUserId = :ownerUserId")
    suspend fun snapshot(ownerUserId: String): FriendActivitySnapshotEntity?

    @Upsert
    suspend fun upsert(snapshot: FriendActivitySnapshotEntity)

    @Query("DELETE FROM momo_friend_activity_snapshots WHERE ownerUserId = :ownerUserId")
    suspend fun delete(ownerUserId: String)

    @Query("DELETE FROM momo_friend_activity_snapshots")
    suspend fun deleteAll()
}

/** Queryable projection of Momo's rich per-friend statistics. */
@Entity(
    tableName = "momo_friend_activity_summaries",
    primaryKeys = ["ownerUserId", "friendUserId"],
)
internal data class FriendActivitySummaryEntity(
    val ownerUserId: String,
    val friendUserId: String,
    val lastSeenTogetherAtMillis: Long?,
    val meetingCount: Int,
    val togetherDurationMillis: Long,
    val lastOnlineAtMillis: Long?,
    val lastOfflineAtMillis: Long?,
    val lastActivityAtMillis: Long?,
)

/** Queryable timeline index. The JSON payload preserves Diff lines and future-compatible fields. */
@Entity(
    tableName = "momo_friend_activity_events",
    indices = [Index(value = ["ownerUserId", "occurredAtMillis"]), Index(value = ["ownerUserId", "friendUserId", "occurredAtMillis"])],
)
internal data class FriendActivityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ownerUserId: String,
    val friendUserId: String,
    val occurredAtMillis: Long,
    val eventType: String,
    val payloadJson: String,
)

@Dao
internal interface FriendActivityIndexDao {
    @Query("DELETE FROM momo_friend_activity_summaries WHERE ownerUserId = :ownerUserId")
    suspend fun deleteSummaries(ownerUserId: String)

    @Query("DELETE FROM momo_friend_activity_events WHERE ownerUserId = :ownerUserId")
    suspend fun deleteEvents(ownerUserId: String)

    @Query("DELETE FROM momo_friend_activity_summaries")
    suspend fun deleteAllSummaries()

    @Query("DELETE FROM momo_friend_activity_events")
    suspend fun deleteAllEvents()

    @Insert
    suspend fun insertEvents(events: List<FriendActivityEventEntity>)

    @Upsert
    suspend fun upsertSummaries(summaries: List<FriendActivitySummaryEntity>)
}

private fun FriendActivityStats.toEntity(ownerUserId: String) = FriendActivitySummaryEntity(
    ownerUserId = ownerUserId,
    friendUserId = userId,
    lastSeenTogetherAtMillis = lastSeenTogetherAtMillis,
    meetingCount = meetingCount,
    togetherDurationMillis = togetherDurationMillis,
    lastOnlineAtMillis = lastOnlineAtMillis,
    lastOfflineAtMillis = lastOfflineAtMillis,
    lastActivityAtMillis = lastActivityAtMillis,
)

private fun FriendActivityEvent.toEntity(ownerUserId: String, json: Json) = FriendActivityEventEntity(
    ownerUserId = ownerUserId,
    friendUserId = userId,
    occurredAtMillis = occurredAtMillis,
    eventType = type.name,
    payloadJson = json.encodeToString(FriendActivityEvent.serializer(), this),
)

@Database(
    entities = [
        FriendActivitySnapshotEntity::class,
        FriendActivitySummaryEntity::class,
        FriendActivityEventEntity::class,
    ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
@ConstructedBy(VrcmomoActivityDatabaseConstructor::class)
internal abstract class VrcmomoActivityDatabase : RoomDatabase() {
    abstract fun friendActivitySnapshotDao(): FriendActivitySnapshotDao
    abstract fun friendActivityIndexDao(): FriendActivityIndexDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect object VrcmomoActivityDatabaseConstructor : RoomDatabaseConstructor<VrcmomoActivityDatabase> {
    override fun initialize(): VrcmomoActivityDatabase
}

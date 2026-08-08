package io.github.vrcmteam.vrcm.storage

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

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

@Database(entities = [FriendActivitySnapshotEntity::class], version = 1, exportSchema = true)
@ConstructedBy(VrcmomoActivityDatabaseConstructor::class)
internal abstract class VrcmomoActivityDatabase : RoomDatabase() {
    abstract fun friendActivitySnapshotDao(): FriendActivitySnapshotDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect object VrcmomoActivityDatabaseConstructor : RoomDatabaseConstructor<VrcmomoActivityDatabase> {
    override fun initialize(): VrcmomoActivityDatabase
}

package io.github.vrcmteam.vrcm.storage

import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Staged SQLite copy of VRCMomo's existing activity archive.
 *
 * JSON remains the read source in this release, so an interrupted or failed SQLite write can
 * never hide historical data. Once the mirror has shipped and migration checks are in place,
 * the same rows can become the primary source without losing Momo-specific fields.
 */
@OptIn(ExperimentalTime::class)
class RoomFriendActivityMirror internal constructor(
    private val dao: FriendActivitySnapshotDao,
) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun mirror(ownerUserId: String, cache: FriendActivityCache) {
        runCatching {
            dao.upsert(
                FriendActivitySnapshotEntity(
                    ownerUserId = ownerUserId,
                    schemaVersion = cache.schemaVersion,
                    payloadJson = json.encodeToString(FriendActivityCache.serializer(), cache),
                    updatedAtMillis = Clock.System.now().toEpochMilliseconds(),
                ),
            )
        }
    }

    suspend fun load(ownerUserId: String): FriendActivityCache? = runCatching {
        dao.snapshot(ownerUserId)?.payloadJson?.let { payload ->
            json.decodeFromString(FriendActivityCache.serializer(), payload)
        }
    }.getOrNull()

    /** Used only by the existing synchronous account activation and delete paths. */
    fun loadBlocking(ownerUserId: String): FriendActivityCache? =
        runBlocking(Dispatchers.IO) { load(ownerUserId) }

    fun deleteBlocking(ownerUserId: String) {
        runBlocking(Dispatchers.IO) { runCatching { dao.delete(ownerUserId) } }
    }

    fun deleteAllBlocking() {
        runBlocking(Dispatchers.IO) { runCatching { dao.deleteAll() } }
    }
}

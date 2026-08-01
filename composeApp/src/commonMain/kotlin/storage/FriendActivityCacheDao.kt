package io.github.vrcmteam.vrcm.storage

import com.russhwolf.settings.Settings
import io.github.vrcmteam.vrcm.AppPlatform
import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Durable, account-scoped friend activity storage.
 *
 * Production uses the app-private data directory. The Settings value is retained only as a
 * one-time migration source and as a compatibility fallback for lightweight common tests.
 */
class FriendActivityCacheDao(
    private val settings: Settings,
    appPlatform: AppPlatform? = null,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    storageRoot: Path? = null,
) {
    private val lock = Any()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val storageDirectory: Path? = storageRoot ?: appPlatform
        ?.persistentDataDirectory
        ?.toPath()
        ?.resolve("VRCMomo")
        ?.resolve("friend-activity")

    private fun legacyKey(ownerUserId: String) =
        "${DaoKeys.FriendActivity.KEY_PREFIX}.$ownerUserId"

    private fun accountFile(ownerUserId: String): Path? = storageDirectory?.resolve(
        ownerUserId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json",
    )

    private fun eventFile(ownerUserId: String, index: Int): Path? = storageDirectory?.resolve(
        ownerUserId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".events.$index.json",
    )

    fun load(ownerUserId: String): FriendActivityCache? = synchronized(lock) {
        val file = accountFile(ownerUserId)
        if (file != null && fileSystem.exists(file)) {
            val raw = runCatching { fileSystem.read(file) { readUtf8() } }.getOrNull()
            decode(raw)?.let { cached ->
                val migrated = migrateLoadedCache(file, raw, cached)
                return@synchronized migrated.copy(
                    activityEvents = (migrated.activityEvents + loadEventChunks(ownerUserId, migrated.eventChunkCount))
                        .sortedByDescending(FriendActivityEvent::occurredAtMillis),
                )
            }
        }

        val legacyRaw = settings.getStringOrNull(legacyKey(ownerUserId))
        val legacy = decode(legacyRaw) ?: return@synchronized null
        val migrated = migrate(legacy)
        if (file != null && migrated != null && writeFile(file, migrated)) {
            // Keep the Settings value until the durable file write succeeds.
            settings.remove(legacyKey(ownerUserId))
        }
        migrated ?: legacy
    }

    fun save(ownerUserId: String, cache: FriendActivityCache) = synchronized(lock) {
        val file = accountFile(ownerUserId)
        // An older build must never overwrite a record produced by a newer schema.
        if (storedSchemaVersion(file, ownerUserId)?.let { it > FriendActivityCache.CURRENT_SCHEMA_VERSION } == true) {
            return@synchronized
        }
        val normalized = migrate(cache) ?: return@synchronized
        if (file == null) {
            settings.putString(legacyKey(ownerUserId), encode(normalized))
            return@synchronized
        }
        val chunks = normalized.activityEvents.chunked(EVENTS_PER_FILE)
        if (!writeEventChunks(ownerUserId, chunks)) return@synchronized
        val primary = normalized.copy(activityEvents = emptyList(), eventChunkCount = chunks.size)
        if (writeFile(file, primary)) {
            removeStaleEventChunks(ownerUserId, chunks.size)
            settings.remove(legacyKey(ownerUserId))
        }
    }

    fun clear(ownerUserId: String) = synchronized(lock) {
        accountFile(ownerUserId)?.let { file ->
            runCatching { if (fileSystem.exists(file)) fileSystem.delete(file) }
            runCatching {
                val temp = (file.toString() + ".tmp").toPath()
                if (fileSystem.exists(temp)) fileSystem.delete(temp)
            }
        }
        removeStaleEventChunks(ownerUserId, keepCount = 0)
        settings.remove(legacyKey(ownerUserId))
    }

    fun clearAll() = synchronized(lock) {
        storageDirectory?.let { directory ->
            runCatching {
                if (fileSystem.exists(directory)) {
                    fileSystem.list(directory)
                        .filter { it.name.endsWith(".json") || it.name.endsWith(".json.tmp") }
                        .forEach { fileSystem.delete(it) }
                }
            }
        }
        settings.keys
            .filter { it.startsWith("${DaoKeys.FriendActivity.KEY_PREFIX}.") }
            .forEach(settings::remove)
    }

    internal fun storageFile(ownerUserId: String): Path? = accountFile(ownerUserId)

    private fun migrateLoadedCache(
        file: Path,
        raw: String?,
        cache: FriendActivityCache,
    ): FriendActivityCache {
        val migrated = migrate(cache) ?: return cache
        if (migrated.schemaVersion != cache.schemaVersion) {
            backupBeforeMigration(file, raw, cache.schemaVersion)
            writeFile(file, migrated)
        }
        return migrated
    }

    /**
     * Each schema step is explicit. Never remove or rename a stored field without adding a step
     * that carries its value forward.
     */
    private fun migrate(cache: FriendActivityCache): FriendActivityCache? {
        if (cache.schemaVersion > FriendActivityCache.CURRENT_SCHEMA_VERSION) return null
        var migrated = cache
        while (migrated.schemaVersion < FriendActivityCache.CURRENT_SCHEMA_VERSION) {
            migrated = when (migrated.schemaVersion) {
                FriendActivityCache.LEGACY_SCHEMA_VERSION -> migrated.copy(
                    schemaVersion = 2,
                )
                2 -> migrated.copy(
                    schemaVersion = 3,
                )
                3 -> migrated.copy(
                    schemaVersion = 4,
                )
                4 -> migrated.copy(
                    schemaVersion = 5,
                )
                5 -> migrated.copy(
                    schemaVersion = FriendActivityCache.CURRENT_SCHEMA_VERSION,
                )
                else -> return null
            }
        }
        return migrated
    }

    private fun storedSchemaVersion(file: Path?, ownerUserId: String): Int? {
        val raw = when {
            file != null && fileSystem.exists(file) ->
                runCatching { fileSystem.read(file) { readUtf8() } }.getOrNull()
            else -> settings.getStringOrNull(legacyKey(ownerUserId))
        }
        return decode(raw)?.schemaVersion
    }

    private fun backupBeforeMigration(file: Path, raw: String?, sourceSchema: Int) {
        if (raw == null) return
        val backup = (file.toString() + ".v$sourceSchema.backup").toPath()
        if (fileSystem.exists(backup)) return
        runCatching {
            fileSystem.write(backup) { writeUtf8(raw) }
        }
    }

    private fun writeFile(file: Path, cache: FriendActivityCache): Boolean = runCatching {
        file.parent?.let(fileSystem::createDirectories)
        val temp = (file.toString() + ".tmp").toPath()
        fileSystem.write(temp) { writeUtf8(encode(cache)) }
        runCatching { fileSystem.atomicMove(temp, file) }.getOrElse {
            if (fileSystem.exists(file)) fileSystem.delete(file)
            fileSystem.atomicMove(temp, file)
        }
        true
    }.getOrDefault(false)

    private fun encode(cache: FriendActivityCache): String =
        json.encodeToString(FriendActivityCache.serializer(), cache)

    private fun decode(raw: String?): FriendActivityCache? = raw?.let {
        runCatching { json.decodeFromString<FriendActivityCache>(it) }.getOrNull()
    }

    private fun loadEventChunks(ownerUserId: String, count: Int): List<FriendActivityEvent> =
        (0 until count).flatMap { index ->
            eventFile(ownerUserId, index)
                ?.takeIf(fileSystem::exists)
                ?.let { file ->
                    runCatching {
                        json.decodeFromString(ListSerializer(FriendActivityEvent.serializer()), fileSystem.read(file) { readUtf8() })
                    }.getOrDefault(emptyList())
                }.orEmpty()
        }

    private fun writeEventChunks(ownerUserId: String, chunks: List<List<FriendActivityEvent>>): Boolean = runCatching {
        chunks.forEachIndexed { index, events ->
            val file = requireNotNull(eventFile(ownerUserId, index))
            file.parent?.let(fileSystem::createDirectories)
            val temp = (file.toString() + ".tmp").toPath()
            fileSystem.write(temp) { writeUtf8(json.encodeToString(ListSerializer(FriendActivityEvent.serializer()), events)) }
            runCatching { fileSystem.atomicMove(temp, file) }.getOrElse {
                if (fileSystem.exists(file)) fileSystem.delete(file)
                fileSystem.atomicMove(temp, file)
            }
        }
        true
    }.getOrDefault(false)

    private fun removeStaleEventChunks(ownerUserId: String, keepCount: Int) {
        val directory = storageDirectory ?: return
        val prefix = ownerUserId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".events."
        runCatching {
            if (fileSystem.exists(directory)) {
                fileSystem.list(directory)
                    .filter { file -> file.name.startsWith(prefix) }
                    .filter { file -> file.name.substringAfter(prefix).substringBefore('.').toIntOrNull()?.let { it >= keepCount } == true }
                    .forEach(fileSystem::delete)
            }
        }
    }

    private companion object {
        const val EVENTS_PER_FILE = 500
    }
}

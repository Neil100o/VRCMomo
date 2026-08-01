package io.github.vrcmteam.vrcm.storage

import com.russhwolf.settings.Settings
import io.github.vrcmteam.vrcm.AppPlatform
import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
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

    fun load(ownerUserId: String): FriendActivityCache? = synchronized(lock) {
        val file = accountFile(ownerUserId)
        if (file != null && fileSystem.exists(file)) {
            decode(runCatching { fileSystem.read(file) { readUtf8() } }.getOrNull())
                ?.let { return@synchronized it }
        }

        val legacy = decode(settings.getStringOrNull(legacyKey(ownerUserId)))
            ?: return@synchronized null
        if (file != null && writeFile(file, legacy)) {
            settings.remove(legacyKey(ownerUserId))
        }
        legacy
    }

    fun save(ownerUserId: String, cache: FriendActivityCache) = synchronized(lock) {
        val file = accountFile(ownerUserId)
        if (file == null) {
            settings.putString(legacyKey(ownerUserId), encode(cache))
            return@synchronized
        }
        if (writeFile(file, cache)) {
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
}

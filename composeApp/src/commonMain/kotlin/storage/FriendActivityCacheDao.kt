package io.github.vrcmteam.vrcm.storage

import com.russhwolf.settings.Settings
import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import kotlinx.serialization.json.Json

class FriendActivityCacheDao(
    private val settings: Settings,
) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun key(ownerUserId: String) = "${DaoKeys.FriendActivity.KEY_PREFIX}.$ownerUserId"

    fun load(ownerUserId: String): FriendActivityCache? =
        settings.getStringOrNull(key(ownerUserId))?.let { raw ->
            runCatching { json.decodeFromString<FriendActivityCache>(raw) }.getOrNull()
        }

    fun save(ownerUserId: String, cache: FriendActivityCache) {
        settings.putString(key(ownerUserId), json.encodeToString(FriendActivityCache.serializer(), cache))
    }

    fun clear(ownerUserId: String) {
        settings.remove(key(ownerUserId))
    }

    fun clearAll() {
        settings.keys
            .filter { it.startsWith("${DaoKeys.FriendActivity.KEY_PREFIX}.") }
            .forEach(settings::remove)
    }
}

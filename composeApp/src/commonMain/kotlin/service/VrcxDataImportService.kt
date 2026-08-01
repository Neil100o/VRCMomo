package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.attributes.FavoriteType
import io.github.vrcmteam.vrcm.storage.FavoriteLocalDao
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Safe, file-based bridge for VRCX exports.
 *
 * VRCX's documented friend-list export is either `{ "friends": ["usr_..."] }`
 * JSON or a CSV containing a `UserID` column.  The import deliberately accepts
 * only user IDs and writes them to VRCMomo's local friend-favorites bucket. It
 * never imports VRCX's database, account cookies, passwords, or tokens.
 */
class VrcxDataImportService(
    private val favoriteLocalDao: FavoriteLocalDao,
) {
    fun previewFriendImport(content: String): VrcxFriendImportPreview {
        val friendIds = parseVrcxFriendIds(content)
        return VrcxFriendImportPreview(friendIds)
    }

    fun importFriendFavorites(preview: VrcxFriendImportPreview): Int {
        val existing = favoriteLocalDao.load(FavoriteType.Friend).toSet()
        val merged = existing + preview.friendIds
        favoriteLocalDao.save(FavoriteType.Friend, merged.sorted())
        return merged.size - existing.size
    }
}

data class VrcxFriendImportPreview(
    val friendIds: List<String>,
) {
    val isEmpty: Boolean get() = friendIds.isEmpty()
}

internal fun parseVrcxFriendIds(content: String): List<String> {
    val fromJson = runCatching {
        val root = Json.parseToJsonElement(content)
        val friends = (root as? JsonObject)?.get("friends") as? JsonArray
        friends.orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    }.getOrNull()

    val candidates = fromJson ?: USER_ID_PATTERN.findAll(content).map { it.value }.toList()
    return candidates
        .asSequence()
        .map(String::trim)
        .filter { USER_ID_PATTERN.matches(it) }
        .distinct()
        .toList()
}

private val USER_ID_PATTERN = Regex("usr_[0-9a-fA-F-]{8,}")

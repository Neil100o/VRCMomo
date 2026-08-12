package io.github.vrcmteam.vrcm.network.api.notification.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class NotificationDataV2(
    @SerialName("created_at")
    val createdAt: String,
    /**
     * VRChat normally returns this as an object, but older notification responses can wrap the
     * same object as a JSON string. Keep the raw element so both variants remain readable.
     */
    val details: JsonElement,
    val id: String,
    val message: String,
    val seen: Boolean,
    val senderUserId: String,
    val receiverUserId: String?,
    /** Notification types are server-extensible. */
    val type: String,
) {
    fun boopDetail(json: Json = notificationDetailsJson): NotificationDetailBoop? {
        if (type != "boop") return null
        val detailRoot = details.unwrapJsonString(json) ?: return null
        return detailRoot.boopCandidates()
            .mapNotNull { candidate -> candidate.toBoopDetailOrNull() }
            .firstOrNull()
    }

    companion object {
        private val notificationDetailsJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}

private fun JsonElement.unwrapJsonString(json: Json): JsonElement? =
    if (this is JsonPrimitive && isString) {
        runCatching { json.parseToJsonElement(content) }.getOrNull()
    } else {
        this
    }

/**
 * The REST and legacy notification feeds have used both a direct details object and small
 * wrapper objects. Only inspect the known Boop wrappers rather than guessing arbitrary IDs.
 */
private fun JsonElement.boopCandidates(): Sequence<JsonObject> = sequence {
    val root = this@boopCandidates as? JsonObject ?: return@sequence
    yield(root)
    listOf("boop", "boopEmoji", "emoji", "data").forEach { key ->
        (root[key] as? JsonObject)?.let { candidate -> yield(candidate) }
    }
}

private fun JsonObject.toBoopDetailOrNull(): NotificationDetailBoop? {
    fun stringValue(vararg keys: String): String? = keys
        .asSequence()
        .mapNotNull { key -> (this[key] as? JsonPrimitive)?.contentOrNull }
        .firstOrNull { it.isNotBlank() }
    fun intValue(vararg keys: String): Int? = keys
        .asSequence()
        .mapNotNull { key -> (this[key] as? JsonPrimitive)?.intOrNull }
        .firstOrNull()

    val emojiId = stringValue("emojiId", "emoji_id", "id")
    val emojiVersion = intValue("emojiVersion", "emoji_version", "version")
    val inventoryItemId = stringValue("inventoryItemId", "inventory_item_id")
    return NotificationDetailBoop(emojiId, emojiVersion, inventoryItemId)
        .takeIf { it.emojiId != null || it.inventoryItemId != null }
}

package io.github.vrcmteam.vrcm.network.api.notification.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Serializable
data class NotificationDataV2(
    @SerialName("created_at")
    val createdAt: String,
    /** JSON-encoded NotificationDetail; Boop details are decoded by [boopDetail]. */
    val details: String,
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
        return runCatching {
            json.decodeFromString<NotificationDetailBoop>(details)
        }.getOrNull()
    }

    companion object {
        private val notificationDetailsJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}

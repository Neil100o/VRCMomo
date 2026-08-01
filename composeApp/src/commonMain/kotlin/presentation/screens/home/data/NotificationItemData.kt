package io.github.vrcmteam.vrcm.presentation.screens.home.data

import io.github.vrcmteam.vrcm.network.api.notification.data.NotificationData
import io.github.vrcmteam.vrcm.network.api.notification.data.NotificationDataV2

data class NotificationItemData(
    val id: String,
    val imageUrl: String,
    val title: String?,
    val message: String,
    val createdAt: String,
    val senderUserId: String,
    val link: String?,
    val type: String,
    val actions: List<ActionData>,
    /** Whether VRChat has acknowledged this notification as seen. */
    val seen: Boolean? = null,
    /** The selected emoji metadata for a received VRChat Boop, when supplied by the API. */
    val boopEmojiId: String? = null,
    val boopEmojiVersion: Int? = null,
    val boopInventoryItemId: String? = null,
) {
    /** The notification sender used by sender-specific actions such as opening a profile or replying to a Boop. */
    val senderId: String?
        get() = senderUserId.trim().takeIf { it.isNotEmpty() }

    /** A readable label for built-in Boop reactions. Custom inventory reactions remain custom. */
    val boopEmojiLabel: String?
        get() = boopEmojiId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { emojiId ->
                if (emojiId.startsWith("default_")) {
                    emojiId.removePrefix("default_")
                        .split("_")
                        .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
                } else {
                    "Custom emoji"
                }
            }
            ?: boopInventoryItemId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { "Custom emoji" }

    /** The VRChat user targeted by a `user:usr_...` notification link. */
    val linkedUserId: String?
        get() = link
            ?.takeIf { it.startsWith("user:") }
            ?.removePrefix("user:")
            ?.takeIf { it.isNotBlank() }

    data class ActionData(
        val data: String,
        val type: String,
        val icon: String = "",
    )

    constructor(n: NotificationDataV2) : this(
        id = n.id,
        imageUrl = "",
        title = null,
        message = n.message,
        createdAt = n.createdAt,
        senderUserId = n.senderUserId,
        link = "user:${n.senderUserId}",
        type = n.type,
        actions = emptyList(),
        seen = n.seen,
        boopEmojiId = n.boopDetail()?.emojiId,
        boopEmojiVersion = n.boopDetail()?.emojiVersion,
        boopInventoryItemId = n.boopDetail()?.inventoryItemId,
    )

    constructor(n: NotificationData) : this(
        id = n.id,
        imageUrl = n.imageUrl.orEmpty(),
        title = n.title,
        message = n.message,
        createdAt = n.createdAt,
        senderUserId = n.senderUserId.orEmpty(),
        link = n.link,
        type = n.type,
        actions = n.responses.map { responses ->
            ActionData(
                data = responses.responseData,
                type = responses.type,
                icon = responses.icon,
            )
        },
        seen = n.seen,
        boopEmojiId = n.data.emojiId,
        boopEmojiVersion = n.data.emojiVersion,
        boopInventoryItemId = n.data.inventoryItemId,
    )

}

/** Boops that VRChat has not yet acknowledged are safe to surface after a cold app start. */
internal val NotificationItemData.isUnreadBoop: Boolean
    get() = type == "boop" && seen == false

internal enum class NotificationResponseTarget {
    BOOP_USER_API,
    NOTIFICATION_API,
}

internal fun NotificationItemData.responseTarget(
    action: NotificationItemData.ActionData,
): NotificationResponseTarget =
    if (type == "boop" && action.icon.equals("reply", ignoreCase = true)) {
        NotificationResponseTarget.BOOP_USER_API
    } else {
        NotificationResponseTarget.NOTIFICATION_API
    }

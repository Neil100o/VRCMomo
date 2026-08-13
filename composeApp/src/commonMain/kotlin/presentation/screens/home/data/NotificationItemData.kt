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
        get() = normalizedBoopEmojiId
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

    /** A compact, offline-safe visual for the built-in Boop reactions. */
    val boopEmojiGlyph: String
        get() = when (normalizedBoopEmojiId) {
            "default_angry" -> "😠"
            "default_arrowpoint" -> "👉"
            "default_bats" -> "🦇"
            "default_beachball" -> "🏖"
            "default_beer" -> "🍺"
            "default_blushing" -> "😊"
            "default_boo", "default_spooky_ghost" -> "👻"
            "default_broken_heart" -> "💔"
            "default_candy", "default_candy_cane" -> "🍬"
            "default_candy_corn" -> "🌽"
            "default_cantsee" -> "🙈"
            "default_champagne" -> "🍾"
            "default_cloud" -> "☁"
            "default_coal" -> "●"
            "default_confetti" -> "🎉"
            "default_crying" -> "😢"
            "default_exclamation" -> "❗"
            "default_fire" -> "🔥"
            "default_frown" -> "☹"
            "default_gift", "default_gifts" -> "🎁"
            "default_gingerbread" -> "🍪"
            "default_go" -> "🟢"
            "default_hang_ten" -> "🤙"
            "default_hand_wave" -> "👋"
            "default_heart", "default_in_love", "default_kiss" -> "❤"
            "default_hourglass" -> "⌛"
            "default_ice_cream" -> "🍦"
            "default_jack_o_lantern" -> "🎃"
            "default_keyboard" -> "⌨"
            "default_laugh" -> "😂"
            "default_life_ring" -> "⭕"
            "default_mistletoe" -> "🌿"
            "default_money" -> "💰"
            "default_music_note" -> "🎵"
            "default_noheadphones" -> "🎧"
            "default_nomic" -> "🎤"
            "default_pineapple" -> "🍍"
            "default_pizza" -> "🍕"
            "default_portal" -> "🌀"
            "default_question" -> "❓"
            "default_shush" -> "🤫"
            "default_skull" -> "💀"
            "default_smile" -> "🙂"
            "default_snowball", "default_snow_fall" -> "❄"
            "default_splash" -> "💦"
            "default_stoic" -> "😐"
            "default_stop" -> "⛔"
            "default_sun_lotion" -> "☀"
            "default_sunglasses", "default_neon_shades" -> "😎"
            "default_thinking" -> "🤔"
            "default_thumbs_down" -> "👎"
            "default_thumbs_up" -> "👍"
            "default_tomato" -> "🍅"
            "default_zzz" -> "💤"
            null -> "✦"
            else -> "✦"
        }

    /** VRChat clients have historically sent both `default_heart` and display-style `Heart`. */
    private val normalizedBoopEmojiId: String?
        get() = boopEmojiId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase()
            ?.replace(' ', '_')
            ?.replace('-', '_')
            ?.let { id ->
                if (id.startsWith("default_")) id
                else if (id in knownDefaultBoopIds) "default_$id"
                else id
            }

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
        boopEmojiId = n.details?.emojiId ?: n.data.emojiId,
        boopEmojiVersion = n.details?.emojiVersion ?: n.data.emojiVersion,
        boopInventoryItemId = n.details?.inventoryItemId ?: n.data.inventoryItemId,
    )

}

private val knownDefaultBoopIds = setOf(
    "angry", "arrowpoint", "bats", "beachball", "beer", "blushing", "boo", "broken_heart",
    "candy", "candy_cane", "candy_corn", "cantsee", "champagne", "cloud", "coal", "confetti",
    "crying", "drink", "exclamation", "fire", "frown", "gift", "gifts", "gingerbread", "go",
    "hand_wave", "hang_ten", "heart", "hourglass", "ice_cream", "in_love", "jack_o_lantern",
    "keyboard", "kiss", "laugh", "life_ring", "mistletoe", "money", "music_note", "neon_shades",
    "noheadphones", "nomic", "pineapple", "pizza", "portal", "question", "shush", "skull", "smile",
    "snow_fall", "snowball", "splash", "spooky_ghost", "stoic", "stop", "sun_lotion", "sunglasses",
    "thinking", "thumbs_down", "thumbs_up", "tomato", "tongue_out", "web", "wow", "zzz",
)

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

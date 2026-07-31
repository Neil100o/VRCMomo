package io.github.vrcmteam.vrcm.network.api.notification.data

import kotlinx.serialization.Serializable

/**
 * The REST V2 notification endpoint returns `details` as a JSON-encoded string.
 * For Boop notifications this is the selected built-in/custom reaction metadata.
 */
@Serializable
data class NotificationDetailBoop(
    val emojiId: String? = null,
    val emojiVersion: Int? = null,
    val inventoryItemId: String? = null,
)

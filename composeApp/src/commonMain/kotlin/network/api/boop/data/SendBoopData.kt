package io.github.vrcmteam.vrcm.network.api.boop.data

import kotlinx.serialization.Serializable

@Serializable
data class SendBoopData(
    val emojiId: String? = null,
    val emojiVersion: Int? = null,
    val inventoryItemId: String? = null,
)

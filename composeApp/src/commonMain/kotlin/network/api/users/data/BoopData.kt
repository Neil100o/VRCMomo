package io.github.vrcmteam.vrcm.network.api.users.data

import kotlinx.serialization.Serializable

@Serializable
data class BoopData(
    val emojiId: String? = null,
    val emojiVersion: Int? = null,
    val inventoryItemId: String? = null,
)

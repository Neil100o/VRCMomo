package io.github.vrcmteam.vrcm.network.api.groups.data

import kotlinx.serialization.Serializable

@Serializable
data class MyMember(
    val acceptedByDisplayName: String? = null,
    val acceptedById: String? = null,
    val bannedAt: String? = null,
    val createdAt: String = "",
    val groupId: String = "",
    val has2FA: Boolean = false,
    val hasJoinedFromPurchase: Boolean = false,
    val id: String = "",
    val isRepresenting: Boolean = false,
    val isSubscribedToAnnouncements: Boolean = false,
    val isSubscribedToEventAnnouncements: Boolean = false,
    val joinedAt: String = "",
    val lastPostReadAt: String? = null,
    val mRoleIds: List<String> = emptyList(),
    val managerNotes: String = "",
    val membershipStatus: String = "",
    val permissions: List<String> = emptyList(),
    val roleIds: List<String> = emptyList(),
    val userId: String = "",
    val visibility: String = "",
)

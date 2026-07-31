package io.github.vrcmteam.vrcm.storage.data

import kotlinx.serialization.Serializable

/**
 * Local, account-scoped relationship activity captured while VRCMomo is running.
 * This cache deliberately does not attempt to reconstruct historical activity from VRChat.
 */
@Serializable
data class FriendActivityCache(
    val statsByFriendId: Map<String, FriendActivityStats> = emptyMap(),
)

@Serializable
data class FriendActivityStats(
    val userId: String,
    val lastSeenTogetherAtMillis: Long? = null,
    val meetingCount: Int = 0,
    val togetherDurationMillis: Long = 0,
    val lastOnlineAtMillis: Long? = null,
    val lastOfflineAtMillis: Long? = null,
    val lastActivityAtMillis: Long? = null,
    /** Runtime-only fields. They are cleared when a cached session is restored. */
    val activeTogetherSinceMillis: Long? = null,
    val activeTogetherInstanceId: String? = null,
    val lastObservedLocation: String? = null,
    val lastObservedStatus: String? = null,
) {
    fun clearRuntimeObservation(): FriendActivityStats = copy(
        activeTogetherSinceMillis = null,
        activeTogetherInstanceId = null,
        lastObservedLocation = null,
        lastObservedStatus = null,
    )
}

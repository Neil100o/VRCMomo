package io.github.vrcmteam.vrcm.storage.data

import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import kotlinx.serialization.Serializable

/**
 * Local, account-scoped relationship activity captured while VRCMomo is running.
 * This cache deliberately does not attempt to reconstruct historical activity from VRChat.
 */
@Serializable
data class FriendActivityCache(
    val statsByFriendId: Map<String, FriendActivityStats> = emptyMap(),
    /**
     * Persisted-data schema, independent from the app version.
     * Missing in pre-0.2 records and therefore defaults to schema 1 for migration.
     */
    val schemaVersion: Int = LEGACY_SCHEMA_VERSION,
    /** Stable keys from imported VRCX events, used to make repeated imports idempotent. */
    val importedVrcxEventKeys: Set<String> = emptySet(),
    /** Newest-first local event timeline, capped by [FriendActivityTracker] before persistence. */
    val activityEvents: List<FriendActivityEvent> = emptyList(),
    /** Number of 500-event files written beside the account cache. */
    val eventChunkCount: Int = 0,
) {
    companion object {
        const val LEGACY_SCHEMA_VERSION = 1
        const val CURRENT_SCHEMA_VERSION = 6
    }
}

@Serializable
data class FriendActivityEvent(
    val userId: String,
    val displayName: String,
    val type: FriendActivityEventType,
    val occurredAtMillis: Long,
    /** Line-level profile text changes. Removed lines are false; added lines are true. */
    val diffLines: List<FriendActivityDiffLine> = emptyList(),
)

@Serializable
data class FriendActivityDiffLine(
    val added: Boolean,
    val text: String,
)

@Serializable
enum class FriendActivityEventType {
    Online,
    Offline,
    Met,
    Left,
    LocationChanged,
    StatusChanged,
    ProfileChanged,
}

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
    /** Last observed public friend profile fields for offline recent-player rendering. */
    val lastKnownFriend: FriendData? = null,
) {
    fun clearRuntimeObservation(): FriendActivityStats = copy(
        activeTogetherSinceMillis = null,
        activeTogetherInstanceId = null,
        lastObservedLocation = null,
        lastObservedStatus = null,
    )
}

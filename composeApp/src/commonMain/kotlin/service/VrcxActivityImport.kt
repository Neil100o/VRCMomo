package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityDiffLine
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEventType
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

internal const val VRCX_ACTIVITY_BRIDGE_FORMAT_V1 = "vrcmomo-vrcx-activity-v1"
internal const val VRCX_ACTIVITY_BRIDGE_FORMAT_V2 = "vrcmomo-vrcx-activity-v2"

@Serializable
internal data class VrcxActivityBridge(
    val format: String,
    val events: VrcxActivityEvents = VrcxActivityEvents(),
)

@Serializable
internal data class VrcxActivityEvents(
    val presence: List<VrcxPresenceEvent> = emptyList(),
    val locationChanges: List<VrcxLocationChangeEvent> = emptyList(),
    val statusChanges: List<VrcxStatusChangeEvent> = emptyList(),
    val profileChanges: List<VrcxProfileChangeEvent> = emptyList(),
    val avatarChanges: List<VrcxAvatarChangeEvent> = emptyList(),
    val friendHistory: List<VrcxFriendHistoryEvent> = emptyList(),
    val instanceJoinLeave: List<VrcxJoinLeaveEvent> = emptyList(),
)

@Serializable
internal data class VrcxPresenceEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    val type: String,
    val location: String = "",
)

@Serializable
internal data class VrcxLocationChangeEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    val location: String = "",
    @SerialName("previous_location") val previousLocation: String = "",
)

@Serializable
internal data class VrcxStatusChangeEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    val status: String = "",
    @SerialName("status_description") val statusDescription: String = "",
    @SerialName("previous_status") val previousStatus: String = "",
    @SerialName("previous_status_description") val previousStatusDescription: String = "",
)

@Serializable
internal data class VrcxProfileChangeEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    val bio: String = "",
    @SerialName("previous_bio") val previousBio: String = "",
)

@Serializable
internal data class VrcxAvatarChangeEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_name") val avatarName: String = "",
    @SerialName("current_avatar_image_url") val currentAvatarImageUrl: String = "",
    @SerialName("previous_current_avatar_image_url") val previousAvatarImageUrl: String = "",
)

@Serializable
internal data class VrcxFriendHistoryEvent(
    @SerialName("created_at") val createdAt: String,
    val type: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("previous_display_name") val previousDisplayName: String = "",
    @SerialName("trust_level") val trustLevel: String = "",
    @SerialName("previous_trust_level") val previousTrustLevel: String = "",
)

@Serializable
internal data class VrcxJoinLeaveEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("display_name") val displayName: String = "",
    val type: String,
    val location: String = "",
    val time: Long? = null,
)

internal data class VrcxActivityImportPreview(
    val presenceEvents: Int,
    val completedMeetings: Int,
    val involvedFriends: Int,
    val alreadyImportedEvents: Int,
    internal val result: VrcxActivityImportResult,
)

internal data class VrcxActivityImportResult(
    val updates: Map<String, FriendActivityStats>,
    val events: List<FriendActivityEvent>,
    val acceptedEventKeys: Set<String>,
)

/** Imports only social activity information. Avatar URLs, notes, moderation data and credentials are never imported. */
@OptIn(ExperimentalTime::class)
internal object VrcxActivityImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        // VRCX SQLite columns can be NULL in older histories; use each model's default value.
        coerceInputValues = true
    }

    fun preview(raw: String, existingEventKeys: Set<String>): VrcxActivityImportPreview {
        val bridge = json.decodeFromString<VrcxActivityBridge>(raw)
        require(bridge.format in setOf(VRCX_ACTIVITY_BRIDGE_FORMAT_V1, VRCX_ACTIVITY_BRIDGE_FORMAT_V2)) {
            "Unsupported VRCX activity export"
        }

        val updates = mutableMapOf<String, FriendActivityStats>()
        val importedEvents = mutableListOf<FriendActivityEvent>()
        val accepted = mutableSetOf<String>()
        var skipped = 0
        var presenceCount = 0
        var meetingCount = 0

        fun accept(key: String, block: () -> Unit) {
            if (key in existingEventKeys || key in accepted) skipped++ else {
                accepted += key
                block()
            }
        }
        fun statsFor(userId: String) = updates[userId] ?: FriendActivityStats(userId = userId)
        fun save(userId: String, stats: FriendActivityStats) { updates[userId] = stats }

        bridge.events.presence.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId()) return@forEach
            val eventType = when {
                event.type.equals("Online", true) -> FriendActivityEventType.Online
                event.type.equals("Offline", true) -> FriendActivityEventType.Offline
                else -> null
            } ?: return@forEach
            val key = "presence|${event.createdAt}|${event.userId}|${event.type}"
            accept(key) {
                val current = statsFor(event.userId)
                save(event.userId, current.copy(
                    lastOnlineAtMillis = if (eventType == FriendActivityEventType.Online) latest(current.lastOnlineAtMillis, timestamp) else current.lastOnlineAtMillis,
                    lastOfflineAtMillis = if (eventType == FriendActivityEventType.Offline) latest(current.lastOfflineAtMillis, timestamp) else current.lastOfflineAtMillis,
                    lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp),
                ))
                importedEvents += FriendActivityEvent(event.userId, event.displayName, eventType, timestamp, currentValue = event.location)
                presenceCount++
            }
        }

        bridge.events.locationChanges.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId()) return@forEach
            val key = "location|${event.createdAt}|${event.userId}|${event.previousLocation}|${event.location}"
            accept(key) {
                val current = statsFor(event.userId)
                save(event.userId, current.copy(lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp)))
                importedEvents += FriendActivityEvent(event.userId, event.displayName, FriendActivityEventType.LocationChanged, timestamp, previousValue = event.previousLocation, currentValue = event.location)
            }
        }

        bridge.events.statusChanges.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId()) return@forEach
            val before = event.previousStatus.withDescription(event.previousStatusDescription)
            val after = event.status.withDescription(event.statusDescription)
            val key = "status|${event.createdAt}|${event.userId}|$before|$after"
            accept(key) {
                val current = statsFor(event.userId)
                save(event.userId, current.copy(lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp)))
                importedEvents += FriendActivityEvent(event.userId, event.displayName, FriendActivityEventType.StatusChanged, timestamp, previousValue = before, currentValue = after)
            }
        }

        bridge.events.profileChanges.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId()) return@forEach
            val key = "bio|${event.createdAt}|${event.userId}|${event.previousBio.hashCode()}|${event.bio.hashCode()}"
            accept(key) {
                val current = statsFor(event.userId)
                save(event.userId, current.copy(lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp)))
                importedEvents += FriendActivityEvent(
                    event.userId, event.displayName, FriendActivityEventType.ProfileChanged, timestamp,
                    diffLines = friendBioDiff(event.previousBio, event.bio),
                )
            }
        }

        bridge.events.avatarChanges.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId()) return@forEach
            val key = "avatar|${event.createdAt}|${event.userId}|${event.previousAvatarImageUrl}|${event.currentAvatarImageUrl}"
            accept(key) {
                val current = statsFor(event.userId)
                save(event.userId, current.copy(lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp)))
                importedEvents += FriendActivityEvent(
                    event.userId, event.displayName, FriendActivityEventType.AvatarChanged, timestamp,
                    currentValue = event.avatarName,
                )
            }
        }

        bridge.events.friendHistory.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId()) return@forEach
            val before = event.previousDisplayName.withDescription(event.previousTrustLevel)
            val after = event.displayName.withDescription(event.trustLevel)
            val key = "friend|${event.createdAt}|${event.userId}|${event.type}|$before|$after"
            accept(key) {
                val current = statsFor(event.userId)
                save(event.userId, current.copy(lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp)))
                importedEvents += FriendActivityEvent(
                    event.userId, event.displayName, FriendActivityEventType.FriendshipChanged, timestamp,
                    previousValue = before, currentValue = listOf(event.type, after).filter { it.isNotBlank() }.joinToString(" · "),
                )
            }
        }

        bridge.events.instanceJoinLeave.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId() || !event.type.equals("OnPlayerLeft", true)) return@forEach
            val duration = event.time?.coerceAtLeast(0L) ?: 0L
            val key = "meeting|${event.createdAt}|${event.userId}|${event.location}|$duration"
            accept(key) {
                val current = statsFor(event.userId)
                save(event.userId, current.copy(
                    lastSeenTogetherAtMillis = latest(current.lastSeenTogetherAtMillis, timestamp),
                    meetingCount = current.meetingCount + 1,
                    togetherDurationMillis = current.togetherDurationMillis + duration,
                    lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp),
                ))
                importedEvents += FriendActivityEvent(event.userId, event.displayName, FriendActivityEventType.Left, timestamp, currentValue = event.location)
                meetingCount++
            }
        }

        return VrcxActivityImportPreview(
            presenceEvents = presenceCount,
            completedMeetings = meetingCount,
            involvedFriends = updates.size,
            alreadyImportedEvents = skipped,
            result = VrcxActivityImportResult(updates, importedEvents.sortedByDescending(FriendActivityEvent::occurredAtMillis), accepted),
        )
    }

    private fun String.isVrcUserId(): Boolean = startsWith("usr_") && length > 8
    private fun String.toEpochMillisOrNull(): Long? = runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrNull()
    private fun latest(first: Long?, second: Long): Long = maxOf(first ?: Long.MIN_VALUE, second)
    private fun String.withDescription(description: String): String = listOf(this, description).filter { it.isNotBlank() }.joinToString(" · ")
}

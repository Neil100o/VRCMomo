package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

internal const val VRCX_ACTIVITY_BRIDGE_FORMAT = "vrcmomo-vrcx-activity-v1"

@Serializable
internal data class VrcxActivityBridge(
    val format: String,
    val events: VrcxActivityEvents = VrcxActivityEvents(),
)

@Serializable
internal data class VrcxActivityEvents(
    val presence: List<VrcxPresenceEvent> = emptyList(),
    val instanceJoinLeave: List<VrcxJoinLeaveEvent> = emptyList(),
)

@Serializable
internal data class VrcxPresenceEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    val type: String,
)

@Serializable
internal data class VrcxJoinLeaveEvent(
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String = "",
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
    val acceptedEventKeys: Set<String>,
)

@OptIn(ExperimentalTime::class)
internal object VrcxActivityImporter {
    private val json = Json { ignoreUnknownKeys = true }

    fun preview(
        raw: String,
        existingEventKeys: Set<String>,
    ): VrcxActivityImportPreview {
        val bridge = json.decodeFromString<VrcxActivityBridge>(raw)
        require(bridge.format == VRCX_ACTIVITY_BRIDGE_FORMAT) { "Unsupported VRCX activity export" }

        val updates = mutableMapOf<String, FriendActivityStats>()
        val accepted = mutableSetOf<String>()
        var skipped = 0
        var presenceCount = 0
        var meetingCount = 0

        bridge.events.presence.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId()) return@forEach
            val key = "presence|${event.createdAt}|${event.userId}|${event.type}"
            if (key in existingEventKeys) {
                skipped++
                return@forEach
            }
            val current = updates[event.userId] ?: FriendActivityStats(userId = event.userId)
            updates[event.userId] = current.copy(
                lastOnlineAtMillis = if (event.type.equals("Online", true)) latest(current.lastOnlineAtMillis, timestamp) else current.lastOnlineAtMillis,
                lastOfflineAtMillis = if (event.type.equals("Offline", true)) latest(current.lastOfflineAtMillis, timestamp) else current.lastOfflineAtMillis,
                lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp),
            )
            accepted += key
            presenceCount++
        }

        bridge.events.instanceJoinLeave.forEach { event ->
            val timestamp = event.createdAt.toEpochMillisOrNull() ?: return@forEach
            if (!event.userId.isVrcUserId() || !event.type.equals("OnPlayerLeft", true)) return@forEach
            val duration = event.time?.coerceAtLeast(0L) ?: 0L
            val key = "meeting|${event.createdAt}|${event.userId}|${event.location}|$duration"
            if (key in existingEventKeys) {
                skipped++
                return@forEach
            }
            val current = updates[event.userId] ?: FriendActivityStats(userId = event.userId)
            updates[event.userId] = current.copy(
                lastSeenTogetherAtMillis = latest(current.lastSeenTogetherAtMillis, timestamp),
                meetingCount = current.meetingCount + 1,
                togetherDurationMillis = current.togetherDurationMillis + duration,
                lastActivityAtMillis = latest(current.lastActivityAtMillis, timestamp),
            )
            accepted += key
            meetingCount++
        }

        return VrcxActivityImportPreview(
            presenceEvents = presenceCount,
            completedMeetings = meetingCount,
            involvedFriends = updates.size,
            alreadyImportedEvents = skipped,
            result = VrcxActivityImportResult(updates, accepted),
        )
    }

    private fun String.isVrcUserId(): Boolean = startsWith("usr_") && length > 8

    private fun String.toEpochMillisOrNull(): Long? =
        runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrNull()

    private fun latest(first: Long?, second: Long): Long = maxOf(first ?: Long.MIN_VALUE, second)
}

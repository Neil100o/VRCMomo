package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

internal const val VRCMOMO_ACTIVITY_SYNC_FORMAT_V1 = "vrcmomo-activity-sync-v1"
internal const val VRCMOMO_ACTIVITY_SYNC_FORMAT_V2 = "vrcmomo-activity-sync-v2"
internal const val VRCMOMO_ACTIVITY_ARCHIVE_FORMAT_V1 = "vrcmomo-activity-archive-v1"

/**
 * Credential-free activity snapshot produced by one VRCMomo installation.
 *
 * V2 adds [sourceDeviceId]. The bridge may retain several snapshots, but an importer always
 * merges immutable timeline events by [vrcmomoActivityEventKey], never by blindly adding totals.
 */
@Serializable
internal data class VrcmomoActivitySyncEnvelope(
    val format: String = VRCMOMO_ACTIVITY_SYNC_FORMAT_V2,
    val ownerUserId: String,
    val exportedAtMillis: Long,
    val sourceDeviceId: String? = null,
    val statsByFriendId: Map<String, FriendActivityStats>,
    val activityEvents: List<FriendActivityEvent>,
)

/** An archive returned by the local bridge. It is a transport container, not a new source of truth. */
@Serializable
internal data class VrcmomoActivitySyncArchive(
    val format: String = VRCMOMO_ACTIVITY_ARCHIVE_FORMAT_V1,
    val documents: List<VrcmomoActivitySyncEnvelope> = emptyList(),
)

internal data class VrcmomoActivityImportPreview(
    val sourceDocuments: Int,
    val acceptedEvents: Int,
    val alreadyKnownEvents: Int,
    val involvedFriends: Int,
    internal val events: List<FriendActivityEvent>,
    internal val acceptedEventKeys: Set<String>,
    internal val baselineStats: Map<String, FriendActivityStats>,
)

internal object VrcmomoActivityImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun preview(raw: String, knownEventKeys: Set<String>): VrcmomoActivityImportPreview {
        val documents = decodeDocuments(raw)
        val accepted = linkedSetOf<String>()
        val events = mutableListOf<FriendActivityEvent>()
        val baselineStats = mutableMapOf<String, FriendActivityStats>()
        var known = 0
        documents.forEach { document ->
            require(document.format in setOf(VRCMOMO_ACTIVITY_SYNC_FORMAT_V1, VRCMOMO_ACTIVITY_SYNC_FORMAT_V2)) {
                "Unsupported VRCMomo activity document"
            }
            document.statsByFriendId.forEach { (userId, incoming) ->
                baselineStats[userId] = mergeVrcmomoSnapshotBaseline(baselineStats[userId], incoming)
            }
            document.activityEvents.forEach { event ->
                val key = vrcmomoActivityEventKey(event)
                if (key in knownEventKeys || !accepted.add(key)) {
                    known++
                } else {
                    events += event
                }
            }
        }
        return VrcmomoActivityImportPreview(
            sourceDocuments = documents.size,
            acceptedEvents = events.size,
            alreadyKnownEvents = known,
            involvedFriends = (events.map(FriendActivityEvent::userId) + baselineStats.keys).distinct().size,
            events = events.sortedByDescending(FriendActivityEvent::occurredAtMillis),
            acceptedEventKeys = accepted,
            baselineStats = baselineStats,
        )
    }

    private fun decodeDocuments(raw: String): List<VrcmomoActivitySyncEnvelope> {
        val root = json.parseToJsonElement(raw)
        return when (root) {
            is kotlinx.serialization.json.JsonObject -> {
                val format = root["format"]?.jsonPrimitive?.content
                when {
                    format == VRCMOMO_ACTIVITY_ARCHIVE_FORMAT_V1 || (format == null && root.containsKey("documents")) ->
                        json.decodeFromJsonElement<VrcmomoActivitySyncArchive>(root).documents
                    format in setOf(VRCMOMO_ACTIVITY_SYNC_FORMAT_V1, VRCMOMO_ACTIVITY_SYNC_FORMAT_V2) ->
                        listOf(json.decodeFromJsonElement<VrcmomoActivitySyncEnvelope>(root))
                    else -> error("Unsupported VRCMomo activity archive")
                }
            }
            else -> error("Invalid VRCMomo activity archive")
        }
    }
}

/**
 * Phone sync documents are complete snapshots, not deltas. Taking the larger cumulative value
 * preserves the most complete baseline while making repeated imports idempotent.
 */
internal fun mergeVrcmomoSnapshotBaseline(
    current: FriendActivityStats?,
    incoming: FriendActivityStats,
): FriendActivityStats {
    if (current == null) return incoming.clearRuntimeObservation()
    return current.copy(
        lastSeenTogetherAtMillis = latestTimestamp(current.lastSeenTogetherAtMillis, incoming.lastSeenTogetherAtMillis),
        meetingCount = maxOf(current.meetingCount, incoming.meetingCount),
        togetherDurationMillis = maxOf(current.togetherDurationMillis, incoming.togetherDurationMillis),
        lastOnlineAtMillis = latestTimestamp(current.lastOnlineAtMillis, incoming.lastOnlineAtMillis),
        lastOfflineAtMillis = latestTimestamp(current.lastOfflineAtMillis, incoming.lastOfflineAtMillis),
        lastActivityAtMillis = latestTimestamp(current.lastActivityAtMillis, incoming.lastActivityAtMillis),
        lastObservedLocation = current.lastObservedLocation ?: incoming.lastObservedLocation,
        lastObservedStatus = current.lastObservedStatus ?: incoming.lastObservedStatus,
        lastKnownFriend = current.lastKnownFriend ?: incoming.lastKnownFriend,
    )
}

private fun latestTimestamp(first: Long?, second: Long?): Long? = listOfNotNull(first, second).maxOrNull()

/**
 * A source-independent fingerprint. It lets retries and overlapping phone/desktop observations
 * collapse into one timeline entry. Aggregate counters are intentionally excluded from merging:
 * summing repeated snapshots would inflate them.
 */
internal fun vrcmomoActivityEventKey(event: FriendActivityEvent): String = buildString {
    append(event.userId).append('|')
    append(event.type.name).append('|')
    append(event.occurredAtMillis).append('|')
    append(event.previousValue.orEmpty()).append('|')
    append(event.currentValue.orEmpty()).append('|')
    event.diffLines.forEach { line ->
        append(if (line.added) '+' else '-').append(line.text).append('\u001f')
    }
}

internal fun VrcmomoActivitySyncEnvelope.encode(): String = syncJson.encodeToString(this)

private val syncJson = Json {
    encodeDefaults = true
    explicitNulls = false
}

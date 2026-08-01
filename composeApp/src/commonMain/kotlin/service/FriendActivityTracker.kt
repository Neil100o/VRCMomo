package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEventType
import io.github.vrcmteam.vrcm.storage.data.FriendActivityDiffLine

internal data class FriendActivityObservation(
    val userId: String,
    val location: String,
    val status: String,
    val lastActivityAtMillis: Long?,
    val friendData: FriendData? = null,
)

/**
 * Tracks only observations made by this app. It never guesses activity that happened while the
 * app was not monitoring a relationship.
 */
internal class FriendActivityTracker(
    initialStats: Map<String, FriendActivityStats> = emptyMap(),
    initialEvents: List<FriendActivityEvent> = emptyList(),
) {
    private val statsByFriendId = initialStats.mapValues { (_, stats) ->
        stats.clearRuntimeObservation()
    }.toMutableMap()
    private var selfInstanceId: String? = null
    private val activityEvents = initialEvents
        .sortedByDescending(FriendActivityEvent::occurredAtMillis)
        .toMutableList()

    val snapshot: Map<String, FriendActivityStats>
        get() = statsByFriendId.toMap()

    val eventLog: List<FriendActivityEvent>
        get() = activityEvents.toList()

    fun observeFriends(
        friends: Collection<FriendActivityObservation>,
        nowMillis: Long,
    ): Boolean {
        var changed = false
        friends.forEach { friend ->
            val existing = statsByFriendId[friend.userId]
            val previousLocation = existing?.lastObservedLocation
            val previousStatus = existing?.lastObservedStatus
            val wasInGame = isInGameLocation(previousLocation)
            val isInGame = isInGameLocation(friend.location)
            val hasPriorObservation = previousLocation != null || previousStatus != null
            var next = (existing ?: FriendActivityStats(userId = friend.userId)).copy(
                lastKnownFriend = friend.friendData ?: existing?.lastKnownFriend,
            )

            if (previousLocation == null && previousStatus == null) {
                // First observation is a baseline, not an invented online/offline transition.
                next = next.copy(
                    lastObservedLocation = friend.location,
                    lastObservedStatus = friend.status,
                    lastActivityAtMillis = latest(next.lastActivityAtMillis, friend.lastActivityAtMillis),
                )
            } else {
                val presenceChanged = previousLocation != friend.location || previousStatus != friend.status
                val activityAt = if (presenceChanged) nowMillis else null
                next = next.copy(
                    lastOnlineAtMillis = if (!wasInGame && isInGame) nowMillis else next.lastOnlineAtMillis,
                    lastOfflineAtMillis = if (wasInGame && !isInGame) nowMillis else next.lastOfflineAtMillis,
                    lastActivityAtMillis = latest(
                        next.lastActivityAtMillis,
                        friend.lastActivityAtMillis,
                        activityAt,
                    ),
                    lastObservedLocation = friend.location,
                    lastObservedStatus = friend.status,
                )
            }

                next = reconcileMeeting(next, friend.location, nowMillis)
                val name = friend.friendData?.displayName ?: existing?.lastKnownFriend?.displayName.orEmpty()
                if (hasPriorObservation) {
                    if (!wasInGame && isInGame) appendEvent(friend.userId, name, FriendActivityEventType.Online, nowMillis)
                    if (wasInGame && !isInGame) appendEvent(friend.userId, name, FriendActivityEventType.Offline, nowMillis)
                    if (wasInGame && isInGame && previousLocation != friend.location) {
                        appendEvent(friend.userId, name, FriendActivityEventType.LocationChanged, nowMillis)
                    }
                if (previousStatus != friend.status) {
                    appendEvent(friend.userId, name, FriendActivityEventType.StatusChanged, nowMillis)
                }
                val profileDiff = friendBioDiff(existing?.lastKnownFriend?.bio, friend.friendData?.bio)
                if (profileDiff.isNotEmpty()) {
                    appendEvent(
                        userId = friend.userId,
                        displayName = name,
                        type = FriendActivityEventType.ProfileChanged,
                        occurredAtMillis = nowMillis,
                        diffLines = profileDiff,
                    )
                }
                }
                if (existing?.activeTogetherSinceMillis == null && next.activeTogetherSinceMillis != null) {
                    appendEvent(friend.userId, name, FriendActivityEventType.Met, nowMillis)
                }
                if (existing?.activeTogetherSinceMillis != null && next.activeTogetherSinceMillis == null) {
                    appendEvent(friend.userId, name, FriendActivityEventType.Left, nowMillis)
                }
            if (existing != next) {
                statsByFriendId[friend.userId] = next
                changed = true
            }
        }
        return changed
    }

    fun snapshotForPersistence(nowMillis: Long): Map<String, FriendActivityStats> =
        statsByFriendId.mapValues { (_, stats) ->
            val activeSince = stats.activeTogetherSinceMillis
            if (activeSince == null) stats else stats.copy(
                togetherDurationMillis = stats.togetherDurationMillis +
                    (nowMillis - activeSince).coerceAtLeast(0),
                activeTogetherSinceMillis = nowMillis,
            )
        }

    fun updateSelfInstance(instanceId: String?, nowMillis: Long): Boolean {
        val normalized = instanceId.normalizedInstanceId()
        if (selfInstanceId == normalized) return false
        selfInstanceId = normalized

        var changed = false
        statsByFriendId.forEach { (userId, current) ->
            val next = reconcileMeeting(current, current.lastObservedLocation.orEmpty(), nowMillis)
            val name = current.lastKnownFriend?.displayName.orEmpty()
            if (current.activeTogetherSinceMillis == null && next.activeTogetherSinceMillis != null) {
                appendEvent(userId, name, FriendActivityEventType.Met, nowMillis)
            }
            if (current.activeTogetherSinceMillis != null && next.activeTogetherSinceMillis == null) {
                appendEvent(userId, name, FriendActivityEventType.Left, nowMillis)
            }
            if (next != current) {
                statsByFriendId[userId] = next
                changed = true
            }
        }
        return changed
    }

    /**
     * Merges historical values without overwriting the live observation fields
     * maintained by the current mobile monitoring session.
     */
    fun mergeImportedStats(imported: Map<String, FriendActivityStats>) {
        imported.forEach { (userId, incoming) ->
            val current = statsByFriendId[userId] ?: FriendActivityStats(userId = userId)
            statsByFriendId[userId] = current.copy(
                lastSeenTogetherAtMillis = latest(current.lastSeenTogetherAtMillis, incoming.lastSeenTogetherAtMillis),
                meetingCount = current.meetingCount + incoming.meetingCount,
                togetherDurationMillis = current.togetherDurationMillis + incoming.togetherDurationMillis,
                lastOnlineAtMillis = latest(current.lastOnlineAtMillis, incoming.lastOnlineAtMillis),
                lastOfflineAtMillis = latest(current.lastOfflineAtMillis, incoming.lastOfflineAtMillis),
                lastActivityAtMillis = latest(current.lastActivityAtMillis, incoming.lastActivityAtMillis),
            )
        }
    }

    private fun reconcileMeeting(
        stats: FriendActivityStats,
        friendLocation: String,
        nowMillis: Long,
    ): FriendActivityStats {
        val sharedInstance = selfInstanceId?.takeIf { it == friendLocation.normalizedInstanceId() }
        val activeSince = stats.activeTogetherSinceMillis
        if (sharedInstance != null && activeSince == null) {
            return stats.copy(
                lastSeenTogetherAtMillis = nowMillis,
                meetingCount = stats.meetingCount + 1,
                activeTogetherSinceMillis = nowMillis,
                activeTogetherInstanceId = sharedInstance,
            )
        }
        if (sharedInstance == null && activeSince != null) {
            return stats.copy(
                lastSeenTogetherAtMillis = nowMillis,
                togetherDurationMillis = stats.togetherDurationMillis + (nowMillis - activeSince).coerceAtLeast(0),
                activeTogetherSinceMillis = null,
                activeTogetherInstanceId = null,
            )
        }
        return stats
    }
    private fun String?.normalizedInstanceId(): String? =
        this?.trim()?.takeIf { it.startsWith("wrld_") && ':' in it }

    private fun latest(vararg timestamps: Long?): Long? = timestamps.filterNotNull().maxOrNull()

    private fun appendEvent(
        userId: String,
        displayName: String,
        type: FriendActivityEventType,
        occurredAtMillis: Long,
        diffLines: List<FriendActivityDiffLine> = emptyList(),
    ) {
        activityEvents.add(0, FriendActivityEvent(userId, displayName, type, occurredAtMillis, diffLines))
    }

    fun pruneEventsBefore(cutoffMillis: Long): Boolean {
        val before = activityEvents.size
        activityEvents.removeAll { it.occurredAtMillis < cutoffMillis }
        return activityEvents.size != before
    }

    fun clearEvents(): Boolean {
        if (activityEvents.isEmpty()) return false
        activityEvents.clear()
        return true
    }
}

/** A compact line diff for profile bios, retaining only lines that were actually changed. */
internal fun friendBioDiff(before: String?, after: String?): List<FriendActivityDiffLine> {
    val oldLines = before.orEmpty().lines()
    val newLines = after.orEmpty().lines()
    if (oldLines == newLines) return emptyList()

    val lcs = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }
    for (oldIndex in oldLines.indices.reversed()) {
        for (newIndex in newLines.indices.reversed()) {
            lcs[oldIndex][newIndex] = if (oldLines[oldIndex] == newLines[newIndex]) {
                lcs[oldIndex + 1][newIndex + 1] + 1
            } else {
                maxOf(lcs[oldIndex + 1][newIndex], lcs[oldIndex][newIndex + 1])
            }
        }
    }
    val result = mutableListOf<FriendActivityDiffLine>()
    var oldIndex = 0
    var newIndex = 0
    while (oldIndex < oldLines.size || newIndex < newLines.size) {
        when {
            oldIndex < oldLines.size && newIndex < newLines.size && oldLines[oldIndex] == newLines[newIndex] -> {
                oldIndex++
                newIndex++
            }
            newIndex < newLines.size && (oldIndex == oldLines.size || lcs[oldIndex][newIndex + 1] > lcs[oldIndex + 1][newIndex]) -> {
                result += FriendActivityDiffLine(added = true, text = newLines[newIndex++])
            }
            else -> result += FriendActivityDiffLine(added = false, text = oldLines[oldIndex++])
        }
    }
    return result.take(MAX_PROFILE_DIFF_LINES)
}

private const val MAX_PROFILE_DIFF_LINES = 200

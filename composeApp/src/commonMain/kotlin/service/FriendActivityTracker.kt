package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats

internal data class FriendActivityObservation(
    val userId: String,
    val location: String,
    val status: String,
    val lastActivityAtMillis: Long?,
)

/**
 * Tracks only observations made by this app. It never guesses activity that happened while the
 * app was not monitoring a relationship.
 */
internal class FriendActivityTracker(
    initialStats: Map<String, FriendActivityStats> = emptyMap(),
) {
    private val statsByFriendId = initialStats.mapValues { (_, stats) ->
        stats.clearRuntimeObservation()
    }.toMutableMap()
    private var selfInstanceId: String? = null

    val snapshot: Map<String, FriendActivityStats>
        get() = statsByFriendId.toMap()

    fun observeFriends(
        friends: Collection<FriendActivityObservation>,
        nowMillis: Long,
    ): Boolean {
        var changed = false
        friends.forEach { friend ->
            val existing = statsByFriendId[friend.userId]
            val previousLocation = existing?.lastObservedLocation
            val previousStatus = existing?.lastObservedStatus
            var next = existing ?: FriendActivityStats(userId = friend.userId)

            if (previousLocation == null && previousStatus == null) {
                // First observation is a baseline, not an invented online/offline transition.
                next = next.copy(
                    lastObservedLocation = friend.location,
                    lastObservedStatus = friend.status,
                    lastActivityAtMillis = latest(next.lastActivityAtMillis, friend.lastActivityAtMillis),
                )
            } else {
                val wasOffline = isOffline(previousLocation, previousStatus)
                val isOffline = isOffline(friend.location, friend.status)
                val presenceChanged = previousLocation != friend.location || previousStatus != friend.status
                val activityAt = if (presenceChanged) nowMillis else null
                next = next.copy(
                    lastOnlineAtMillis = if (wasOffline && !isOffline) nowMillis else next.lastOnlineAtMillis,
                    lastOfflineAtMillis = if (!wasOffline && isOffline) nowMillis else next.lastOfflineAtMillis,
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
            if (existing != next) {
                statsByFriendId[friend.userId] = next
                changed = true
            }
        }
        return changed
    }

    fun updateSelfInstance(instanceId: String?, nowMillis: Long): Boolean {
        val normalized = instanceId.normalizedInstanceId()
        if (selfInstanceId == normalized) return false
        selfInstanceId = normalized

        var changed = false
        statsByFriendId.forEach { (userId, current) ->
            val next = reconcileMeeting(current, current.lastObservedLocation.orEmpty(), nowMillis)
            if (next != current) {
                statsByFriendId[userId] = next
                changed = true
            }
        }
        return changed
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

    private fun isOffline(location: String?, status: String?): Boolean =
        location == "offline" || status.equals("offline", ignoreCase = true)

    private fun String?.normalizedInstanceId(): String? =
        this?.trim()?.takeIf { it.startsWith("wrld_") && ':' in it }

    private fun latest(vararg timestamps: Long?): Long? = timestamps.filterNotNull().maxOrNull()
}

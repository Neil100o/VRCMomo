package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.storage.FriendActivityCacheDao
import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Account-scoped relationship telemetry captured from the live friend feed.
 * VRChat does not expose a complete historical meeting log, so this service only records sessions
 * observed after VRCMomo begins monitoring the active account.
 */
@OptIn(ExperimentalTime::class)
class FriendActivityService(
    private val cacheDao: FriendActivityCacheDao,
) {
    private val lock = Any()
    private var activeAccountUserId: String? = null
    private var tracker = FriendActivityTracker()

    private val _friendActivityState = MutableStateFlow<Map<String, FriendActivityStats>>(emptyMap())
    val friendActivityState: StateFlow<Map<String, FriendActivityStats>> = _friendActivityState.asStateFlow()

    fun activateAccount(userId: String) = synchronized(lock) {
        if (activeAccountUserId == userId) return@synchronized
        activeAccountUserId = userId
        tracker = FriendActivityTracker(cacheDao.load(userId)?.statsByFriendId.orEmpty())
        publishLocked(save = false)
    }

    fun deactivateAccount() = synchronized(lock) {
        activeAccountUserId = null
        tracker = FriendActivityTracker()
        _friendActivityState.value = emptyMap()
    }

    fun observeFriends(friends: Map<String, FriendData>) = synchronized(lock) {
        if (activeAccountUserId == null) return@synchronized
        val changed = tracker.observeFriends(
            friends = friends.values.map { friend ->
                FriendActivityObservation(
                    userId = friend.id,
                    location = friend.location,
                    status = friend.status.name,
                    lastActivityAtMillis = friend.lastActivity.toEpochMillisOrNull(),
                )
            },
            nowMillis = Clock.System.now().toEpochMilliseconds(),
        )
        if (changed) publishLocked(save = true)
    }

    fun updateSelfInstance(instanceId: String?) = synchronized(lock) {
        if (activeAccountUserId == null) return@synchronized
        if (tracker.updateSelfInstance(instanceId, Clock.System.now().toEpochMilliseconds())) {
            publishLocked(save = true)
        }
    }

    private fun publishLocked(save: Boolean) {
        val snapshot = tracker.snapshot
        _friendActivityState.value = snapshot
        if (save) {
            activeAccountUserId?.let { ownerUserId ->
                cacheDao.save(ownerUserId, FriendActivityCache(snapshot))
            }
        }
    }

    private fun String.toEpochMillisOrNull(): Long? =
        takeIf(String::isNotBlank)?.let { raw ->
            runCatching { Instant.parse(raw).toEpochMilliseconds() }.getOrNull()
        }
}

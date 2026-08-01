package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.storage.FriendActivityCacheDao
import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Account-scoped relationship telemetry captured from the live friend feed.
 *
 * VRChat does not expose a complete historical meeting log, so this service only records sessions
 * observed after VRCMomo begins monitoring the active account. Durable writes are kept outside the
 * settings store and are coalesced so presence bursts do not block the friend feed.
 */
@OptIn(ExperimentalTime::class)
class FriendActivityService(
    private val cacheDao: FriendActivityCacheDao,
) {
    private val lock = Any()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writer = ConflatedAccountCacheWriter<FriendActivityCache>(serviceScope) { owner, cache ->
        cacheDao.save(owner, cache)
    }
    private var activeAccountUserId: String? = null
    private var tracker = FriendActivityTracker()

    private val _friendActivityState = MutableStateFlow<Map<String, FriendActivityStats>>(emptyMap())
    val friendActivityState: StateFlow<Map<String, FriendActivityStats>> = _friendActivityState.asStateFlow()

    init {
        serviceScope.launch {
            while (isActive) {
                delay(PERSIST_INTERVAL_MILLIS)
                persistSnapshot()
            }
        }
    }

    fun activateAccount(userId: String) = synchronized(lock) {
        if (activeAccountUserId == userId) return@synchronized
        persistLocked()
        activeAccountUserId = userId
        tracker = FriendActivityTracker(cacheDao.load(userId)?.statsByFriendId.orEmpty())
        publishLocked(save = false)
    }

    fun deactivateAccount() = synchronized(lock) {
        persistLocked()
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
                    friendData = friend,
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

    /**
     * Returns players observed in the same instance during the requested time window.
     * The returned profile is the last observed friend payload, so an empty search does not need
     * one network request per recent player.
     */
    fun recentPlayersSince(cutoffMillis: Long, limit: Int = 20): List<FriendData> =
        friendActivityState.value.values
            .asSequence()
            .filter { (it.lastSeenTogetherAtMillis ?: Long.MIN_VALUE) >= cutoffMillis }
            .sortedByDescending { it.lastSeenTogetherAtMillis ?: Long.MIN_VALUE }
            .mapNotNull { it.lastKnownFriend }
            .distinctBy(FriendData::id)
            .take(limit.coerceAtLeast(0))
            .toList()

    /** Requests a coalesced write of the current account, including active session duration. */
    fun flushNow() {
        persistSnapshot()
    }

    private fun persistSnapshot() {
        val pending = synchronized(lock) {
            val owner = activeAccountUserId ?: return@synchronized null
            owner to FriendActivityCache(
                tracker.snapshotForPersistence(Clock.System.now().toEpochMilliseconds()),
            )
        } ?: return
        writer.submit(pending.first, pending.second)
    }

    private fun persistLocked() {
        val owner = activeAccountUserId ?: return
        writer.submit(
            owner,
            FriendActivityCache(
                tracker.snapshotForPersistence(Clock.System.now().toEpochMilliseconds()),
            ),
        )
    }

    private fun publishLocked(save: Boolean) {
        val snapshot = tracker.snapshot
        _friendActivityState.value = snapshot
        if (save) persistLocked()
    }

    private fun String.toEpochMillisOrNull(): Long? =
        takeIf(String::isNotBlank)?.let { raw ->
            runCatching { Instant.parse(raw).toEpochMilliseconds() }.getOrNull()
        }

    private companion object {
        const val PERSIST_INTERVAL_MILLIS = 30_000L
    }
}

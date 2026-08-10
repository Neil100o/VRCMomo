package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.storage.FriendActivityCacheDao
import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.SettingsDao
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
    private val settingsDao: SettingsDao? = null,
) {
    private val lock = Any()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writer = ConflatedAccountCacheWriter<FriendActivityCache>(serviceScope) { owner, cache ->
        cacheDao.save(owner, cache)
    }
    private var activeAccountUserId: String? = null
    private var tracker = FriendActivityTracker()
    private var importedVrcxEventKeys: Set<String> = emptySet()
    private var importedVrcmomoEventKeys: Set<String> = emptySet()

    private val _friendActivityState = MutableStateFlow<Map<String, FriendActivityStats>>(emptyMap())
    val friendActivityState: StateFlow<Map<String, FriendActivityStats>> = _friendActivityState.asStateFlow()
    private val _activityLog = MutableStateFlow<List<FriendActivityEvent>>(emptyList())
    val activityLog: StateFlow<List<FriendActivityEvent>> = _activityLog.asStateFlow()

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
        val cache = cacheDao.load(userId)
        tracker = FriendActivityTracker(cache?.statsByFriendId.orEmpty(), cache?.activityEvents.orEmpty())
        pruneEventsLocked()
        importedVrcxEventKeys = cache?.importedVrcxEventKeys.orEmpty()
        importedVrcmomoEventKeys = cache?.importedVrcmomoEventKeys.orEmpty()
        publishLocked(save = false)
    }

    fun deactivateAccount() = synchronized(lock) {
        persistLocked()
        activeAccountUserId = null
        tracker = FriendActivityTracker()
        importedVrcxEventKeys = emptySet()
        importedVrcmomoEventKeys = emptySet()
        _friendActivityState.value = emptyMap()
        _activityLog.value = emptyList()
    }

    fun observeFriends(friends: Map<String, FriendData>) = synchronized(lock) {
        if (activeAccountUserId == null) return@synchronized
        val changed = tracker.observeFriends(
            friends = friends.values.map { friend ->
                FriendActivityObservation(
                    userId = friend.id,
                    location = friend.location,
                    status = friend.status.name,
                    statusDescription = friend.statusDescription,
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

    /** Creates a credential-free snapshot for the paired desktop LAN bridge. */
    internal fun exportLanActivitySync(): String = synchronized(lock) {
        val owner = checkNotNull(activeAccountUserId) { "Sign in before exporting activity history" }
        VrcmomoActivitySyncEnvelope(
            ownerUserId = owner,
            exportedAtMillis = Clock.System.now().toEpochMilliseconds(),
            sourceDeviceId = settingsDao?.lanSyncDeviceId,
            statsByFriendId = tracker.snapshotForPersistence(Clock.System.now().toEpochMilliseconds()),
            activityEvents = tracker.eventLog,
        ).encode()
    }

    /** Preview a desktop-held VRCMomo event archive without modifying the active account. */
    internal fun previewVrcmomoActivityImport(raw: String): VrcmomoActivityImportPreview = synchronized(lock) {
        check(activeAccountUserId != null) { "Sign in before importing activity history" }
        val localKeys = tracker.eventLog.mapTo(mutableSetOf(), ::vrcmomoActivityEventKey)
        VrcmomoActivityImporter.preview(raw, localKeys + importedVrcmomoEventKeys)
    }

    /**
     * Imports only unseen timeline events. Cumulative totals are intentionally not added here:
     * adding complete snapshots after a retry would inflate meetings and play time.
     */
    internal fun applyVrcmomoActivityImport(preview: VrcmomoActivityImportPreview) = synchronized(lock) {
        check(activeAccountUserId != null) { "Sign in before importing activity history" }
        if (preview.acceptedEventKeys.isEmpty()) return@synchronized
        tracker.mergeImportedEvents(preview.events)
        importedVrcmomoEventKeys = importedVrcmomoEventKeys + preview.acceptedEventKeys
        publishLocked(save = true)
    }
    /** Requests a coalesced write of the current account, including active session duration. */
    fun flushNow() {
        persistSnapshot()
    }

    /** Deletes only timeline entries; relationship totals and imported history remain intact. */
    fun clearActivityLog() = synchronized(lock) {
        if (tracker.clearEvents()) publishLocked(save = true)
    }

    fun setActivityLogRetentionDays(days: Int?) = synchronized(lock) {
        require(days == null || days > 0)
        if (days != null) {
            val cutoff = Clock.System.now().toEpochMilliseconds() - days * MILLIS_PER_DAY
            if (tracker.pruneEventsBefore(cutoff)) publishLocked(save = true)
        }
    }

    /** Preview an activity bridge without modifying the current account. */
    internal fun previewVrcxActivityImport(raw: String): VrcxActivityImportPreview = synchronized(lock) {
        check(activeAccountUserId != null) { "Sign in before importing activity history" }
        VrcxActivityImporter.preview(raw, importedVrcxEventKeys)
    }

    /** Merge only previously unseen VRCX events and persist the resulting account cache. */
    internal fun applyVrcxActivityImport(preview: VrcxActivityImportPreview) = synchronized(lock) {
        check(activeAccountUserId != null) { "Sign in before importing activity history" }
        if (preview.result.acceptedEventKeys.isEmpty()) return@synchronized

        tracker.mergeImportedStats(preview.result.updates)
        tracker.mergeImportedEvents(preview.result.events)
        importedVrcxEventKeys = importedVrcxEventKeys + preview.result.acceptedEventKeys
        publishLocked(save = true)
    }

    private fun persistSnapshot() {
        val pending = synchronized(lock) {
            pruneEventsLocked()
            val owner = activeAccountUserId ?: return@synchronized null
            owner to FriendActivityCache(
                statsByFriendId = tracker.snapshotForPersistence(Clock.System.now().toEpochMilliseconds()),
                schemaVersion = FriendActivityCache.CURRENT_SCHEMA_VERSION,
                importedVrcxEventKeys = importedVrcxEventKeys,
                importedVrcmomoEventKeys = importedVrcmomoEventKeys,
                activityEvents = tracker.eventLog,
            )
        } ?: return
        writer.submit(pending.first, pending.second)
    }

    private fun persistLocked() {
        val owner = activeAccountUserId ?: return
        writer.submit(
            owner,
            FriendActivityCache(
                statsByFriendId = tracker.snapshotForPersistence(Clock.System.now().toEpochMilliseconds()),
                schemaVersion = FriendActivityCache.CURRENT_SCHEMA_VERSION,
                importedVrcxEventKeys = importedVrcxEventKeys,
                importedVrcmomoEventKeys = importedVrcmomoEventKeys,
                activityEvents = tracker.eventLog,
            ),
        )
    }

    private fun pruneEventsLocked() {
        val days = settingsDao?.settings?.activityLogRetentionDays ?: return
        tracker.pruneEventsBefore(Clock.System.now().toEpochMilliseconds() - days * MILLIS_PER_DAY)
    }

    private fun publishLocked(save: Boolean) {
        val snapshot = tracker.snapshot
        _friendActivityState.value = snapshot
        _activityLog.value = tracker.eventLog
        if (save) persistLocked()
    }

    private fun String.toEpochMillisOrNull(): Long? =
        takeIf(String::isNotBlank)?.let { raw ->
            runCatching { Instant.parse(raw).toEpochMilliseconds() }.getOrNull()
        }

    private companion object {
        const val PERSIST_INTERVAL_MILLIS = 30_000L
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }
}

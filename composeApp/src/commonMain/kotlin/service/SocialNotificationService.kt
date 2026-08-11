package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.core.shared.AccountSessionToken
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.attributes.FavoriteType
import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.presentation.notifications.PlatformNotificationService
import io.github.vrcmteam.vrcm.presentation.notifications.SystemNotification
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationItemData
import io.github.vrcmteam.vrcm.storage.SettingsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Turns VRCMomo's in-process social events into platform local notifications.
 *
 * Friend presence notifications use the favorite-group and per-friend selection. Presence is derived
 * from the complete friend state instead of only friend-online/friend-offline events, because
 * VRChat can report the same transition through active, location, update, or a refreshed snapshot.
 */
@OptIn(ExperimentalTime::class)
class SocialNotificationService(
    private val platformNotificationService: PlatformNotificationService,
    private val favoriteService: FavoriteService,
    private val friendService: FriendService,
    private val settingsDao: SettingsDao,
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMutex = Mutex()
    private val presenceTracker = FavoriteFriendPresenceTracker()

    private var started = false
    private var activeSessionToken: AccountSessionToken? = null
    private var hasLivePresenceBaseline = false
    private var favoriteCollector: Job? = null
    private var favoriteGroupIdsByUser: Map<String, Set<String>> = emptyMap()
    private var knownFriendProfiles: Map<String, FriendData> = emptyMap()

    fun start() {
        if (started) return
        started = true
        platformNotificationService.requestPermission()

        serviceScope.launch {
            SharedFlowCentre.currentSession.collect { session ->
                favoriteCollector?.cancel()
                stateMutex.withLock {
                    activeSessionToken = session?.token
                    hasLivePresenceBaseline = false
                    favoriteGroupIdsByUser = emptyMap()
                    knownFriendProfiles = emptyMap()
                    presenceTracker.reset()
                }

                session?.let { authenticatedAccount ->
                    favoriteCollector = serviceScope.launch {
                        favoriteService.loadFavoriteByGroup(FavoriteType.Friend)
                        favoriteService.favoritesByGroup(FavoriteType.Friend).collect { groups ->
                            val groupIdsByUser = buildMap<String, MutableSet<String>> {
                                groups.forEach { (group, favorites) ->
                                    favorites.forEach { favorite ->
                                        getOrPut(favorite.favoriteId) { mutableSetOf() } += group.id
                                    }
                                }
                            }
                            val friendSnapshot = friendService.friendMap
                            stateMutex.withLock {
                                if (activeSessionToken == authenticatedAccount.token) {
                                    favoriteGroupIdsByUser = groupIdsByUser
                                    presenceTracker.updateFavorites(
                                        settingsDao.settings.friendPresenceNotificationSelection
                                            .selectedUserIds(groupIdsByUser),
                                        friendSnapshot,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            friendService.friendStateSnapshots.collect(::handleFriendSnapshot)
        }
    }

    fun notifyBoop(notification: NotificationItemData) {
        if (!settingsDao.settings.isSystemNotificationsEnabled) return
        val senderName = notification.title?.trim().takeUnless { it.isNullOrEmpty() } ?: "未知用户"
        val emojiLabel = notification.boopEmojiLabel
        val reactionText = emojiLabel?.let { "（$it）" }.orEmpty()
        platformNotificationService.show(
            SystemNotification(
                id = "boop-${notification.id}",
                title = "$senderName 戳了你一下$reactionText",
                message = emojiLabel?.let { "使用了 $it 表情。打开 VRCMomo 查看并回戳。" }
                    ?: "打开 VRCMomo 查看并回戳。",
            ),
        )
    }

    fun notifyFriendRequest(notification: NotificationItemData) {
        if (!settingsDao.settings.isSystemNotificationsEnabled) return
        val senderName = notification.title?.trim().takeUnless { it.isNullOrEmpty() }
            ?: notification.message.trim().takeIf(String::isNotEmpty)
            ?: "新的好友请求"
        platformNotificationService.show(
            SystemNotification(
                id = "friend-request-${notification.id}",
                title = senderName,
                message = "收到好友请求",
            ),
        )
    }

    private suspend fun handleFriendSnapshot(snapshot: FriendStateSnapshot) {
        val notifications = stateMutex.withLock {
            if (activeSessionToken == null) {
                FriendSnapshotNotifications()
            } else if (!snapshot.isLiveObservation) {
                // Friend IDs from the durable cache are safe as a relationship baseline even
                // though their presence fields are intentionally rewritten as offline.
                knownFriendProfiles = snapshot.friends
                FriendSnapshotNotifications()
            } else {
                presenceTracker.updateFavorites(
                    settingsDao.settings.friendPresenceNotificationSelection
                        .selectedUserIds(favoriteGroupIdsByUser),
                    snapshot.friends,
                )
                if (!hasLivePresenceBaseline) {
                // Cached rows deliberately carry offline presence so the UI is safe to render.
                // Do not compare the first real snapshot to that synthetic cache state.
                    presenceTracker.establishLiveBaseline(snapshot.friends)
                    val cachedFriends = knownFriendProfiles
                    knownFriendProfiles = snapshot.friends
                    hasLivePresenceBaseline = true
                    if (cachedFriends.isEmpty()) {
                        FriendSnapshotNotifications()
                    } else {
                        FriendSnapshotNotifications(
                            added = snapshot.friends.filterKeys { it !in cachedFriends }.values.toList(),
                            removed = cachedFriends.filterKeys { it !in snapshot.friends }.values.toList(),
                        )
                    }
                } else {
                    val previousFriends = knownFriendProfiles
                    knownFriendProfiles = snapshot.friends
                    FriendSnapshotNotifications(
                        presence = presenceTracker.observe(snapshot.friends),
                        added = snapshot.friends
                            .filterKeys { it !in previousFriends }
                            .values
                            .toList(),
                        removed = previousFriends
                            .filterKeys { it !in snapshot.friends }
                            .values
                            .toList(),
                    )
                }
            }
        }
        notifications.presence.forEach(::notifyPresenceTransition)
        notifications.added.forEach(::notifyFriendAdded)
        notifications.removed.forEach(::notifyFriendRemoved)
    }

    private fun notifyPresenceTransition(transition: FriendPresenceTransition) {
        if (!settingsDao.settings.isSystemNotificationsEnabled) return
        val status = if (transition.inGame) "进入 VRChat 了" else "离开 VRChat 了"
        platformNotificationService.show(
            SystemNotification(
                id = "friend-presence-${transition.userId}-${nowMillis()}",
                title = "${transition.displayName} $status",
                message = if (transition.inGame) {
                    "你收藏的好友现在正在游戏中。"
                } else {
                    "你收藏的好友已离开游戏；仅登录网页端不会计为在线。"
                },
            ),
        )
    }

    private fun notifyFriendAdded(friend: FriendData) {
        if (!settingsDao.settings.isSystemNotificationsEnabled) return
        platformNotificationService.show(
            SystemNotification(
                id = "friend-added-${friend.id}-${nowMillis()}",
                title = "${friend.displayName} 已成为你的好友",
                message = "好友关系已更新",
            ),
        )
    }

    private fun notifyFriendRemoved(friend: FriendData) {
        if (!settingsDao.settings.isSystemNotificationsEnabled) return
        platformNotificationService.show(
            SystemNotification(
                id = "friend-removed-${friend.id}-${nowMillis()}",
                title = "${friend.displayName} 已不在好友列表",
                message = "好友关系已更新",
            ),
        )
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

private data class FriendSnapshotNotifications(
    val presence: List<FriendPresenceTransition> = emptyList(),
    val added: List<FriendData> = emptyList(),
    val removed: List<FriendData> = emptyList(),
)

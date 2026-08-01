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

/**
 * Turns VRCMomo's in-process social events into platform local notifications.
 *
 * Friend presence notifications are restricted to VRChat friend favorites. Presence is derived
 * from the complete friend state instead of only friend-online/friend-offline events, because
 * VRChat can report the same transition through active, location, update, or a refreshed snapshot.
 */
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
    private var favoriteCollector: Job? = null

    fun start() {
        if (started) return
        started = true
        platformNotificationService.requestPermission()

        serviceScope.launch {
            SharedFlowCentre.currentSession.collect { session ->
                favoriteCollector?.cancel()
                stateMutex.withLock {
                    activeSessionToken = session?.token
                    presenceTracker.reset()
                }

                session?.let { authenticatedAccount ->
                    favoriteCollector = serviceScope.launch {
                        favoriteService.loadFavoriteByGroup(FavoriteType.Friend)
                        favoriteService.favoritesByGroup(FavoriteType.Friend).collect { groups ->
                            val favoriteIds = groups.values
                                .asSequence()
                                .flatten()
                                .map { it.favoriteId }
                                .toSet()
                            val friendSnapshot = friendService.friendMap
                            stateMutex.withLock {
                                if (activeSessionToken == authenticatedAccount.token) {
                                    presenceTracker.updateFavorites(favoriteIds, friendSnapshot)
                                }
                            }
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            friendService.friendState.collect(::handleFriendSnapshot)
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

    private suspend fun handleFriendSnapshot(friends: Map<String, FriendData>) {
        val transitions = stateMutex.withLock {
            if (activeSessionToken == null) emptyList() else presenceTracker.observe(friends)
        }
        transitions.forEach(::notifyPresenceTransition)
    }

    private fun notifyPresenceTransition(transition: FriendPresenceTransition) {
        if (!settingsDao.settings.isSystemNotificationsEnabled) return
        val status = if (transition.inGame) "进入 VRChat 了" else "离开 VRChat 了"
        platformNotificationService.show(
            SystemNotification(
                id = "friend-presence-${transition.userId}",
                title = "${transition.displayName} $status",
                message = if (transition.inGame) {
                    "你收藏的好友现在正在游戏中。"
                } else {
                    "你收藏的好友已离开游戏；仅登录网页端不会计为在线。"
                },
            ),
        )
    }
}

package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.core.shared.AccountSessionToken
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.attributes.FavoriteType
import io.github.vrcmteam.vrcm.presentation.notifications.PlatformNotificationService
import io.github.vrcmteam.vrcm.presentation.notifications.SystemNotification
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationItemData
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
 * Friend presence notifications are restricted to VRChat friend favorites. The
 * app does not synthesize an initial offline notification when a session starts,
 * preventing a cache refresh from flooding the notification tray.
 */
class SocialNotificationService(
    private val platformNotificationService: PlatformNotificationService,
    private val favoriteService: FavoriteService,
    private val friendService: FriendService,
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMutex = Mutex()

    private var started = false
    private var activeSessionToken: AccountSessionToken? = null
    private var favoriteFriendIds: Set<String> = emptySet()
    private val knownPresence = mutableMapOf<String, Boolean>()
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
                    favoriteFriendIds = emptySet()
                    knownPresence.clear()
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
                            stateMutex.withLock {
                                if (activeSessionToken == authenticatedAccount.token) {
                                    favoriteFriendIds = favoriteIds
                                }
                            }
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            friendService.friendUpdateFlow.collect(::handleFriendUpdate)
        }
    }

    fun notifyBoop(notification: NotificationItemData) {
        val senderName = notification.title?.trim().takeUnless { it.isNullOrEmpty() } ?: "\u672a\u77e5\u7528\u6237"
        platformNotificationService.show(
            SystemNotification(
                id = "boop-${notification.id}",
                title = "$senderName \u6233\u4e86\u4f60\u4e00\u4e0b",
                message = "\u6253\u5f00 VRCMomo \u67e5\u770b\u5e76\u56de\u6233\u3002",
            ),
        )
    }

    private suspend fun handleFriendUpdate(accountEvent: AccountFriendUpdateEvent) {
        val presence = when (val event = accountEvent.event) {
            is FriendUpdateEvent.Online -> FriendPresence(event.friend.id, event.friend.displayName, true)
            is FriendUpdateEvent.Offline -> {
                val friend = friendService.friendMap[event.userId] ?: return
                FriendPresence(friend.id, friend.displayName, false)
            }
            else -> return
        }

        val shouldNotify = stateMutex.withLock {
            if (accountEvent.sessionToken != activeSessionToken || presence.userId !in favoriteFriendIds) {
                false
            } else {
                val previous = knownPresence[presence.userId]
                knownPresence[presence.userId] = presence.online
                if (presence.online) {
                    previous != true
                } else {
                    // Do not notify for a first-seen offline cache state.
                    previous == true
                }
            }
        }
        if (!shouldNotify) return

        val status = if (presence.online) "\u4e0a\u7ebf\u4e86" else "\u4e0b\u7ebf\u4e86"
        platformNotificationService.show(
            SystemNotification(
                id = "friend-presence-${presence.userId}",
                title = "${presence.displayName} $status",
                message = "\u4f60\u6536\u85cf\u7684\u597d\u53cb$status\u3002",
            ),
        )
    }

    private data class FriendPresence(
        val userId: String,
        val displayName: String,
        val online: Boolean,
    )
}

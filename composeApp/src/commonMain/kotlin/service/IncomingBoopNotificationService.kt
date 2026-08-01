package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.attributes.NotificationType
import io.github.vrcmteam.vrcm.network.api.notification.NotificationApi
import io.github.vrcmteam.vrcm.network.api.users.UsersApi
import io.github.vrcmteam.vrcm.network.websocket.data.type.NotificationEvents
import io.github.vrcmteam.vrcm.presentation.screens.home.data.BoopNotificationResolver
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationItemData
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationUserPresentation
import io.github.vrcmteam.vrcm.presentation.screens.home.data.isUnreadBoop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps Boop tray notifications working while the user-enabled Android foreground monitor owns
 * the process. UI popups are still handled by HomeScreenModel when the app is visible.
 */
class IncomingBoopNotificationService(
    private val authService: AuthService,
    private val notificationApi: NotificationApi,
    private val usersApi: UsersApi,
    private val friendService: FriendService,
    private val socialNotificationService: SocialNotificationService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val resolver = BoopNotificationResolver()
    private val knownNotificationIds = mutableSetOf<String>()
    private var initialized = false
    private var monitorJob: Job? = null

    fun start() {
        if (monitorJob != null) return
        monitorJob = scope.launch {
            launch { refresh(seedOnly = true) }
            SharedFlowCentre.webSocket.collect { event ->
                if (event.event.type == NotificationEvents.Notification.typeName) {
                    refresh(seedOnly = false)
                }
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        initialized = false
        knownNotificationIds.clear()
    }

    private suspend fun refresh(seedOnly: Boolean) {
        refreshMutex.withLock {
            val legacyNotifications = authService.reTryAuthCatching {
                notificationApi.fetchNotifications()
            }.getOrDefault(emptyList())
            val v2Notifications = authService.reTryAuthCatching {
                notificationApi.fetchNotificationsV2()
            }.getOrDefault(emptyList())
                .filter { it.type == NotificationType.Boop.value }
                .map(::NotificationItemData)
                .associateBy { it.id }

            if (legacyNotifications.isEmpty() && v2Notifications.isEmpty()) return

            val legacyBoops = legacyNotifications.map(::NotificationItemData)
                .filter { it.type == NotificationType.Boop.value }
            val legacyIds = legacyBoops.mapTo(mutableSetOf()) { it.id }
            val boopItems = legacyBoops.map { legacy ->
                val v2 = v2Notifications[legacy.id]
                if (v2 == null) legacy else legacy.copy(
                    seen = v2.seen,
                    boopEmojiId = v2.boopEmojiId,
                    boopEmojiVersion = v2.boopEmojiVersion,
                    boopInventoryItemId = v2.boopInventoryItemId,
                )
            } + v2Notifications.values.filterNot { it.id in legacyIds }

            val friends = friendService.friendMap.mapValues { (_, friend) ->
                NotificationUserPresentation(friend.profileImageUrl, friend.displayName)
            }
            val boops = resolver.resolve(
                notifications = boopItems,
                friends = friends,
            ) { userId ->
                usersApi.fetchUser(userId).let { user ->
                    NotificationUserPresentation(user.profileImageUrl, user.displayName)
                }
            }

            if (!initialized || seedOnly) {
                knownNotificationIds += boops.map { it.id }
                initialized = true
                // The foreground monitor may be started after the Boop was delivered. Surface
                // unread notifications once instead of treating them as permanently "old".
                boops.filter { it.isUnreadBoop }
                    .sortedBy { it.createdAt }
                    .forEach { boop ->
                        socialNotificationService.notifyBoop(boop)
                        markSeen(boop.id)
                    }
                return
            }
            boops.asSequence()
                .filter { knownNotificationIds.add(it.id) }
                .sortedBy { it.createdAt }
                .forEach { boop ->
                    socialNotificationService.notifyBoop(boop)
                    markSeen(boop.id)
                }
        }
    }

    private suspend fun markSeen(notificationId: String) {
        val v2Result = authService.reTryAuthCatching {
            notificationApi.acknowledgeNotificationV2(notificationId)
        }
        if (v2Result.isFailure) {
            authService.reTryAuthCatching {
                notificationApi.markNotificationAsRead(notificationId)
            }
        }
    }
}

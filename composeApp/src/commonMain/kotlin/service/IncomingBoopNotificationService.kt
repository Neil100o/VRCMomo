package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.attributes.NotificationType
import io.github.vrcmteam.vrcm.network.api.notification.NotificationApi
import io.github.vrcmteam.vrcm.network.api.users.UsersApi
import io.github.vrcmteam.vrcm.network.websocket.WebSocketApi
import io.github.vrcmteam.vrcm.network.websocket.WebSocketConnectionState
import io.github.vrcmteam.vrcm.network.websocket.data.type.NotificationEvents
import io.github.vrcmteam.vrcm.presentation.screens.home.data.BoopNotificationResolver
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationItemData
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationUserPresentation
import io.github.vrcmteam.vrcm.storage.SettingsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
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
    private val webSocketApi: WebSocketApi,
    private val settingsDao: SettingsDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val resolver = BoopNotificationResolver()
    private val knownNotificationIds = mutableSetOf<String>()
    private var initialized = false
    private var monitorJob: Job? = null

    fun start() {
        if (monitorJob != null) return
        knownNotificationIds += settingsDao.notifiedSocialNotificationIds
        monitorJob = scope.launch {
            launch {
                SharedFlowCentre.webSocket.collect { event ->
                    if (event.event.type == NotificationEvents.Notification.typeName) {
                        refresh(seedOnly = false)
                    }
                }
            }
            combine(
                SharedFlowCentre.currentSession,
                webSocketApi.connectionState,
            ) { session, connection -> session to connection }
                .collectLatest { (session, connection) ->
                    if (session == null) {
                        initialized = false
                        knownNotificationIds.clear()
                        return@collectLatest
                    }
                    refresh(seedOnly = !initialized)
                    while (true) {
                        delay(
                            if (connection is WebSocketConnectionState.Connected) {
                                CONNECTED_NOTIFICATION_REFRESH_INTERVAL_MILLIS
                            } else {
                                DISCONNECTED_NOTIFICATION_REFRESH_INTERVAL_MILLIS
                            },
                        )
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
                .map(::NotificationItemData)
                .associateBy { it.id }

            if (legacyNotifications.isEmpty() && v2Notifications.isEmpty()) return

            val legacyItems = legacyNotifications.map(::NotificationItemData)
            val legacyIds = legacyItems.mapTo(mutableSetOf()) { it.id }
            val notificationItems = legacyItems.map { legacy ->
                val v2 = v2Notifications[legacy.id]
                if (v2 == null) legacy else legacy.copy(
                    seen = v2.seen,
                    boopEmojiId = v2.boopEmojiId ?: legacy.boopEmojiId,
                    boopEmojiVersion = v2.boopEmojiVersion ?: legacy.boopEmojiVersion,
                    boopInventoryItemId = v2.boopInventoryItemId ?: legacy.boopInventoryItemId,
                )
            } + v2Notifications.values.filterNot { it.id in legacyIds }
            val boopItems = notificationItems.filter { it.type == NotificationType.Boop.value }

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
            val boopsById = boops.associateBy { it.id }

            if (!initialized || seedOnly) {
                knownNotificationIds += notificationItems.map { it.id }
                initialized = true
                // The foreground monitor may be started after the Boop was delivered. Surface
                // unread notifications once instead of treating them as permanently "old".
                notificationItems
                    .filter { it.seen == false }
                    .sortedBy { it.createdAt }
                    .forEach { notifyIncoming(it, boopsById) }
                persistKnownNotificationIds()
                return
            }
            notificationItems.asSequence()
                .filter { knownNotificationIds.add(it.id) }
                .sortedBy { it.createdAt }
                .forEach { notifyIncoming(it, boopsById) }
            persistKnownNotificationIds()
        }
    }

    private suspend fun notifyIncoming(
        notification: NotificationItemData,
        boopsById: Map<String, NotificationItemData>,
    ) {
        when (notification.type) {
            NotificationType.Boop.value -> {
                socialNotificationService.notifyBoop(boopsById[notification.id] ?: notification)
                markSeen(notification.id)
            }
            NotificationType.FriendRequest.value -> {
                socialNotificationService.notifyFriendRequest(notification)
            }
            in GROUP_NOTIFICATION_TYPES -> {
                socialNotificationService.notifyGroupMessage(notification)
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

    private fun persistKnownNotificationIds() {
        settingsDao.notifiedSocialNotificationIds = knownNotificationIds
    }

    private companion object {
        const val CONNECTED_NOTIFICATION_REFRESH_INTERVAL_MILLIS = 5 * 60_000L
        const val DISCONNECTED_NOTIFICATION_REFRESH_INTERVAL_MILLIS = 60_000L

        // Invites are deliberately excluded: if someone is inviting the user, VRChat itself is
        // normally the more useful place to surface it. Keep announcements and actionable group
        // administration messages available while the game is closed.
        val GROUP_NOTIFICATION_TYPES = setOf(
            "groupChange",
            "group.announcement",
            "group.event.created",
            "group.event.starting",
            "group.informative",
            "group.joinRequest",
            "group.transfer",
            "group.queueReady",
        )
    }
}

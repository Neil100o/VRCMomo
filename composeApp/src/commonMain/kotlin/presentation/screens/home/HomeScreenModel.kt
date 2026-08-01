package io.github.vrcmteam.vrcm.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.attributes.NotificationType
import io.github.vrcmteam.vrcm.network.api.attributes.UserStatus
import io.github.vrcmteam.vrcm.network.api.auth.data.CurrentUserData
import io.github.vrcmteam.vrcm.network.api.notification.NotificationApi
import io.github.vrcmteam.vrcm.network.api.users.UsersApi
import io.github.vrcmteam.vrcm.network.api.users.isBoopAlreadySentError
import io.github.vrcmteam.vrcm.network.api.users.data.UpdateUserInfoData
import io.github.vrcmteam.vrcm.network.websocket.data.type.NotificationEvents
import io.github.vrcmteam.vrcm.presentation.compoments.ToastText
import io.github.vrcmteam.vrcm.presentation.extensions.onApiFailure
import io.github.vrcmteam.vrcm.presentation.screens.home.data.BoopNotificationResolver
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationItemData
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationResponseTarget
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationUserPresentation
import io.github.vrcmteam.vrcm.presentation.screens.home.data.isUnreadBoop
import io.github.vrcmteam.vrcm.presentation.screens.home.data.responseTarget
import io.github.vrcmteam.vrcm.presentation.screens.home.pager.FriendLocationPagerModel
import io.github.vrcmteam.vrcm.service.AuthService
import io.github.vrcmteam.vrcm.service.FriendService
import io.github.vrcmteam.vrcm.service.SocialNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.logger.Logger


class HomeScreenModel(
    private val authService: AuthService,
    private val usersApi: UsersApi,
    private val notificationApi: NotificationApi,
    private val friendService: FriendService,
    private val socialNotificationService: SocialNotificationService,
    private val friendLocationPagerModel: FriendLocationPagerModel,
    private val logger: Logger,
) : ScreenModel {

    private val boopNotificationResolver = BoopNotificationResolver()
    private var initialized = false

    private val _currentUser = mutableStateOf<CurrentUserData?>(null)

    val userId: String
        get() = authService.accountDto().userId

    val iconUrl: String
        get() = authService.accountDto().iconUrl.orEmpty()

    var currentUser by _currentUser

    private val _notifications = mutableStateOf<List<NotificationItemData>>(emptyList())
    val notifications by _notifications

    private val _friendRequestNotifications = mutableStateOf<List<NotificationItemData>>(emptyList())
    val friendRequestNotifications by _friendRequestNotifications

    private val notificationStateMutex = Mutex()
    private val knownNotificationIds = mutableSetOf<String>()
    private val pendingReceivedBoops = mutableListOf<NotificationItemData>()
    private var notificationSnapshotInitialized = false

    private val _receivedBoop = mutableStateOf<NotificationItemData?>(null)
    val receivedBoop by _receivedBoop

    fun init() {
        if (initialized) return
        initialized = true
        socialNotificationService.start()
        friendService.preloadFriendList()
        friendLocationPagerModel.preloadFriendLocations()
        refreshCurrentUser()
        refreshFriendRequestNotification()
        refreshNotifications()
        screenModelScope.launch {
            SharedFlowCentre.webSocket.collect { event ->
                if (event.event.type == NotificationEvents.Notification.typeName) {
                    refreshAllNotification()
                }
            }
        }
    }

    fun refreshAllNotification() {
        refreshFriendRequestNotification()
        refreshNotifications()
    }

    private fun refreshFriendRequestNotification() =
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching { notificationApi.fetchFriendRequestNotifications() }
                .onHomeFailure()
                .onSuccess {
                    runCatching {
                        _friendRequestNotifications.value = it.map { data ->
                            val user = usersApi.fetchUser(data.senderUserId)
                            NotificationItemData(
                                id = data.id,
                                imageUrl = user.profileImageUrl,
                                title = user.displayName,
                                message = user.displayName,
                                createdAt = data.createdAt,
                                senderUserId = data.senderUserId,
                                link = "user:${data.senderUserId}",
                                type = data.type,
                                actions = listOf(
                                    NotificationItemData.ActionData(
                                        data = "",
                                        type = "Hide"
                                    ),
                                    NotificationItemData.ActionData(
                                        data = "",
                                        type = "Accept"
                                    )
                                )
                            )
                        }
                    }.onHomeFailure()
                }
        }

    private fun refreshNotifications() =
        screenModelScope.launch(Dispatchers.IO) {
            val legacyResult = authService.reTryAuthCatching { notificationApi.fetchNotifications() }
            val v2Result = authService.reTryAuthCatching { notificationApi.fetchNotificationsV2() }
            if (legacyResult.isFailure && v2Result.isFailure) {
                legacyResult.onHomeFailure()
                return@launch
            }

            val legacyItems = legacyResult.getOrDefault(emptyList()).map(::NotificationItemData)
            val v2Items = v2Result.getOrDefault(emptyList())
                .filter { it.type == NotificationType.Boop.value }
                .map(::NotificationItemData)
            val v2ById = v2Items.associateBy { it.id }
            val mergedItems = legacyItems.map { legacy ->
                val v2 = v2ById[legacy.id]
                if (v2 == null) legacy else legacy.copy(
                    seen = v2.seen,
                    boopEmojiId = v2.boopEmojiId,
                    boopEmojiVersion = v2.boopEmojiVersion,
                    boopInventoryItemId = v2.boopInventoryItemId,
                )
            }.let { existing ->
                val existingIds = existing.mapTo(mutableSetOf()) { it.id }
                existing + v2Items.filterNot { it.id in existingIds }
            }

            val friendPresentations = friendService.friendMap.mapValues { (_, friend) ->
                NotificationUserPresentation(
                    imageUrl = friend.profileImageUrl,
                    displayName = friend.displayName,
                )
            }
            val resolvedNotifications = boopNotificationResolver.resolve(
                notifications = mergedItems,
                friends = friendPresentations,
            ) { userId ->
                usersApi.fetchUser(userId).let { user ->
                    NotificationUserPresentation(
                        imageUrl = user.profileImageUrl,
                        displayName = user.displayName,
                    )
                }
            }
            notificationStateMutex.withLock {
                updateNotificationState(resolvedNotifications)
            }
        }

    private fun updateNotificationState(items: List<NotificationItemData>) {
        if (!notificationSnapshotInitialized) {
            knownNotificationIds += items.map { it.id }
            notificationSnapshotInitialized = true
            // A Boop can arrive while Android has stopped the app. Do not silently discard
            // unread server notifications just because this is the first local snapshot.
            val unreadBoops = items.filter { it.isUnreadBoop }
            unreadBoops.forEach(pendingReceivedBoops::add)
            unreadBoops.forEach(::markBoopSeen)
            showNextReceivedBoopIfNeeded()
        } else {
            val newBoops = items.asSequence()
                .filter { it.type == NotificationType.Boop.value }
                .filter { knownNotificationIds.add(it.id) }
                .sortedBy { it.createdAt }
                .toList()
            newBoops.forEach { notification ->
                pendingReceivedBoops.add(notification)
                socialNotificationService.notifyBoop(notification)
                markBoopSeen(notification)
            }
            showNextReceivedBoopIfNeeded()
        }
        _notifications.value = items
    }

    private fun markBoopSeen(notification: NotificationItemData) {
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching {
                notificationApi.markNotificationAsRead(notification.id)
            }.onFailure { logger.warn("Could not mark Boop ${notification.id} as seen: ${it.message}") }
        }
    }

    private fun showNextReceivedBoopIfNeeded() {
        if (_receivedBoop.value == null && pendingReceivedBoops.isNotEmpty()) {
            _receivedBoop.value = pendingReceivedBoops.removeAt(0)
        }
    }

    fun dismissReceivedBoop() {
        screenModelScope.launch {
            notificationStateMutex.withLock {
                _receivedBoop.value = null
                showNextReceivedBoopIfNeeded()
            }
        }
    }

    fun responseAllNotification(
        item: NotificationItemData,
        action: NotificationItemData.ActionData,
        boopSuccessMessage: String,
        boopAlreadySentMessage: String,
    ) {
        when (item.responseTarget(action)) {
            NotificationResponseTarget.BOOP_USER_API -> {
                item.senderId?.let { boopUser(it, boopSuccessMessage, boopAlreadySentMessage) }
                return
            }
            NotificationResponseTarget.NOTIFICATION_API -> Unit
        }

        val id = item.id
        val type = item.type
        if (type == NotificationType.FriendRequest.value) {
            responseFriendRequest(id, action)
        } else {
            responseNotification(id, action)
        }
    }

    private fun responseFriendRequest(id: String, response: NotificationItemData.ActionData) {
        if (response.type == "Accept") {
            acceptFriendRequest(id)
        } else {
            hideNotification(id)
        }
    }

    private fun responseNotification(id: String, response: NotificationItemData.ActionData) = notificationAction {
        notificationApi.responseNotification(id, response)
    }

    private fun boopUser(userId: String, successMessage: String, alreadySentMessage: String) {
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching { usersApi.boop(userId) }
                .onSuccess {
                    SharedFlowCentre.toastText.emit(ToastText.Success(successMessage))
                }
                .onFailure { error ->
                    if (error.isBoopAlreadySentError()) {
                        SharedFlowCentre.toastText.emit(ToastText.Info(alreadySentMessage))
                    } else {
                        Result.failure<Unit>(error).onHomeFailure()
                    }
                }
        }
    }


    private fun acceptFriendRequest(notificationId: String) = notificationAction {
        notificationApi.acceptFriendRequest(notificationId)
    }

    private fun hideNotification(notificationId: String) = notificationAction {
        notificationApi.deleteNotification(notificationId)
    }

    private fun notificationAction(action: suspend () -> Unit) {
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching { action() }
                .onHomeFailure()
                .onSuccess {
                    runCatching { refreshAllNotification() }
                        .onHomeFailure()
                }
        }
    }

    private fun refreshCurrentUser() =
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching { authService.currentUser(isRefresh = true) }
                .onHomeFailure()
                .onSuccess {
                    _currentUser.value = it
                }
        }


    private inline fun <T> Result<T>.onHomeFailure() =
        onApiFailure("Home") {
            logger.error(it)
            screenModelScope.launch {
                SharedFlowCentre.toastText.emit(ToastText.Error(it))
            }
        }
    fun updateUserStatus(userStatus: UserStatus, statusDescription: String) {
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching {
                usersApi.updateUserInfo(
                    userId = userId,
                    updateUserInfoData = UpdateUserInfoData(
                        status = userStatus,
                        statusDescription = statusDescription
                    )
                )
            }.onHomeFailure()
                .onSuccess {
                    refreshCurrentUser()
                }
        }
    }


}





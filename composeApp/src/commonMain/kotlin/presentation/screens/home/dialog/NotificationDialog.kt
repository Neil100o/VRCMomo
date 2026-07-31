package io.github.vrcmteam.vrcm.presentation.screens.home.dialog

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.vrcmteam.vrcm.core.extensions.capitalizeFirst
import io.github.vrcmteam.vrcm.network.api.attributes.NotificationType
import io.github.vrcmteam.vrcm.presentation.compoments.AImage
import io.github.vrcmteam.vrcm.presentation.compoments.SharedDialog
import io.github.vrcmteam.vrcm.presentation.compoments.SharedDialogContainer
import io.github.vrcmteam.vrcm.presentation.compoments.sharedBoundsBy
import io.github.vrcmteam.vrcm.presentation.extensions.enableIf
import io.github.vrcmteam.vrcm.presentation.extensions.ignoredFormat
import io.github.vrcmteam.vrcm.presentation.extensions.koinScreenModelByLastItem
import io.github.vrcmteam.vrcm.presentation.screens.home.HomeScreenModel
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationItemData
import io.github.vrcmteam.vrcm.presentation.screens.user.UserProfileScreen
import io.github.vrcmteam.vrcm.presentation.screens.user.data.UserProfileVo
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.presentation.supports.AppIcons
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

object NotificationDialog : SharedDialog {

    @Composable
    override fun Content(animatedVisibilityScope: AnimatedVisibilityScope) {
        val homeScreenModel: HomeScreenModel = koinScreenModelByLastItem()
        // 每打开一次刷新一次
        LaunchedEffect(Unit) {
            homeScreenModel.refreshAllNotification()
        }
        val notifications: List<NotificationItemData> by remember {
            derivedStateOf {
                (homeScreenModel.friendRequestNotifications + homeScreenModel.notifications)
                    .sortedByDescending { it.createdAt }
            }
        }
        val boopSuccessMessage = strings.profileBoopSuccess
        val boopAlreadySentMessage = strings.profileBoopAlreadySent
        val onResponseNotification: (NotificationItemData, NotificationItemData.ActionData) -> Unit = { item, response ->
            homeScreenModel.responseAllNotification(
                item = item,
                action = response,
                boopSuccessMessage = boopSuccessMessage,
                boopAlreadySentMessage = boopAlreadySentMessage,
            )
        }

        AnimatedContent(
            modifier = Modifier.animateContentSize(),
            targetState = notifications,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        slideInVertically(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            }
        ) { notificationItemDataList ->
            if (notificationItemDataList.isEmpty()) {
                Text(
                    modifier = Modifier.padding(6.dp),
                    text = strings.homeNotificationEmpty,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                SharedDialogContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(notificationItemDataList) { item ->
                            NotificationItem(item, onResponseNotification)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LazyItemScope.NotificationItem(
    item: NotificationItemData,
    onResponse: (NotificationItemData, NotificationItemData.ActionData) -> Unit,
) {
    var isExpand by remember { mutableStateOf(false) }
    var respondedAction by remember { mutableStateOf<NotificationItemData.ActionData?>(null) }
    val isFriendRequest = item.type == NotificationType.FriendRequest.value
    val actions = if (isFriendRequest) {
        item.actions.sortedBy { it.type != "Accept" }
    } else {
        item.actions
    }
    val senderId = item.senderId.orEmpty()
    val linkedUserId = item.linkedUserId.orEmpty()
    val contentText = if (isFriendRequest) "${item.message} ${strings.notificationFriendRequest}" else item.message
    val navigator = LocalNavigator.currentOrThrow
    Card(
        modifier = Modifier.fillMaxWidth().animateItem()
            .clip(MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = if (isFriendRequest) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.height(72.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AImage(
                    modifier = Modifier
                        .enableIf(senderId.isNotEmpty()) {
                            this.clickable {
                                navigator push UserProfileScreen(
                                    userProfileVO = UserProfileVo(
                                        id = senderId,
                                        profileImageUrl = item.imageUrl
                                    )
                                )
                            }.sharedBoundsBy("${senderId}UserIcon")
                        }
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium)
                        .clip(MaterialTheme.shapes.medium),
                    imageData = item.imageUrl
                )
                Column(modifier = Modifier.weight(1f)) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(text = item.title ?: item.message)
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = item.title ?: item.message,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isFriendRequest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (isFriendRequest) {
                        Text(
                            text = strings.profileSendFriendRequest,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = remember {
                            @OptIn(ExperimentalTime::class)
                            Instant.parse(item.createdAt).toLocalDateTime(TimeZone.currentSystemDefault()).ignoredFormat
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                actions.forEach { action ->
                    val actionLabel = if (isFriendRequest && action.type.equals("Hide", ignoreCase = true)) {
                        strings.cancel
                    } else {
                        action.type.capitalizeFirst()
                    }
                    val onClickAction = {
                        if (action.type.equals("link", ignoreCase = true) && linkedUserId.isNotEmpty()) {
                            navigator push UserProfileScreen(
                                userProfileVO = UserProfileVo(
                                    id = linkedUserId,
                                    profileImageUrl = item.imageUrl
                                )
                            )
                        } else {
                            respondedAction = action
                            onResponse(item, action)
                        }
                    }
                    if (isFriendRequest && action.type.equals("Accept", ignoreCase = true)) {
                        Button(
                            modifier = Modifier.weight(1f).animateContentSize(),
                            enabled = respondedAction != action,
                            onClick = onClickAction,
                        ) {
                            Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        OutlinedButton(
                            modifier = Modifier.animateContentSize(),
                            enabled = respondedAction != action,
                            onClick = onClickAction,
                        ) {
                            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                IconButton(
                    onClick = { isExpand = !isExpand }
                ) {
                    Icon(
                        imageVector = if (isExpand) AppIcons.ExpandLess else AppIcons.ExpandMore,
                        contentDescription = "ExpandIconButton"
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.small
                    )
                    .animateContentSize()
            ) {
                if (isExpand) {
                    Text(
                        modifier = Modifier.padding(6.dp),
                        text = contentText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }
    }
}

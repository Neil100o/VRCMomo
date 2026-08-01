package io.github.vrcmteam.vrcm.presentation.screens.home.pager

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import io.github.vrcmteam.vrcm.network.api.attributes.FavoriteType
import io.github.vrcmteam.vrcm.network.api.attributes.UserStatus
import io.github.vrcmteam.vrcm.presentation.compoments.SearchTabType
import io.github.vrcmteam.vrcm.presentation.compoments.StandardSearchList
import io.github.vrcmteam.vrcm.presentation.extensions.currentNavigator
import io.github.vrcmteam.vrcm.presentation.extensions.simpleClickable
import io.github.vrcmteam.vrcm.presentation.screens.home.compoments.GroupOptionsUI
import io.github.vrcmteam.vrcm.presentation.screens.user.FriendNetworkScreen
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.presentation.supports.AppIcons
import io.github.vrcmteam.vrcm.presentation.supports.Pager
import io.github.vrcmteam.vrcm.presentation.theme.GameColor
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.painterResource
import vrcm.composeapp.generated.resources.Res
import vrcm.composeapp.generated.resources.star

object FriendListPager : Pager {

    override val index: Int
        get() = 1
    override val title: String
        @Composable
        get() = strings.homePagerFriends

    override val icon: Painter
        @Composable get() = painterResource(Res.drawable.star)

    @Composable
    override fun Content() {
        val friendListPagerModel: FriendListPagerModel = koinScreenModel()

        // 搜索文本
        val searchText by friendListPagerModel.searchText.collectAsState()

        // 选中的标签页索引
        val selectedTabIndex by friendListPagerModel.selectedTabIndex.collectAsState()

        // 获取好友组列表、世界组列表、模型组列表
        val friendFavoriteGroups by friendListPagerModel.friendFavoriteGroupsFlow.collectAsState()
        val worldFavoriteGroups by friendListPagerModel.worldFavoriteGroupsFlow.collectAsState()
        val avatarFavoriteGroups by friendListPagerModel.avatarFavoriteGroupsFlow.collectAsState()

        // 获取分组选项状态
        val friendGroupOptions by friendListPagerModel.friendGroupOptions.collectAsState()
        val worldGroupOptions by friendListPagerModel.worldGroupOptions.collectAsState()
        val avatarGroupOptions by friendListPagerModel.avatarGroupOptions.collectAsState()

        // 获取总数
        val friendTotal by friendListPagerModel.friendTotal.collectAsState()
        val worldTotal by friendListPagerModel.worldTotal.collectAsState()
        val avatarTotal by friendListPagerModel.avatarTotal.collectAsState()
        // 获取列表数据
        val filteredFriends by friendListPagerModel.friendList.collectAsState()
        val friendActivityState by friendListPagerModel.friendActivityState.collectAsState()
        val filteredWorlds by friendListPagerModel.worldList.collectAsState()
        val filteredAvatars by friendListPagerModel.avatarList.collectAsState()
        val isRefreshing by friendListPagerModel.isRefreshing.collectAsState()
        // 初始加载缓存数据: 只有第一次默认为ture才会刷新一次
        LaunchedEffect(Unit) {
            if (isRefreshing) {
                friendListPagerModel.refreshCurrentTabCacheData(tabIndex = 0)
                friendListPagerModel.refreshCurrentTabCacheData(tabIndex = 1)
                friendListPagerModel.refreshCurrentTabCacheData(tabIndex = 2)
            }
        }

        StandardSearchList(
            key = title,
            searchText = searchText,
            updateSearchText = friendListPagerModel::setSearchText,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = friendListPagerModel::setSelectedTabIndex,
            isRefreshing = isRefreshing,
            doRefresh = friendListPagerModel::refreshCurrentTabCacheData,
            headerContent = {
                if (selectedTabIndex == SearchTabType.USER.index) {
                    Column {
                        FriendActivityOverviewCard(
                            friends = filteredFriends,
                            activityStats = friendActivityState,
                        )
                        RelationshipHubCard()
                    }
                }
            },
            userList = filteredFriends,
            worldList = filteredWorlds,
            avatarList = filteredAvatars,
            advancedOptionsContent = { tabType ->
                // 根据当前选中的标签页显示不同的高级选项
                when (tabType) {
                    SearchTabType.USER -> { // 好友标签页

                        GroupOptionsUI(
                            currentOptions = friendGroupOptions,
                            favoriteGroups = friendFavoriteGroups,
                            favoriteType = FavoriteType.Friend,
                            defaultText = strings.friendListPagerAllFriends,
                            total = friendTotal,
                            onOptionsChanged = { newOptions ->
                                friendListPagerModel.updateFriendGroupOptions(newOptions)
                            },
                            getSelectedGroup = { it.selectedGroup },
                            updateOptions = { options, group -> options.copy(selectedGroup = group) }
                        )

                    }

                    SearchTabType.WORLD -> { // 世界标签页

                        GroupOptionsUI(
                            currentOptions = worldGroupOptions,
                            favoriteGroups = worldFavoriteGroups,
                            favoriteType = FavoriteType.World,
                            total = worldTotal,
                            defaultText = strings.friendListPagerAllWorlds,
                            onOptionsChanged = { newOptions ->
                                friendListPagerModel.updateWorldGroupOptions(newOptions)
                            },
                            getSelectedGroup = { it.selectedGroup },
                            updateOptions = { options, group -> options.copy(selectedGroup = group) }
                        )

                    }

                    SearchTabType.AVATAR -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            FilterChip(
                                selected = avatarGroupOptions.showOwnUploads,
                                onClick = {
                                    friendListPagerModel.updateAvatarGroupOptions(
                                        avatarGroupOptions.copy(
                                            showOwnUploads = !avatarGroupOptions.showOwnUploads,
                                            selectedGroup = null,
                                        )
                                    )
                                },
                                label = { Text(strings.userCreatedAvatars) },
                            )
                        }
 // 模型标签页

                        GroupOptionsUI(
                            currentOptions = avatarGroupOptions,
                            favoriteGroups = avatarFavoriteGroups,
                            favoriteType = FavoriteType.Avatar,
                            total = avatarTotal,
                            defaultText = strings.friendListPagerAllAvatars,
                            onOptionsChanged = { newOptions ->
                                friendListPagerModel.updateAvatarGroupOptions(newOptions)
                            },
                            getSelectedGroup = { it.selectedGroup },
                            updateOptions = { options, group -> options.copy(selectedGroup = group) }
                        )

                    }

                    else -> {}
                }
            }
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun FriendActivityOverviewCard(
    friends: List<io.github.vrcmteam.vrcm.network.api.friends.date.FriendData>,
    activityStats: Map<String, FriendActivityStats>,
) {
    val localeStrings = strings
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val statusCounts = remember(friends) {
        UserStatus.entries.associateWith { status -> friends.count { it.status == status } }
    }
    val activityIntervals = remember(friends, activityStats, nowMillis / ACTIVITY_OVERVIEW_REFRESH_WINDOW) {
        val counts = IntArray(5)
        friends.forEach { friend ->
            val lastActivity = activityStats[friend.id]?.lastActivityAtMillis
            val index = when {
                lastActivity == null -> 4
                nowMillis - lastActivity <= HOUR_MILLIS -> 0
                nowMillis - lastActivity <= DAY_MILLIS -> 1
                nowMillis - lastActivity <= WEEK_MILLIS -> 2
                else -> 3
            }
            counts[index]++
        }
        counts
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(localeStrings.friendActivityOverviewTitle, style = MaterialTheme.typography.titleSmall)
            Text(
                localeStrings.friendActivityOverviewNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(localeStrings.friendActivityStatusDistribution, style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                UserStatus.entries.forEach { status ->
                    StatusLightMetric(status = status, count = statusCounts[status].orZero())
                }
            }
            Text(localeStrings.friendActivityIntervalDistribution, style = MaterialTheme.typography.labelLarge)
            ActivityIntervalRow(localeStrings.friendActivityIntervalHour, activityIntervals[0])
            ActivityIntervalRow(localeStrings.friendActivityIntervalDay, activityIntervals[1])
            ActivityIntervalRow(localeStrings.friendActivityIntervalWeek, activityIntervals[2])
            ActivityIntervalRow(localeStrings.friendActivityIntervalEarlier, activityIntervals[3])
            ActivityIntervalRow(localeStrings.friendActivityIntervalUnknown, activityIntervals[4])
        }
    }
}

@Composable
private fun StatusLightMetric(status: UserStatus, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(GameColor.Status.fromValue(status), CircleShape),
        )
        Text(count.toString(), style = MaterialTheme.typography.labelMedium)
        Text(
            status.value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActivityIntervalRow(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(count.toString(), style = MaterialTheme.typography.bodySmall)
    }
}

private fun Int?.orZero(): Int = this ?: 0

private const val HOUR_MILLIS = 60L * 60L * 1_000L
private const val DAY_MILLIS = 24L * HOUR_MILLIS
private const val WEEK_MILLIS = 7L * DAY_MILLIS
private const val ACTIVITY_OVERVIEW_REFRESH_WINDOW = 5L * 60L * 1_000L


@Composable
private fun RelationshipHubCard() {
    val navigator = currentNavigator

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .simpleClickable {
                if (navigator.size <= 1) {
                    navigator.push(FriendNetworkScreen)
                }
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(
                    modifier = Modifier.padding(9.dp),
                    painter = rememberVectorPainter(AppIcons.Groups),
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.relationshipHubTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = strings.relationshipHubDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                )
            }
            Text(
                text = strings.relationshipHubAction,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

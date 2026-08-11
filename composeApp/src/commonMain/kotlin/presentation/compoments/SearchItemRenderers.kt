package io.github.vrcmteam.vrcm.presentation.compoments

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.core.extensions.toLocalDateTime
import io.github.vrcmteam.vrcm.network.api.attributes.IUser
import io.github.vrcmteam.vrcm.network.api.attributes.UserStatus
import io.github.vrcmteam.vrcm.network.api.attributes.lastSeenAt
import io.github.vrcmteam.vrcm.network.api.avatars.data.AvatarData
import io.github.vrcmteam.vrcm.network.api.files.data.PlatformType.*
import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.network.api.groups.data.LimitedGroup
import io.github.vrcmteam.vrcm.network.api.worlds.data.WorldData
import io.github.vrcmteam.vrcm.presentation.extensions.ignoredFormat
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.presentation.supports.AppIcons
import io.github.vrcmteam.vrcm.presentation.theme.GameColor
import io.github.vrcmteam.vrcm.service.platformPackages

/**
 * 用户列表渲染
 */
fun LazyListScope.renderUserItems(
    users: List<IUser>,
    columns: Int = 1,
    onUserClick: (IUser) -> Unit
) {
    items(
        items = users.chunked(columns.coerceAtLeast(1)),
        key = { row -> row.joinToString(separator = "|") { it.id } },
    ) { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            row.forEach { user ->
                CompactFriendCard(user, Modifier.weight(1f), onUserClick)
            }
            repeat(columns.coerceAtLeast(1) - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactFriendCard(
    user: IUser,
    modifier: Modifier,
    onClick: (IUser) -> Unit,
) {
    OutlinedCard(
        modifier = modifier.clickable { onClick(user) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserStateIcon(
                modifier = Modifier.size(64.dp),
                iconUrl = user.iconUrl,
                userStatus = user.status,
                location = user.location,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppIcons.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = GameColor.Rank.fromValue(user.trustRank),
                    )
                    Text(
                        text = user.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (user.isSupporter) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.labelMedium,
                            color = GameColor.Supporter,
                        )
                    }
                }
                UserStatusRow(
                    user = user,
                    iconSize = 8.dp,
                    spacedBy = 6.dp,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * 单个用户项渲染
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LazyItemScope.renderUserItem(
    user: IUser,
    onUserClick: (IUser) -> Unit
) {
    SearchResultItem(
        item = user,
        onClick = onUserClick,
        modifier = Modifier.animateItem(),
        leadingContent = {
            UserStateIcon(
                modifier = Modifier.sharedBoundsBy("${user.id}UserIcon"),
                iconUrl = user.iconUrl,
            )
        },
        headlineContent = {
            UserInfoRow(
                iconSize = 16.dp,
                style = MaterialTheme.typography.titleMedium,
                user = user
            )
        },
        supportingContent = {
            UserStatusRow(
                iconSize = 8.dp,
                style = MaterialTheme.typography.bodyMedium,
                user = user,
            )
        },
        trailingContent = {
            // 离线用户显示最后活动时间
            val lastSeenAt = user.lastSeenAt()
            if (user.status != UserStatus.Offline || lastSeenAt == null) return@SearchResultItem
            Text(
                text = lastSeenAt.toLocalDateTime()?.ignoredFormat.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    )
}

/**
 * 判断世界是否被隐藏（API对不可见世界返回id为"???"）
 */
fun WorldData.isHiddenWorld(): Boolean = id == "???"

/**
 * 获取世界的安全图片URL，空字符串视为无图片
 */
fun WorldData.safeImageUrl(): String? = imageUrl.ifBlank { null }

/**
 * 获取隐藏世界的显示名称（使用favoriteId替代"???"）
 */
fun WorldData.hiddenWorldDisplayName(): String = favoriteId ?: name

/**
 * 世界列表渲染
 */
fun LazyListScope.renderWorldItems(
    worlds: List<WorldData>,
    columns: Int = 2,
    onWorldClick: (WorldData) -> Unit,
) {
    items(
        items = worlds.chunked(columns.coerceAtLeast(1)),
        key = { row -> row.joinToString(separator = "|") { it.favoriteId ?: it.id } },
    ) { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { world ->
                WorldTile(
                    world = world,
                    modifier = Modifier.weight(1f),
                    onClick = onWorldClick,
                )
            }
            repeat(columns.coerceAtLeast(1) - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WorldTile(
    world: WorldData,
    modifier: Modifier,
    onClick: (WorldData) -> Unit,
) {
    Card(
        modifier = modifier.clickable { onClick(world) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                if (world.isHiddenWorld()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.35f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else {
                    AImage(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.35f)
                            .clip(MaterialTheme.shapes.medium),
                        imageData = world.thumbnailImageUrl?.takeIf(String::isNotBlank) ?: world.safeImageUrl(),
                    )
                }
                PlatformBadgeRow(
                    platforms = remember(world.unityPackages) {
                        world.unityPackages.availableWorldPlatforms()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            }
            Text(
                text = if (world.isHiddenWorld()) world.hiddenWorldDisplayName() else world.name,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (world.isHiddenWorld()) strings.hiddenWorld else world.authorName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
/**
 * 单个世界项渲染
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LazyItemScope.renderWorldItem(
    world: WorldData,
    onWorldClick: (WorldData) -> Unit
) {
    SearchResultItem(
        item = world,
        onClick = onWorldClick,
        modifier = Modifier.animateItem(),
        leadingContent = {
            if (world.isHiddenWorld()) {
                Box(
                    modifier = Modifier.sharedBoundsBy("${world.id}WorldImage").size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                AImage(
                    modifier = Modifier.sharedBoundsBy("${world.id}WorldImage").size(48.dp)
                        .clip(MaterialTheme.shapes.medium),
                    imageData = world.safeImageUrl(),
                )
            }
        },
        headlineContent = {
            Text(
                text = if (world.isHiddenWorld()) world.hiddenWorldDisplayName() else world.name,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = if (world.isHiddenWorld()) strings.hiddenWorld else world.authorName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        },
        trailingContent = {
            // 显示世界平台类型
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ){
               remember { world.unityPackages.platformPackages.keys.sortedBy { it.name } } .forEach {
                    val icon = when(it){
                        Android -> AppIcons.Android
                        Ios -> AppIcons.Apple
                        Windows -> AppIcons.Windows
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "PlatformIcon",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

        }
    )
}

/**
 * 模型列表渲染
 */
fun LazyListScope.renderAvatarItems(
    avatars: List<AvatarData>,
    columns: Int = 2,
    onAvatarClick: (AvatarData) -> Unit
) {
    items(
        items = avatars.chunked(columns.coerceAtLeast(1)),
        key = { row -> row.joinToString(separator = "|") { it.id } },
    ) { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { avatar ->
                AvatarTile(
                    avatar = avatar,
                    modifier = Modifier.weight(1f),
                    onClick = onAvatarClick,
                )
            }
            repeat(columns.coerceAtLeast(1) - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AvatarTile(
    avatar: AvatarData,
    modifier: Modifier,
    onClick: (AvatarData) -> Unit,
) {
    Card(
        modifier = modifier.clickable { onClick(avatar) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                if (avatar.releaseStatus == "hidden") {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else {
                    AImage(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium),
                        imageData = avatar.thumbnailImageUrl?.takeIf(String::isNotBlank)
                            ?: avatar.imageUrl.takeIf(String::isNotBlank),
                    )
                }
                PlatformBadgeRow(
                    platforms = remember(avatar.unityPackages) {
                        avatar.unityPackages.availableAvatarPlatforms()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            }
            Text(
                text = if (avatar.releaseStatus == "hidden") avatar.id else avatar.name,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (avatar.releaseStatus == "hidden") strings.hiddenModel else avatar.authorName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 单个模型项渲染
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LazyItemScope.renderAvatarItem(
    avatar: AvatarData,
    onAvatarClick: (AvatarData) -> Unit
) {
    SearchResultItem(
        item = avatar,
        onClick = onAvatarClick,
        modifier = Modifier.animateItem(),
        leadingContent = {
            if (avatar.releaseStatus == "hidden") {
                Box(
                    modifier = Modifier.sharedBoundsBy("${avatar.id}AvatarImage").size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                AImage(
                    modifier = Modifier.sharedBoundsBy("${avatar.id}AvatarImage").size(48.dp)
                        .clip(MaterialTheme.shapes.medium),
                    imageData = avatar.thumbnailImageUrl,
                )
            }
        },
        headlineContent = {
            Text(
                text = if (avatar.releaseStatus == "hidden") avatar.id else avatar.name,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = if (avatar.releaseStatus == "hidden") strings.hiddenModel else avatar.authorName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        },
        trailingContent = {
            // 显示模型平台类型
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                remember(avatar.unityPackages) {
                    avatar.unityPackages.mapNotNull { pkg ->
                        when (pkg.platform?.lowercase()) {
                            "android" -> Android
                            "ios" -> Ios
                            "standalonewindows", "windows" -> Windows
                            else -> null
                        }
                    }.distinct().sortedBy { it.name }
                }.forEach {
                    val icon = when (it) {
                        Android -> AppIcons.Android
                        Ios -> AppIcons.Apple
                        Windows -> AppIcons.Windows
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "PlatformIcon",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    )
}

/**
 * 群组列表渲染
 */
fun LazyListScope.renderGroupItems(
    groups: List<LimitedGroup>,
    onGroupClick: (LimitedGroup) -> Unit
) {
    items(groups, key = { it.id }) { group ->
        renderGroupItem(group, onGroupClick)
    }
}

/**
 * 单个群组项渲染
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LazyItemScope.renderGroupItem(
    group: LimitedGroup,
    onGroupClick: (LimitedGroup) -> Unit
) {
    SearchResultItem(
        item = group,
        onClick = onGroupClick,
        modifier = Modifier.animateItem(),
        leadingContent = {
            GroupIcon(
                iconUrl = group.iconUrl,
                modifier = Modifier.sharedBoundsBy("${group.id}GroupIcon"),
                size = 48.dp
            )
        },
        headlineContent = {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        trailingContent = {
            // 显示成员数量
            Text(
                text = "${group.memberCount}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    )
}

package io.github.vrcmteam.vrcm.presentation.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.vrcmteam.vrcm.presentation.extensions.simpleClickable
import io.github.vrcmteam.vrcm.presentation.screens.user.UserProfileScreen
import io.github.vrcmteam.vrcm.presentation.screens.user.data.UserProfileVo
import io.github.vrcmteam.vrcm.presentation.settings.locale.LocaleStrings
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.service.FriendActivityService
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEventType
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/**
 * One-account local timeline. This complements the per-friend analysis rather than replacing it.
 */
object FriendActivityOverviewScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val activityService = koinInject<FriendActivityService>()
        val events by activityService.activityLog.collectAsState()
        val selectedTypes = remember { mutableStateMapOf<FriendActivityEventType, Boolean>() }
        val visibleEvents = events.filter { event ->
            selectedTypes.filterValues { it }.keys.let { selected -> selected.isEmpty() || event.type in selected }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(strings.friendActivityLogTitle) },
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Text(
                    text = strings.friendActivityLogDescription,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FriendActivityEventType.entries.forEach { type ->
                        val selected = selectedTypes[type] == true
                        AssistChip(
                            onClick = { selectedTypes[type] = !selected },
                            label = { Text(type.activityLabel(strings)) },
                            leadingIcon = { EventDot(type = type, highlighted = selected) },
                        )
                    }
                }
                if (visibleEvents.isEmpty()) {
                    Text(
                        text = strings.friendActivityLogEmpty,
                        modifier = Modifier.padding(32.dp).align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visibleEvents, key = { "${it.userId}-${it.type}-${it.occurredAtMillis}" }) { event ->
                            ActivityEventCard(
                                event = event,
                                onOpenProfile = {
                                    navigator.push(
                                        UserProfileScreen(
                                            UserProfileVo(
                                                id = event.userId,
                                                displayName = event.displayName,
                                                profileImageUrl = "",
                                                iconUrl = "",
                                                isFriend = true,
                                            ),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityEventCard(event: FriendActivityEvent, onOpenProfile: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().simpleClickable(onClick = onOpenProfile),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ListItem(
            leadingContent = { EventDot(event.type, highlighted = true) },
            headlineContent = {
                Text(
                    text = event.displayName.ifBlank { event.userId },
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(event.type.activityLabel(strings))
                    event.diffLines.forEach { line ->
                        Text(
                            text = if (line.added) "+ ${line.text}" else "- ${line.text}",
                            color = if (line.added) DiffAddedColor else DiffRemovedColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    event.previousValue?.takeIf(String::isNotBlank)?.let { Text("- $it", color = DiffRemovedColor) }
                    event.currentValue?.takeIf(String::isNotBlank)?.let { Text("+ $it", color = DiffAddedColor) }
                    Text(
                        event.occurredAtMillis.formatActivityTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

@Composable
private fun EventDot(type: FriendActivityEventType, highlighted: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = type.eventColor().copy(alpha = if (highlighted) 1f else 0.42f),
        modifier = Modifier.heightIn(min = 10.dp).padding(0.dp),
    ) { Text(" ", modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp)) }
}

private fun FriendActivityEventType.activityLabel(locale: LocaleStrings): String = when (this) {
    FriendActivityEventType.Online -> locale.friendActivityEventOnline
    FriendActivityEventType.Offline -> locale.friendActivityEventOffline
    FriendActivityEventType.Met -> locale.friendActivityEventMet
    FriendActivityEventType.Left -> locale.friendActivityEventLeft
    FriendActivityEventType.LocationChanged -> locale.friendActivityEventLocationChanged
    FriendActivityEventType.StatusChanged -> locale.friendActivityEventStatusChanged
    FriendActivityEventType.ProfileChanged -> locale.friendActivityEventProfileChanged
    FriendActivityEventType.AvatarChanged -> locale.friendActivityEventAvatarChanged
    FriendActivityEventType.FriendshipChanged -> locale.friendActivityEventFriendshipChanged
}

private val DiffAddedColor = Color(0xFF2EAD63)
private val DiffRemovedColor = Color(0xFFD54D4D)

private fun FriendActivityEventType.eventColor(): Color = when (this) {
    FriendActivityEventType.Online, FriendActivityEventType.Met -> Color(0xFF2EAD63)
    FriendActivityEventType.Offline, FriendActivityEventType.Left -> Color(0xFF6E7B8B)
    FriendActivityEventType.LocationChanged -> Color(0xFF3C9DE8)
    FriendActivityEventType.StatusChanged -> Color(0xFFFF9D20)
    FriendActivityEventType.ProfileChanged -> Color(0xFF8D65D7)
    FriendActivityEventType.AvatarChanged -> Color(0xFFDA6B9C)
    FriendActivityEventType.FriendshipChanged -> Color(0xFF4CA6A8)
}

@OptIn(ExperimentalTime::class)
private fun Long.formatActivityTime(): String = Instant.fromEpochMilliseconds(this)
    .toLocalDateTime(TimeZone.currentSystemDefault())
    .toString()
    .replace('T', ' ')

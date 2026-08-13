package io.github.vrcmteam.vrcm.presentation.screens.home.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import io.github.vrcmteam.vrcm.AppPlatform
import io.github.vrcmteam.vrcm.BackgroundFriendMonitoringResult
import io.github.vrcmteam.vrcm.core.extensions.bytesToMb
import io.github.vrcmteam.vrcm.core.shared.AppConst
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.presentation.compoments.ABottomSheet
import io.github.vrcmteam.vrcm.presentation.compoments.ToastText
import io.github.vrcmteam.vrcm.presentation.extensions.onApiFailure
import io.github.vrcmteam.vrcm.presentation.extensions.ignoredFormat
import io.github.vrcmteam.vrcm.presentation.extensions.openUrl
import io.github.vrcmteam.vrcm.presentation.screens.gallery.editor.readBoundedBytes
import io.github.vrcmteam.vrcm.presentation.settings.LocalSettingsState
import io.github.vrcmteam.vrcm.presentation.settings.locale.LanguageTag
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.presentation.settings.theme.ThemeColor
import io.github.vrcmteam.vrcm.presentation.supports.WebIcons
import io.github.vrcmteam.vrcm.presentation.supports.AppIcons
import io.github.vrcmteam.vrcm.service.AuthService
import io.github.vrcmteam.vrcm.service.FriendActivityService
import io.github.vrcmteam.vrcm.service.LanBridgePairing
import io.github.vrcmteam.vrcm.service.LanBridgeCandidate
import io.github.vrcmteam.vrcm.service.discoverLanBridges
import io.github.vrcmteam.vrcm.service.LanBridgeQrScanButton
import io.github.vrcmteam.vrcm.service.LanActivitySyncPreview
import io.github.vrcmteam.vrcm.service.LanActivitySyncService
import io.github.vrcmteam.vrcm.service.VersionService
import io.github.vrcmteam.vrcm.service.VrcxActivityImportPreview
import io.github.vrcmteam.vrcm.network.api.worlds.WorldsApi
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEventType
import io.github.vrcmteam.vrcm.storage.AccountCacheManager
import io.github.vrcmteam.vrcm.storage.SettingsDao
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import presentation.compoments.UpdateDialog
import presentation.screens.auth.data.VersionVo
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import io.github.vrcmteam.vrcm.storage.data.LanSyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
) {
    var destination by remember { mutableStateOf<SettingsDestination?>(null) }
    ABottomSheet(
        isVisible = isVisible,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (destination == null) {
                SettingsOverview(onOpen = { destination = it })
                LogoutButton(onDismissRequest)
            } else {
                SettingsDetail(
                    destination = requireNotNull(destination),
                    onBack = { destination = null },
                )
            }
        }
    }
}

private enum class SettingsDestination {
    Appearance,
    Notifications,
    BackgroundAndSync,
    ActivityData,
    About,
}

@Composable
private fun SettingsOverview(
    onOpen: (SettingsDestination) -> Unit,
) {
    val currentSettings by LocalSettingsState.current
    val localeStrings = strings
    SettingsBlockSurface {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsSectionTitle(localeStrings.settingsOverviewTitle)
            SettingsNavigationRow(
                title = localeStrings.settingsCategoryAppearance,
                summary = listOf(currentSettings.languageTag.displayName, currentSettings.themeColor.name)
                    .filter(String::isNotBlank).joinToString(" · "),
                icon = AppIcons.Settings,
                onClick = { onOpen(SettingsDestination.Appearance) },
            )
            SettingsDivider()
            SettingsNavigationRow(
                title = localeStrings.settingsCategoryNotifications,
                summary = if (currentSettings.isSystemNotificationsEnabled) localeStrings.settingsEnabled else localeStrings.settingsDisabled,
                icon = AppIcons.Notifications,
                onClick = { onOpen(SettingsDestination.Notifications) },
            )
            SettingsDivider()
            SettingsNavigationRow(
                title = localeStrings.settingsCategoryBackground,
                summary = if (currentSettings.isLanSyncAutoEnabled) localeStrings.settingsEnabled else localeStrings.settingsDisabled,
                icon = AppIcons.Computer,
                onClick = { onOpen(SettingsDestination.BackgroundAndSync) },
            )
            SettingsDivider()
            SettingsNavigationRow(
                title = localeStrings.settingsCategoryData,
                summary = currentSettings.activityLogRetentionDays?.let { "$it ${localeStrings.friendActivityLogRetentionDaysSuffix}" }
                    ?: localeStrings.friendActivityLogKeepForever,
                icon = AppIcons.DateRange,
                onClick = { onOpen(SettingsDestination.ActivityData) },
            )
            SettingsDivider()
            SettingsNavigationRow(
                title = localeStrings.settingsCategoryAbout,
                summary = AppConst.APP_VERSION,
                icon = AppIcons.QuestionMark,
                onClick = { onOpen(SettingsDestination.About) },
            )
        }
    }
}

@Composable
private fun SettingsDetail(
    destination: SettingsDestination,
    onBack: () -> Unit,
) {
    val localeStrings = strings
    TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text("‹ ${localeStrings.settingsBack}")
    }
    SettingsBlockSurface {
        when (destination) {
            SettingsDestination.Appearance -> AppearanceBlock()
            SettingsDestination.Notifications -> UnifiedSystemNotificationsBlock()
            SettingsDestination.BackgroundAndSync -> Column {
                VrcxLanSyncBlock()
                SettingsDivider()
                VrcxActivityImportBlock()
            }
            SettingsDestination.ActivityData -> FriendActivityLogBlock()
            SettingsDestination.About -> AboutBlock()
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendActivityLogBlock() {
    var currentSettings by LocalSettingsState.current
    val activityService = koinInject<FriendActivityService>()
    val events by activityService.activityLog.collectAsState()
    val localeStrings = strings
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsSectionTitle(localeStrings.friendActivityLogTitle)
        Text(
            localeStrings.friendActivityLogDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsItem(localeStrings.friendActivityLogRetention) {
            listOf<Int?>(null, 30, 90, 180, 365).forEach { days ->
                SettingsChoiceButton(
                    selected = currentSettings.activityLogRetentionDays == days,
                    onClick = {
                        currentSettings = currentSettings.copy(activityLogRetentionDays = days)
                        activityService.setActivityLogRetentionDays(days)
                    },
                ) {
                    Text(days?.let { "$it ${localeStrings.friendActivityLogRetentionDaysSuffix}" } ?: localeStrings.friendActivityLogKeepForever)
                }
            }
        }
        TextButton(onClick = { open = true }) { Text(localeStrings.friendActivityLogOpen) }
        TextButton(onClick = activityService::clearActivityLog) { Text(localeStrings.friendActivityLogClear) }
        Text(
            localeStrings.friendActivityLogClearDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (open) {
        FriendActivityLogSheet(events = events, onDismissRequest = { open = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
private fun FriendActivityLogSheet(
    events: List<FriendActivityEvent>,
    onDismissRequest: () -> Unit,
) {
    val localeStrings = strings
    val worldsApi = koinInject<WorldsApi>()
    val selectedTypes = remember { mutableStateListOf(*FriendActivityEventType.entries.toTypedArray()) }
    val resolvedWorldNames = remember { mutableStateMapOf<String, String>() }
    val resolvingWorldIds = remember { mutableStateSetOf<String>() }
    val filtered = events.filter { it.type in selectedTypes }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(localeStrings.friendActivityLogTitle, style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FriendActivityEventType.entries.forEach { type ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = {
                            if (type in selectedTypes) selectedTypes.remove(type) else selectedTypes.add(type)
                        },
                        label = { Text(type.activityLabel(localeStrings)) },
                    )
                }
            }
            if (filtered.isEmpty()) {
                Text(
                    localeStrings.friendActivityLogEmpty,
                    modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { "${it.userId}-${it.type}-${it.occurredAtMillis}" }) { event ->
                        ListItem(
                            headlineContent = {
                                Text("${event.displayName.ifBlank { event.userId }} ${event.type.activityLabel(localeStrings)}")
                            },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(Instant.fromEpochMilliseconds(event.occurredAtMillis).toLocalDateTime(TimeZone.currentSystemDefault()).ignoredFormat)
                                    event.diffLines.forEach { line ->
                                        Text(
                                            text = if (line.added) "+ ${line.text}" else "- ${line.text}",
                                            color = if (line.added) DiffAddedGreen else DiffRemovedRed,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    event.previousValue?.takeIf { it.isNotBlank() }?.let { value ->
                                        ActivityLogValue(
                                            prefix = "- ",
                                            value = value,
                                            color = DiffRemovedRed,
                                            style = MaterialTheme.typography.bodySmall,
                                            worldsApi = worldsApi,
                                            resolvedWorldNames = resolvedWorldNames,
                                            resolvingWorldIds = resolvingWorldIds,
                                        )
                                    }
                                    event.currentValue?.takeIf { it.isNotBlank() }?.let { value ->
                                        ActivityLogValue(
                                            prefix = "+ ",
                                            value = value,
                                            color = DiffAddedGreen,
                                            style = MaterialTheme.typography.bodySmall,
                                            worldsApi = worldsApi,
                                            resolvedWorldNames = resolvedWorldNames,
                                            resolvingWorldIds = resolvingWorldIds,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun FriendActivityEventType.activityLabel(localeStrings: io.github.vrcmteam.vrcm.presentation.settings.locale.LocaleStrings): String =
    when (this) {
        FriendActivityEventType.Online -> localeStrings.friendActivityEventOnline
        FriendActivityEventType.Offline -> localeStrings.friendActivityEventOffline
        FriendActivityEventType.Met -> localeStrings.friendActivityEventMet
        FriendActivityEventType.Left -> localeStrings.friendActivityEventLeft
        FriendActivityEventType.LocationChanged -> localeStrings.friendActivityEventLocationChanged
        FriendActivityEventType.StatusChanged -> localeStrings.friendActivityEventStatusChanged
        FriendActivityEventType.ProfileChanged -> localeStrings.friendActivityEventProfileChanged
        FriendActivityEventType.AvatarChanged -> localeStrings.friendActivityEventAvatarChanged
        FriendActivityEventType.FriendshipChanged -> localeStrings.friendActivityEventFriendshipChanged
    }

/** Resolves only visible log rows and leaves the original location visible if the lookup fails. */
@Composable
private fun ActivityLogValue(
    prefix: String,
    value: String,
    color: Color,
    style: TextStyle,
    worldsApi: WorldsApi,
    resolvedWorldNames: MutableMap<String, String>,
    resolvingWorldIds: MutableSet<String>,
) {
    val worldId = value.worldIdOrNull()
    LaunchedEffect(worldId) {
        if (worldId == null || worldId in resolvedWorldNames || !resolvingWorldIds.add(worldId)) return@LaunchedEffect
        runCatching { worldsApi.getWorldById(worldId).name }
            .onSuccess { resolvedWorldNames[worldId] = it }
        resolvingWorldIds.remove(worldId)
    }
    val display = worldId?.let { id ->
        resolvedWorldNames[id]?.let { name -> value.replaceFirst(id, name) }
    } ?: value
    Text(text = prefix + display, color = color, style = style)
}

private fun String.worldIdOrNull(): String? =
    Regex("wrld_[A-Za-z0-9-]+").find(this)?.value

private val DiffAddedGreen = Color(0xFF43A047)
private val DiffRemovedRed = Color(0xFFE53935)

@OptIn(ExperimentalTime::class)
@Composable
private fun VrcxLanSyncBlock() {
    var currentSettings by LocalSettingsState.current
    val lanSyncService = koinInject<LanActivitySyncService>()
    val settingsDao = koinInject<SettingsDao>()
    val scope = rememberCoroutineScope()
    val localeStrings = strings
    var bridgeUrl by remember { mutableStateOf(settingsDao.lanBridgeUrl.orEmpty()) }
    var bridgeToken by remember { mutableStateOf(settingsDao.lanBridgeToken.orEmpty()) }
    var isSyncing by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }
    var discoveredBridges by remember { mutableStateOf<List<LanBridgeCandidate>>(emptyList()) }
    var syncStatus by remember { mutableStateOf(settingsDao.lanSyncStatus) }
    var lanPreview by remember { mutableStateOf<LanActivitySyncPreview?>(null) }
    val isPaired = bridgeUrl.isNotBlank() && bridgeToken.isNotBlank()

    fun syncError(cause: Throwable): String = buildString {
        append(localeStrings.vrcxLanSyncFailed)
        cause.message?.trim()?.takeIf { it.isNotEmpty() }?.let {
            append("：")
            append(it.take(120))
        }
    }

    suspend fun pairFromQrAndPreview(rawUrl: String) {
        isSyncing = true
        runCatching {
            val pairing = LanBridgePairing.fromInput(rawUrl, fallbackToken = "")
            bridgeUrl = pairing.baseUrl
            bridgeToken = pairing.token
            settingsDao.lanBridgeUrl = pairing.baseUrl
            settingsDao.lanBridgeToken = pairing.token
            lanSyncService.prepare(pairing)
        }.onSuccess { imported ->
            lanPreview = imported
            syncStatus = syncStatus.copy(lastError = null)
            settingsDao.lanSyncStatus = syncStatus
        }.onFailure { cause ->
            val error = syncError(cause)
            syncStatus = syncStatus.copy(lastError = error)
            settingsDao.lanSyncStatus = syncStatus
            SharedFlowCentre.toastText.emit(ToastText.Error(error))
        }
        isSyncing = false
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsSectionTitle(localeStrings.vrcxLanSyncTitle)
        Text(
            syncStatus.lastSuccessAtMillis?.let {
                localeStrings.vrcxLanSyncLastSuccess.format(
                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).ignoredFormat,
                    syncStatus.lastDirection.lanSyncDirectionLabel(localeStrings),
                )
            } ?: localeStrings.vrcxLanSyncNever,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        syncStatus.lastError?.let { error ->
            Text(
                localeStrings.vrcxLanSyncLastError.format(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                currentSettings = currentSettings.copy(
                    isLanSyncAutoEnabled = !currentSettings.isLanSyncAutoEnabled,
                )
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                localeStrings.vrcxLanAutoSync,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = currentSettings.isLanSyncAutoEnabled,
                onCheckedChange = { enabled ->
                    currentSettings = currentSettings.copy(isLanSyncAutoEnabled = enabled)
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = !isDiscovering,
                onClick = {
                    scope.launch {
                        isDiscovering = true
                        val found = discoverLanBridges()
                        discoveredBridges = found
                        isDiscovering = false
                        found.singleOrNull()?.pairingUrl?.let { pairFromQrAndPreview(it) }
                    }
                },
            ) {
                Text(if (isDiscovering) localeStrings.vrcxLanDiscovering else localeStrings.vrcxLanDiscover)
            }
            LanBridgeQrScanButton(
                modifier = Modifier.weight(1f),
                label = localeStrings.vrcxLanScanQr,
                enabled = !isSyncing,
                onScanned = { scannedUrl ->
                    scope.launch { pairFromQrAndPreview(scannedUrl) }
                },
            )
        }
        discoveredBridges.forEach { candidate ->
            if (candidate.pairingUrl != null) {
                TextButton(
                    enabled = !isSyncing,
                    onClick = { scope.launch { pairFromQrAndPreview(candidate.pairingUrl) } },
                ) { Text(localeStrings.vrcxLanDiscoveredAddress.format(candidate.baseUrl)) }
            } else {
                Text(
                    localeStrings.vrcxLanDiscoveredAddress.format(candidate.baseUrl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!isDiscovering && discoveredBridges.isEmpty()) {
            Text(
                localeStrings.vrcxLanDiscoveryHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isPaired) {
            Text(
                localeStrings.vrcxLanPaired.format(bridgeUrl),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = !isSyncing && isPaired,
                onClick = {
                    scope.launch {
                        isSyncing = true
                        runCatching {
                            val pairing = LanBridgePairing.fromInput(bridgeUrl, bridgeToken)
                            settingsDao.lanBridgeUrl = pairing.baseUrl
                            settingsDao.lanBridgeToken = pairing.token
                            lanSyncService.prepare(pairing)
                        }.onSuccess { lanPreview = it }
                            .onFailure { cause ->
                                val error = syncError(cause)
                                syncStatus = syncStatus.copy(lastError = error)
                                settingsDao.lanSyncStatus = syncStatus
                                SharedFlowCentre.toastText.emit(ToastText.Error(error))
                            }
                        isSyncing = false
                    }
                },
            ) {
                Text(if (isSyncing) localeStrings.vrcxLanSyncing else localeStrings.vrcxLanSyncNow)
            }
        }
    }

    lanPreview?.let { importPreview ->
        AlertDialog(
            onDismissRequest = { lanPreview = null },
            title = { Text(localeStrings.vrcxLanSyncConfirmTitle) },
            text = {
                Text(
                    localeStrings.vrcxLanSyncConfirmMessage.formatCountPlaceholders(
                        importPreview.archive.acceptedEvents,
                        importPreview.archive.alreadyKnownEvents,
                        importPreview.vrcx.newEvents,
                        importPreview.vrcx.alreadyImportedEvents,
                        importPreview.vrcx.presenceEvents,
                        importPreview.vrcx.completedMeetings,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        isSyncing = true
                        runCatching {
                            val pairing = LanBridgePairing.fromInput(bridgeUrl, bridgeToken)
                            lanSyncService.apply(pairing, importPreview)
                        }.onSuccess {
                            syncStatus = LanSyncStatus(
                                lastSuccessAtMillis = Clock.System.now().toEpochMilliseconds(),
                                lastDirection = LAN_SYNC_DIRECTION_AUTOMATIC,
                            )
                            settingsDao.lanSyncStatus = syncStatus
                            lanPreview = null
                            SharedFlowCentre.toastText.emit(ToastText.Info(localeStrings.vrcxLanSyncSuccess))
                        }.onFailure { cause ->
                            val error = syncError(cause)
                            syncStatus = syncStatus.copy(lastError = error)
                            settingsDao.lanSyncStatus = syncStatus
                            SharedFlowCentre.toastText.emit(ToastText.Error(error))
                        }
                        isSyncing = false
                    }
                }) { Text(localeStrings.vrcxLanSyncNow) }
            },
            dismissButton = {
                TextButton(onClick = { lanPreview = null }) { Text(localeStrings.backgroundFriendMonitoringCancel) }
            },
        )
    }
}

@Composable
private fun VrcxActivityImportBlock() {
    val activityService = koinInject<FriendActivityService>()
    val scope = rememberCoroutineScope()
    val localeStrings = strings
    var preview by remember { mutableStateOf<VrcxActivityImportPreview?>(null) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File("json")) { file ->
        if (file != null) {
            scope.launch {
                runCatching {
                    activityService.previewVrcxActivityImport(
                        file.readBoundedBytes(MAX_VRCX_ACTIVITY_IMPORT_BYTES).decodeToString(),
                    )
                }.onSuccess { preview = it }
                    .onFailure { SharedFlowCentre.toastText.emit(ToastText.Error(localeStrings.vrcxActivityImportInvalid)) }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsSectionTitle(localeStrings.vrcxActivityImportTitle)
        TextButton(onClick = picker::launch) { Text(localeStrings.vrcxActivityImportChoose) }
    }

    preview?.let { importPreview ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(localeStrings.vrcxActivityImportConfirmTitle) },
            text = {
                Text(
                    localeStrings.vrcxActivityImportConfirmMessage.formatCountPlaceholders(
                        importPreview.presenceEvents,
                        importPreview.completedMeetings,
                        importPreview.involvedFriends,
                        importPreview.alreadyImportedEvents,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    activityService.applyVrcxActivityImport(importPreview)
                    preview = null
                    scope.launch { SharedFlowCentre.toastText.emit(ToastText.Info(localeStrings.vrcxActivityImportSuccess)) }
                }) { Text(localeStrings.vrcxActivityImportConfirmTitle) }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }) { Text(localeStrings.backgroundFriendMonitoringCancel) }
            },
        )
    }
}

private fun String.formatCountPlaceholders(vararg values: Int): String =
    values.fold(this) { text, value -> text.replaceFirst("%d", value.toString()) }

private const val MAX_VRCX_ACTIVITY_IMPORT_BYTES = 16L * 1024L * 1024L
private const val LAN_SYNC_DIRECTION_AUTOMATIC = "automatic"

private fun String?.lanSyncDirectionLabel(localeStrings: io.github.vrcmteam.vrcm.presentation.settings.locale.LocaleStrings): String =
    when (this) {
        LAN_SYNC_DIRECTION_AUTOMATIC -> localeStrings.vrcxLanSyncDirectionAutomatic
        else -> orEmpty()
    }


@Composable
private fun AppearanceBlock() {
    var currentSettings by LocalSettingsState.current
    val themeColors: List<ThemeColor> = with(currentKoinScope()) {
        remember { getAll<ThemeColor>().filter { it.name != ThemeColor.Default.name } }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsSectionTitle(strings.settingsCategoryAppearance)
        SettingsItem(strings.stettingLanguage) {
            LanguageTag.entries.forEach { language ->
                SettingsChoiceButton(
                    selected = language.tag == currentSettings.languageTag.tag,
                    onClick = { currentSettings = currentSettings.copy(languageTag = language) },
                ) {
                    Text(language.displayName, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        SettingsDivider()
        SettingsItem(strings.stettingThemeMode) {
            listOf(null, true, false).forEach { isDark ->
                SettingsChoiceButton(
                    selected = currentSettings.isDarkTheme == isDark,
                    onClick = { currentSettings = currentSettings.copy(isDarkTheme = isDark) },
                ) {
                    Text(
                        text = when (isDark) {
                            null -> strings.stettingSystemThemeMode
                            true -> strings.stettingDarkThemeMode
                            false -> strings.stettingLightThemeMode
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        SettingsDivider()
        SettingsItem(strings.stettingThemeColor) {
            themeColors.forEach { theme ->
                SettingsChoiceButton(
                    selected = theme.name == currentSettings.themeColor.name,
                    onClick = { currentSettings = currentSettings.copy(themeColor = theme) },
                    containerColor = theme.colorScheme.primaryContainer,
                    contentColor = theme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = CircleShape,
                            color = theme.colorScheme.primary,
                        ) {}
                        Text(theme.name, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedSystemNotificationsBlock() {
    var currentSettings by LocalSettingsState.current
    val platform = koinInject<AppPlatform>()
    val scope = rememberCoroutineScope()
    val localeStrings = strings
    var showBackgroundMonitoringDialog by remember { mutableStateOf(false) }
    var batteryOptimizationAllowed by remember {
        mutableStateOf(platform.isIgnoringBatteryOptimizations())
    }

    val showInfo: (String) -> Unit = { message ->
        scope.launch { SharedFlowCentre.toastText.emit(ToastText.Info(message)) }
    }
    val changeBackgroundMonitoring: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (!platform.supportsBackgroundFriendMonitoring) {
                showInfo(localeStrings.backgroundFriendMonitoringUnsupported)
            } else {
                showBackgroundMonitoringDialog = true
            }
        } else {
            platform.setBackgroundFriendMonitoringEnabled(false)
            currentSettings = currentSettings.copy(
                isSystemNotificationsEnabled = false,
                isBackgroundFriendMonitoringEnabled = false,
            )
            showInfo(localeStrings.backgroundFriendMonitoringStopped)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsSectionTitle(localeStrings.systemNotificationsTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    changeBackgroundMonitoring(!currentSettings.isSystemNotificationsEnabled)
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (platform.supportsBackgroundFriendMonitoring) {
                        localeStrings.systemNotificationsDescription
                    } else {
                        localeStrings.backgroundFriendMonitoringUnsupported
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = currentSettings.isSystemNotificationsEnabled,
                onCheckedChange = changeBackgroundMonitoring,
                enabled = platform.supportsBackgroundFriendMonitoring,
            )
        }
        if (platform.supportsBatteryOptimizationSettings &&
            currentSettings.isSystemNotificationsEnabled
        ) {
            SettingsDivider()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = localeStrings.backgroundFriendMonitoringBatteryTitle,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = if (batteryOptimizationAllowed) {
                                localeStrings.backgroundFriendMonitoringBatteryAllowed
                            } else {
                                localeStrings.backgroundFriendMonitoringBatteryRestricted
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (batteryOptimizationAllowed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    TextButton(
                        onClick = {
                            platform.openBatteryOptimizationSettings()
                            batteryOptimizationAllowed = platform.isIgnoringBatteryOptimizations()
                        },
                    ) {
                        Text(localeStrings.backgroundFriendMonitoringBatteryAction)
                    }
                }
                Text(
                    text = localeStrings.backgroundFriendMonitoringBatteryHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showBackgroundMonitoringDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundMonitoringDialog = false },
            title = { Text(localeStrings.backgroundFriendMonitoringDialogTitle) },
            text = { Text(localeStrings.backgroundFriendMonitoringDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundMonitoringDialog = false
                        if (!platform.hasBackgroundFriendMonitoringPermission()) {
                            platform.requestBackgroundFriendMonitoringPermission()
                            showInfo(localeStrings.backgroundFriendMonitoringPermissionRequired)
                            return@TextButton
                        }
                        when (platform.setBackgroundFriendMonitoringEnabled(true)) {
                            BackgroundFriendMonitoringResult.Started -> {
                                currentSettings = currentSettings.copy(
                                    isBackgroundFriendMonitoringEnabled = true,
                                    isSystemNotificationsEnabled = true,
                                )
                                showInfo(localeStrings.backgroundFriendMonitoringStarted)
                            }
                            BackgroundFriendMonitoringResult.PermissionRequired -> {
                                platform.requestBackgroundFriendMonitoringPermission()
                                showInfo(localeStrings.backgroundFriendMonitoringPermissionRequired)
                            }
                            BackgroundFriendMonitoringResult.Unsupported -> {
                                showInfo(localeStrings.backgroundFriendMonitoringUnsupported)
                            }
                            BackgroundFriendMonitoringResult.Stopped -> Unit
                        }
                    },
                ) {
                    Text(localeStrings.backgroundFriendMonitoringConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundMonitoringDialog = false }) {
                    Text(localeStrings.backgroundFriendMonitoringCancel)
                }
            },
        )
    }
}

@Composable
private fun AboutBlock() {
    val versionService = koinInject<VersionService>()
    val imageLoader = koinInject<ImageLoader>()
    val accountCacheManager = koinInject<AccountCacheManager>()
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(VersionVo()) }
    var isLatestVersion by remember { mutableStateOf(false) }
    var isLoadingVersion by remember { mutableStateOf(false) }
    val platform = koinInject<AppPlatform>()
    val checkVersion = {
        scope.launch {
            if (isLoadingVersion) return@launch
            isLoadingVersion = true
            versionService.checkVersion(false).onSuccess {
                isLatestVersion = it.hasNewVersion.not()
                version = VersionVo(
                    it.tagName,
                    it.htmlUrl,
                    it.body,
                    it.hasNewVersion,
                    it.downloadUrl,
                )
            }.onApiFailure("Setting") {
                SharedFlowCentre.toastText.emit(ToastText.Error(it))
            }
            isLoadingVersion = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        SettingsSectionTitle(strings.stettingAbout)
        val diskCache = imageLoader.diskCache
        var size by remember(diskCache) { mutableStateOf(diskCache?.size ?: 0L) }
        SettingsActionRow(
            title = strings.stettingClearCache,
            value = diskCache?.let { "${size.bytesToMb()}/${it.maxSize.bytesToMb()}MB" },
            onClick = {
                diskCache?.clear()
                accountCacheManager.clearAll()
                size = 0
            },
        )
        SettingsDivider()
        SettingsActionRow(
            title = strings.stettingVersion,
            value = AppConst.APP_VERSION,
            onClick = { checkVersion(); Unit },
            trailing = {
                AnimatedVisibility(isLatestVersion) {
                    Text(
                        text = strings.stettingAlreadyLatest,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                AnimatedVisibility(isLoadingVersion) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                }
            },
        )
        SettingsDivider()
        SettingsActionRow(
            title = strings.stettingAbout,
            value = "GitHub",
            onClick = { platform.openUrl(AppConst.APP_GITHUB_URL) },
            trailing = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = WebIcons.GithubIcon,
                    contentDescription = "GitHub",
                )
            },
        )
        if (!isLatestVersion) {
            UpdateDialog(
                version = version,
                onDismissRequest = { version = VersionVo() },
            )
        }
    }
}

@Composable
private fun LogoutButton(onDismissRequest: () -> Unit) {
    val authService = koinInject<AuthService>()
    val platform = koinInject<AppPlatform>()
    var currentSettings by LocalSettingsState.current
    val logoutCall = {
        platform.setBackgroundFriendMonitoringEnabled(false)
        currentSettings = currentSettings.copy(
            isBackgroundFriendMonitoringEnabled = false,
            isSystemNotificationsEnabled = false,
        )
        onDismissRequest()
        authService.logout()
    }
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = logoutCall,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(
            text = strings.stettingLogout,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        thickness = 0.5.dp,
    )
}

@Composable
private fun SettingsChoiceButton(
    selected: Boolean,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        enabled = !selected,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else containerColor,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        trailing()
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsBlockSurface(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

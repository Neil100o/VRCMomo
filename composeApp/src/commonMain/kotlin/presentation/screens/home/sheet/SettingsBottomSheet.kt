package io.github.vrcmteam.vrcm.presentation.screens.home.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import io.github.vrcmteam.vrcm.presentation.extensions.openUrl
import io.github.vrcmteam.vrcm.presentation.screens.gallery.editor.readBoundedBytes
import io.github.vrcmteam.vrcm.presentation.settings.LocalSettingsState
import io.github.vrcmteam.vrcm.presentation.settings.locale.LanguageTag
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.presentation.settings.theme.ThemeColor
import io.github.vrcmteam.vrcm.presentation.supports.WebIcons
import io.github.vrcmteam.vrcm.service.AuthService
import io.github.vrcmteam.vrcm.service.FriendActivityService
import io.github.vrcmteam.vrcm.service.VersionService
import io.github.vrcmteam.vrcm.service.VrcxActivityImportPreview
import io.github.vrcmteam.vrcm.storage.AccountCacheManager
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import presentation.compoments.UpdateDialog
import presentation.screens.auth.data.VersionVo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
) {
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsBlockSurface {
                AppearanceBlock()
            }
            SettingsBlockSurface {
                BackgroundMonitoringBlock()
            }
            SettingsBlockSurface {
                VrcxActivityImportBlock()
            }
            SettingsBlockSurface {
                AboutBlock()
            }
            LogoutButton(onDismissRequest)
        }
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
        Text(
            localeStrings.vrcxActivityImportDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        SettingsSectionTitle(AppConst.APP_NAME)
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
private fun BackgroundMonitoringBlock() {
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
            currentSettings = currentSettings.copy(isBackgroundFriendMonitoringEnabled = false)
            showInfo(localeStrings.backgroundFriendMonitoringStopped)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsSectionTitle(localeStrings.backgroundFriendMonitoringTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    changeBackgroundMonitoring(!currentSettings.isBackgroundFriendMonitoringEnabled)
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
                        localeStrings.backgroundFriendMonitoringDescription
                    } else {
                        localeStrings.backgroundFriendMonitoringUnsupported
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = currentSettings.isBackgroundFriendMonitoringEnabled,
                onCheckedChange = changeBackgroundMonitoring,
                enabled = platform.supportsBackgroundFriendMonitoring,
            )
        }
        if (platform.supportsBatteryOptimizationSettings &&
            currentSettings.isBackgroundFriendMonitoringEnabled
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
        currentSettings = currentSettings.copy(isBackgroundFriendMonitoringEnabled = false)
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

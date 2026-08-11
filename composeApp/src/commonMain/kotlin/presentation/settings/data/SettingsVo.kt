package io.github.vrcmteam.vrcm.presentation.settings.data

import io.github.vrcmteam.vrcm.presentation.settings.locale.LanguageTag
import io.github.vrcmteam.vrcm.presentation.settings.theme.ThemeColor
import io.github.vrcmteam.vrcm.service.FriendPresenceNotificationSelection

data class SettingsVo(
    val isDarkTheme: Boolean?,
    val languageTag: LanguageTag,
    val themeColor: ThemeColor,
    val isBackgroundFriendMonitoringEnabled: Boolean = false,
    val isSystemNotificationsEnabled: Boolean = false,
    val friendPresenceNotificationSelection: FriendPresenceNotificationSelection = FriendPresenceNotificationSelection(),
    val activityLogRetentionDays: Int? = null,
    val isLanSyncAutoEnabled: Boolean = false,
)

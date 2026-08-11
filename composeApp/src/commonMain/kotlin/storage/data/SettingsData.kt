package io.github.vrcmteam.vrcm.storage.data

import io.github.vrcmteam.vrcm.service.FriendPresenceNotificationSelection
import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val isDarkTheme: Boolean?,
    val themeColor: String?,
    val languageTag: String?,
    val isBackgroundFriendMonitoringEnabled: Boolean = false,
    val isSystemNotificationsEnabled: Boolean = true,
    val friendPresenceNotificationSelection: FriendPresenceNotificationSelection = FriendPresenceNotificationSelection(),
    /** Null keeps all activity-log files indefinitely. */
    val activityLogRetentionDays: Int? = null,
    /** Optional low-frequency LAN bridge cycle while Android background monitoring is active. */
    val isLanSyncAutoEnabled: Boolean = false,
)

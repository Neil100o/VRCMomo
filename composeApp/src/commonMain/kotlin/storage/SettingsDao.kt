package io.github.vrcmteam.vrcm.storage

import com.russhwolf.settings.Settings
import io.github.vrcmteam.vrcm.storage.data.SettingsData
import io.github.vrcmteam.vrcm.storage.data.LanSyncStatus

class SettingsDao(
    private val settingsSettings: Settings
) {

    var settings: SettingsData
        get() {
            return SettingsData(
                isDarkTheme = settingsSettings.getBooleanOrNull(DaoKeys.Settings.IS_DARK_THEME_KEY),
                themeColor = settingsSettings.getStringOrNull(DaoKeys.Settings.THEME_COLOR_KEY),
                languageTag = settingsSettings.getStringOrNull(DaoKeys.Settings.LANGUAGE_TAG_KEY),
                isBackgroundFriendMonitoringEnabled = settingsSettings.getBoolean(
                    DaoKeys.Settings.BACKGROUND_FRIEND_MONITORING_ENABLED_KEY,
                    false,
                ),
                isSystemNotificationsEnabled = settingsSettings.getBoolean(
                    DaoKeys.Settings.SYSTEM_NOTIFICATIONS_ENABLED_KEY,
                    true,
                ),
                activityLogRetentionDays = settingsSettings.getIntOrNull(
                    DaoKeys.Settings.ACTIVITY_LOG_RETENTION_DAYS_KEY,
                ),
            )
        }
        set(value) {
            value.isDarkTheme?.let {
                settingsSettings.putBoolean(DaoKeys.Settings.IS_DARK_THEME_KEY, it)
            } ?: settingsSettings.remove(DaoKeys.Settings.IS_DARK_THEME_KEY)

            value.themeColor?.let {
                settingsSettings.putString(DaoKeys.Settings.THEME_COLOR_KEY, it)
            }

            value.languageTag?.let {
                settingsSettings.putString(DaoKeys.Settings.LANGUAGE_TAG_KEY, it)
            }

            settingsSettings.putBoolean(
                DaoKeys.Settings.BACKGROUND_FRIEND_MONITORING_ENABLED_KEY,
                value.isBackgroundFriendMonitoringEnabled,
            )
            settingsSettings.putBoolean(
                DaoKeys.Settings.SYSTEM_NOTIFICATIONS_ENABLED_KEY,
                value.isSystemNotificationsEnabled,
            )
            value.activityLogRetentionDays?.let {
                settingsSettings.putInt(DaoKeys.Settings.ACTIVITY_LOG_RETENTION_DAYS_KEY, it)
            } ?: settingsSettings.remove(DaoKeys.Settings.ACTIVITY_LOG_RETENTION_DAYS_KEY)
        }

    var lanBridgeUrl: String?
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.LAN_BRIDGE_URL_KEY)
        set(value) = value?.trim().takeUnless { it.isNullOrEmpty() }?.let {
            settingsSettings.putString(DaoKeys.Settings.LAN_BRIDGE_URL_KEY, it)
        } ?: settingsSettings.remove(DaoKeys.Settings.LAN_BRIDGE_URL_KEY)

    var lanBridgeToken: String?
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.LAN_BRIDGE_TOKEN_KEY)
        set(value) = value?.trim().takeUnless { it.isNullOrEmpty() }?.let {
            settingsSettings.putString(DaoKeys.Settings.LAN_BRIDGE_TOKEN_KEY, it)
        } ?: settingsSettings.remove(DaoKeys.Settings.LAN_BRIDGE_TOKEN_KEY)

    var lanSyncStatus: LanSyncStatus
        get() = LanSyncStatus(
            lastSuccessAtMillis = settingsSettings.getLongOrNull(DaoKeys.Settings.LAN_SYNC_LAST_SUCCESS_AT_KEY),
            lastDirection = settingsSettings.getStringOrNull(DaoKeys.Settings.LAN_SYNC_LAST_DIRECTION_KEY),
            lastError = settingsSettings.getStringOrNull(DaoKeys.Settings.LAN_SYNC_LAST_ERROR_KEY),
        )
        set(value) {
            value.lastSuccessAtMillis?.let {
                settingsSettings.putLong(DaoKeys.Settings.LAN_SYNC_LAST_SUCCESS_AT_KEY, it)
            } ?: settingsSettings.remove(DaoKeys.Settings.LAN_SYNC_LAST_SUCCESS_AT_KEY)
            value.lastDirection?.let {
                settingsSettings.putString(DaoKeys.Settings.LAN_SYNC_LAST_DIRECTION_KEY, it)
            } ?: settingsSettings.remove(DaoKeys.Settings.LAN_SYNC_LAST_DIRECTION_KEY)
            value.lastError?.let {
                settingsSettings.putString(DaoKeys.Settings.LAN_SYNC_LAST_ERROR_KEY, it)
            } ?: settingsSettings.remove(DaoKeys.Settings.LAN_SYNC_LAST_ERROR_KEY)
        }

    var rememberVersion: String?
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.REMEMBER_VERSION_KEY)
        set(value) = value.let {
            if (!it.isNullOrEmpty()) {
                settingsSettings.putString(DaoKeys.Settings.REMEMBER_VERSION_KEY, it)
            } else {
                settingsSettings.remove(DaoKeys.Settings.REMEMBER_VERSION_KEY)
            }
        }

}

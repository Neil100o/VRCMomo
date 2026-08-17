package io.github.vrcmteam.vrcm.storage

import com.russhwolf.settings.Settings
import io.github.vrcmteam.vrcm.storage.data.SettingsData
import io.github.vrcmteam.vrcm.storage.data.LanSyncStatus
import io.github.vrcmteam.vrcm.service.FriendPresenceNotificationSelection
import kotlinx.serialization.json.Json

private val settingsJson = Json { ignoreUnknownKeys = true }

class SettingsDao(
    private val settingsSettings: Settings
) {

    var settings: SettingsData
        get() {
            val unifiedNotificationsEnabled = migrateUnifiedAndroidNotifications()
            return SettingsData(
                isDarkTheme = settingsSettings.getBooleanOrNull(DaoKeys.Settings.IS_DARK_THEME_KEY),
                themeColor = settingsSettings.getStringOrNull(DaoKeys.Settings.THEME_COLOR_KEY),
                languageTag = settingsSettings.getStringOrNull(DaoKeys.Settings.LANGUAGE_TAG_KEY),
                isBackgroundFriendMonitoringEnabled = settingsSettings.getBoolean(
                    DaoKeys.Settings.BACKGROUND_FRIEND_MONITORING_ENABLED_KEY,
                    unifiedNotificationsEnabled,
                ),
                isSystemNotificationsEnabled = unifiedNotificationsEnabled,
                friendPresenceNotificationSelection = settingsSettings
                    .getStringOrNull(DaoKeys.Settings.FRIEND_PRESENCE_NOTIFICATION_SELECTION_KEY)
                    ?.let {
                        runCatching {
                            settingsJson.decodeFromString<FriendPresenceNotificationSelection>(it)
                        }.getOrNull()
                    }
                    ?: FriendPresenceNotificationSelection(),
                activityLogRetentionDays = settingsSettings.getIntOrNull(
                    DaoKeys.Settings.ACTIVITY_LOG_RETENTION_DAYS_KEY,
                ),
                isLanSyncAutoEnabled = settingsSettings.getBoolean(
                    DaoKeys.Settings.LAN_SYNC_AUTO_ENABLED_KEY,
                    false,
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
                value.isSystemNotificationsEnabled,
            )
            settingsSettings.putBoolean(
                DaoKeys.Settings.SYSTEM_NOTIFICATIONS_ENABLED_KEY,
                value.isSystemNotificationsEnabled,
            )
            settingsSettings.putBoolean(
                DaoKeys.Settings.UNIFIED_ANDROID_NOTIFICATIONS_MIGRATED_KEY,
                true,
            )
            settingsSettings.putString(
                DaoKeys.Settings.FRIEND_PRESENCE_NOTIFICATION_SELECTION_KEY,
                settingsJson.encodeToString(value.friendPresenceNotificationSelection),
            )
            value.activityLogRetentionDays?.let {
                settingsSettings.putInt(DaoKeys.Settings.ACTIVITY_LOG_RETENTION_DAYS_KEY, it)
            } ?: settingsSettings.remove(DaoKeys.Settings.ACTIVITY_LOG_RETENTION_DAYS_KEY)
            settingsSettings.putBoolean(
                DaoKeys.Settings.LAN_SYNC_AUTO_ENABLED_KEY,
                value.isLanSyncAutoEnabled,
            )
        }

    /**
     * Older builds exposed system alerts and the foreground monitor as separate switches. The
     * unified switch starts enabled only when both old permissions were explicitly active, so an
     * upgrade never starts a long-running service for a user who had background monitoring off.
     */
    private fun migrateUnifiedAndroidNotifications(): Boolean {
        if (settingsSettings.getBoolean(DaoKeys.Settings.UNIFIED_ANDROID_NOTIFICATIONS_MIGRATED_KEY, false)) {
            return settingsSettings.getBoolean(DaoKeys.Settings.SYSTEM_NOTIFICATIONS_ENABLED_KEY, false)
        }
        val enabled = settingsSettings.getBoolean(
            DaoKeys.Settings.SYSTEM_NOTIFICATIONS_ENABLED_KEY,
            true,
        ) && settingsSettings.getBoolean(
            DaoKeys.Settings.BACKGROUND_FRIEND_MONITORING_ENABLED_KEY,
            false,
        )
        settingsSettings.putBoolean(DaoKeys.Settings.SYSTEM_NOTIFICATIONS_ENABLED_KEY, enabled)
        settingsSettings.putBoolean(DaoKeys.Settings.BACKGROUND_FRIEND_MONITORING_ENABLED_KEY, enabled)
        settingsSettings.putBoolean(DaoKeys.Settings.UNIFIED_ANDROID_NOTIFICATIONS_MIGRATED_KEY, true)
        return enabled
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

    /** MomoCall uses its own relay address and never reuses VRChat API credentials. */
    var momoCallSignalingUrl: String?
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.MOMO_CALL_SIGNALING_URL_KEY)
        set(value) = value?.trim().takeUnless { it.isNullOrEmpty() }?.let {
            settingsSettings.putString(DaoKeys.Settings.MOMO_CALL_SIGNALING_URL_KEY, it)
        } ?: settingsSettings.remove(DaoKeys.Settings.MOMO_CALL_SIGNALING_URL_KEY)

    /** Development-only relay secret. Production will replace this with a per-device MomoCall token. */
    var momoCallSharedSecret: String?
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.MOMO_CALL_SHARED_SECRET_KEY)
        set(value) = value?.trim().takeUnless { it.isNullOrEmpty() }?.let {
            settingsSettings.putString(DaoKeys.Settings.MOMO_CALL_SHARED_SECRET_KEY, it)
        } ?: settingsSettings.remove(DaoKeys.Settings.MOMO_CALL_SHARED_SECRET_KEY)

    /** Stable installation identifier used by MomoCall, separate from the LAN archive bridge. */
    val momoCallDeviceId: String
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.MOMO_CALL_DEVICE_ID_KEY)
            ?: "momocall-${kotlin.random.Random.nextLong().toString(36)}-${kotlin.random.Random.nextLong().toString(36)}".also {
                settingsSettings.putString(DaoKeys.Settings.MOMO_CALL_DEVICE_ID_KEY, it)
            }

    /** Stable installation identity for LAN archive dedupe. It contains no VRChat credential. */
    val lanSyncDeviceId: String
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.LAN_SYNC_DEVICE_ID_KEY)
            ?: "device-${kotlin.random.Random.nextLong().toString(36)}-${kotlin.random.Random.nextLong().toString(36)}".also {
                settingsSettings.putString(DaoKeys.Settings.LAN_SYNC_DEVICE_ID_KEY, it)
            }
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

    /** Last parsed public VRChat target, used to avoid prompting for the same clipboard link after restart. */
    var lastOfficialClipboardTargetKey: String?
        get() = settingsSettings.getStringOrNull(DaoKeys.Settings.LAST_OFFICIAL_CLIPBOARD_TARGET_KEY)
        set(value) = value?.trim().takeUnless { it.isNullOrEmpty() }?.let {
            settingsSettings.putString(DaoKeys.Settings.LAST_OFFICIAL_CLIPBOARD_TARGET_KEY, it)
        } ?: settingsSettings.remove(DaoKeys.Settings.LAST_OFFICIAL_CLIPBOARD_TARGET_KEY)

    /** Durable bounded dedupe for tray notifications across Android process recreation. */
    var notifiedSocialNotificationIds: Set<String>
        get() = settingsSettings
            .getStringOrNull(DaoKeys.Settings.NOTIFIED_SOCIAL_NOTIFICATION_IDS_KEY)
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toSet()
            .orEmpty()
        set(value) {
            val bounded = value.toList().takeLast(MAX_NOTIFIED_SOCIAL_IDS)
            if (bounded.isEmpty()) {
                settingsSettings.remove(DaoKeys.Settings.NOTIFIED_SOCIAL_NOTIFICATION_IDS_KEY)
            } else {
                settingsSettings.putString(
                    DaoKeys.Settings.NOTIFIED_SOCIAL_NOTIFICATION_IDS_KEY,
                    bounded.joinToString("\n"),
                )
            }
        }

    var lastVrchatStatusIndicator: String?
        get() = settingsSettings
            .getStringOrNull(DaoKeys.Settings.LAST_VRCHAT_STATUS_INDICATOR_KEY)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        set(value) = value?.trim()?.takeIf(String::isNotEmpty)?.let {
            settingsSettings.putString(DaoKeys.Settings.LAST_VRCHAT_STATUS_INDICATOR_KEY, it)
        } ?: settingsSettings.remove(DaoKeys.Settings.LAST_VRCHAT_STATUS_INDICATOR_KEY)

    private companion object {
        const val MAX_NOTIFIED_SOCIAL_IDS = 256
    }

}

package io.github.vrcmteam.vrcm.storage

import io.github.vrcmteam.vrcm.network.api.attributes.CookieNames

object DaoKeys{

    const val PREFIX = "vrcm"

    /**
     * 是否是当前的
     */
    const val CURRENT_KEY = "${PREFIX}.current"

    object Account {


        const val NAME = "${PREFIX}.account"

        /**
         * 用户名
         */
        const val USERNAME_KEY = "${PREFIX}.username"

        /**
         * 密码
         */
        const val PASSWORD_KEY = "${PREFIX}.password"

        /**
         * 头像链接
         */
        const val ICON_URL_KEY = "${PREFIX}.iconUrl"

        /**
         * authCookie Token
         */
        const val AUTH_KEY = "${PREFIX}.${CookieNames.AUTH}"

        /**
         * 二步验证Token
         */
        const val TWO_FACTOR_AUTH_KEY = "${PREFIX}.${CookieNames.TWO_FACTOR_AUTH}"
    }

    object Settings {

        const val NAME = "${PREFIX}.settings"

        /**
         * 是否是暗黑模式,为null则为更随系统
         */
        const val IS_DARK_THEME_KEY = "${PREFIX}.isDarkTheme"

        /**
         * 主题颜色名
         */
        const val THEME_COLOR_KEY = "${PREFIX}.themeColor"

        /**
         * 语言
         */
        const val LANGUAGE_TAG_KEY = "${PREFIX}.languageTag"

        /**
         * 记住的版本
         */
        const val REMEMBER_VERSION_KEY = "${PREFIX}.rememberVersion"

        const val BACKGROUND_FRIEND_MONITORING_ENABLED_KEY = "${PREFIX}.backgroundFriendMonitoringEnabled"
        const val SYSTEM_NOTIFICATIONS_ENABLED_KEY = "${PREFIX}.systemNotificationsEnabled"
        const val UNIFIED_ANDROID_NOTIFICATIONS_MIGRATED_KEY = "${PREFIX}.unifiedAndroidNotificationsMigrated"
        const val FRIEND_PRESENCE_NOTIFICATION_SELECTION_KEY = "${PREFIX}.friendPresenceNotificationSelection"
        const val ACTIVITY_LOG_RETENTION_DAYS_KEY = "${PREFIX}.activityLogRetentionDays"
        const val LAN_BRIDGE_URL_KEY = "${PREFIX}.lanBridgeUrl"
        const val LAN_BRIDGE_TOKEN_KEY = "${PREFIX}.lanBridgeToken"
        const val LAN_SYNC_LAST_SUCCESS_AT_KEY = "${PREFIX}.lanSyncLastSuccessAt"
        const val LAN_SYNC_LAST_DIRECTION_KEY = "${PREFIX}.lanSyncLastDirection"
        const val LAN_SYNC_LAST_ERROR_KEY = "${PREFIX}.lanSyncLastError"
        const val LAN_SYNC_AUTO_ENABLED_KEY = "${PREFIX}.lanSyncAutoEnabled"
        const val LAN_SYNC_DEVICE_ID_KEY = "${PREFIX}.lanSyncDeviceId"
        const val LAST_OFFICIAL_CLIPBOARD_TARGET_KEY = "${PREFIX}.lastOfficialClipboardTarget"
        const val NOTIFIED_SOCIAL_NOTIFICATION_IDS_KEY = "${PREFIX}.notifiedSocialNotificationIds"
        const val LAST_VRCHAT_STATUS_INDICATOR_KEY = "${PREFIX}.lastVrchatStatusIndicator"
        const val MOMO_CALL_SIGNALING_URL_KEY = "${PREFIX}.momoCallSignalingUrl"
        const val MOMO_CALL_SHARED_SECRET_KEY = "${PREFIX}.momoCallSharedSecret"
        const val MOMO_CALL_DEVICE_ID_KEY = "${PREFIX}.momoCallDeviceId"

    }

    /**
     * 本地收藏分组设置
     */
    object FavoriteLocal {
        const val NAME = "${PREFIX}.favorite.local"
        const val WORLD_KEY = "${PREFIX}.favorite.local.world"
        const val FRIEND_KEY = "${PREFIX}.favorite.local.friend"
        const val AVATAR_KEY = "${PREFIX}.favorite.local.avatar"
    }

    object FriendNetwork {
        const val NAME = "${PREFIX}.friend.network"
        const val KEY_PREFIX = "${PREFIX}.friend.network.cache"
    }

    object UserProfileCache {
        const val NAME = "${PREFIX}.user.profile.cache"
        const val KEY_PREFIX = "${PREFIX}.user.profile.cache"
    }

    object FriendListCache {
        const val NAME = "${PREFIX}.friend.list.cache"
        const val KEY_PREFIX = "${PREFIX}.friend.list.cache"
    }

    object FriendActivity {
        const val NAME = "${PREFIX}.friend.activity"
        const val KEY_PREFIX = "${PREFIX}.friend.activity.cache"
    }
}

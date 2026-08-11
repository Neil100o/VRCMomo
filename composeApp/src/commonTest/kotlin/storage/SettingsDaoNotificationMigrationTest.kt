package io.github.vrcmteam.vrcm.storage

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsDaoNotificationMigrationTest {
    @Test
    fun `legacy notification settings enable unified switch only when both were enabled`() {
        val storage = MapSettings().apply {
            putBoolean(DaoKeys.Settings.SYSTEM_NOTIFICATIONS_ENABLED_KEY, true)
            putBoolean(DaoKeys.Settings.BACKGROUND_FRIEND_MONITORING_ENABLED_KEY, false)
        }

        val migrated = SettingsDao(storage).settings

        assertFalse(migrated.isSystemNotificationsEnabled)
        assertFalse(migrated.isBackgroundFriendMonitoringEnabled)
    }

    @Test
    fun `saving unified switch keeps legacy mirror aligned`() {
        val storage = MapSettings()
        val dao = SettingsDao(storage)
        val initial = dao.settings

        dao.settings = initial.copy(isSystemNotificationsEnabled = true)

        assertTrue(dao.settings.isSystemNotificationsEnabled)
        assertTrue(dao.settings.isBackgroundFriendMonitoringEnabled)
        assertTrue(storage.getBoolean(DaoKeys.Settings.BACKGROUND_FRIEND_MONITORING_ENABLED_KEY, false))
    }

    @Test
    fun `notified social ids survive process recreation`() {
        val storage = MapSettings()
        SettingsDao(storage).notifiedSocialNotificationIds = linkedSetOf("ntf_1", "ntf_2")

        val restored = SettingsDao(storage).notifiedSocialNotificationIds

        assertTrue("ntf_1" in restored)
        assertTrue("ntf_2" in restored)
    }
}

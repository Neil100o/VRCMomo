package io.github.vrcmteam.vrcm.presentation.settings

import io.github.vrcmteam.vrcm.presentation.settings.data.SettingsVo
import io.github.vrcmteam.vrcm.presentation.settings.locale.LanguageTag
import io.github.vrcmteam.vrcm.presentation.settings.theme.ThemeColor
import io.github.vrcmteam.vrcm.presentation.theme.momo.MomoThemeColor
import io.github.vrcmteam.vrcm.storage.SettingsDao
import io.github.vrcmteam.vrcm.storage.data.SettingsData

class SettingsModel(
    private val settingsDao: SettingsDao,
    private val themeColors: List<ThemeColor>
) {
    fun saveSettings(settingsVo: SettingsVo) {
        settingsDao.settings = settingsVo.let {
            SettingsData(
                isDarkTheme = it.isDarkTheme,
                themeColor = it.themeColor.name,
                languageTag = it.languageTag.tag,
                isBackgroundFriendMonitoringEnabled = it.isBackgroundFriendMonitoringEnabled,
                isSystemNotificationsEnabled = it.isSystemNotificationsEnabled,
            )
        }
    }

    val settingsVo: SettingsVo
        get() {
            val settings = settingsDao.settings
            val languageTag = settings.languageTag?.let { LanguageTag.fromTag(it) } ?: LanguageTag.Default
            // Older installations stored "Default". Treat it as Momo so the rebrand
            // applies without overwriting users who deliberately chose another color.
            val themeColor = settings.themeColor
                ?.takeUnless { it == ThemeColor.Default.name }
                ?.let { name -> themeColors.firstOrNull { it.name == name } }
                ?: MomoThemeColor
            return SettingsVo(
                isDarkTheme = settings.isDarkTheme,
                themeColor = themeColor,
                languageTag = languageTag,
                isBackgroundFriendMonitoringEnabled = settings.isBackgroundFriendMonitoringEnabled,
                isSystemNotificationsEnabled = settings.isSystemNotificationsEnabled,
            )
        }
}

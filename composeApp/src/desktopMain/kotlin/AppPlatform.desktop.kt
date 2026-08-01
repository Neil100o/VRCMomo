package io.github.vrcmteam.vrcm

import java.io.File

class DesktopAppPlatform : AppPlatform {
    override val name: String = System.getProperty("os.name")
    override val version: String = System.getProperty("os.version")
    override val type: AppPlatformType = AppPlatformType.Desktop
    override val persistentDataDirectory: String = desktopPersistentDataDirectory()
}

private fun desktopPersistentDataDirectory(): String {
    val home = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") ->
            System.getenv("LOCALAPPDATA") ?: System.getenv("APPDATA") ?: home
        osName.contains("mac") ->
            File(home, "Library/Application Support").absolutePath
        else ->
            System.getenv("XDG_DATA_HOME") ?: File(home, ".local/share").absolutePath
    }
}

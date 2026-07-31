package io.github.vrcmteam.vrcm

import androidx.compose.runtime.Composable
import org.koin.compose.getKoin
import org.koin.core.component.KoinComponent

interface AppPlatform: KoinComponent {
    val name: String
    val version: String
    val type: AppPlatformType

    /** Background friend monitoring is intentionally opt-in and platform-specific. */
    val supportsBackgroundFriendMonitoring: Boolean
        get() = false

    /** Returns true when the platform can currently show the required monitoring notification. */
    fun hasBackgroundFriendMonitoringPermission(): Boolean = true

    /** Ask the platform to show the permission flow, if one exists. */
    fun requestBackgroundFriendMonitoringPermission() = Unit

    /** Start or stop the opt-in background monitor. */
    fun setBackgroundFriendMonitoringEnabled(enabled: Boolean): BackgroundFriendMonitoringResult =
        BackgroundFriendMonitoringResult.Unsupported
}

enum class BackgroundFriendMonitoringResult {
    Started,
    Stopped,
    PermissionRequired,
    Unsupported,
}

enum class AppPlatformType {
    Android,
    Desktop,
    Ios,
    Web
}

@Composable
fun getAppPlatform(): AppPlatform = getKoin().get()

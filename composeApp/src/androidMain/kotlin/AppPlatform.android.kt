package io.github.vrcmteam.vrcm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.github.vrcmteam.vrcm.service.FriendActivityForegroundService

class AndroidAppPlatform(val context: Context) : AppPlatform {
    override val name: String = "Android"
    override val version: String = Build.VERSION.SDK_INT.toString()
    override val type: AppPlatformType = AppPlatformType.Android
    override val supportsBackgroundFriendMonitoring: Boolean = true

    override fun hasBackgroundFriendMonitoringPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun requestBackgroundFriendMonitoringPermission() {
        MainActivity.requestNotificationPermissionFromCurrentActivity()
    }

    override fun setBackgroundFriendMonitoringEnabled(
        enabled: Boolean,
    ): BackgroundFriendMonitoringResult {
        val serviceIntent = Intent(context, FriendActivityForegroundService::class.java)
        if (!enabled) {
            context.stopService(serviceIntent)
            return BackgroundFriendMonitoringResult.Stopped
        }
        if (!hasBackgroundFriendMonitoringPermission()) {
            return BackgroundFriendMonitoringResult.PermissionRequired
        }
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            BackgroundFriendMonitoringResult.Started
        }.getOrElse {
            BackgroundFriendMonitoringResult.Unsupported
        }
    }
}

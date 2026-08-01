package io.github.vrcmteam.vrcm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import io.github.vrcmteam.vrcm.service.FriendActivityForegroundService

class AndroidAppPlatform(val context: Context) : AppPlatform {
    override val name: String = "Android"
    override val version: String = Build.VERSION.SDK_INT.toString()
    override val type: AppPlatformType = AppPlatformType.Android
    override val persistentDataDirectory: String = context.filesDir.absolutePath
    override val supportsBackgroundFriendMonitoring: Boolean = true
    override val supportsBatteryOptimizationSettings: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    override fun hasBackgroundFriendMonitoringPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun requestBackgroundFriendMonitoringPermission() {
        MainActivity.requestNotificationPermissionFromCurrentActivity()
    }

    override fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    override fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val requestIntent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallbackIntent = Intent(
            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(requestIntent) }
            .recoverCatching { context.startActivity(fallbackIntent) }
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

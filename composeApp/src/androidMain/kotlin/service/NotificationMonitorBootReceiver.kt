package io.github.vrcmteam.vrcm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.vrcmteam.vrcm.AppPlatform
import io.github.vrcmteam.vrcm.storage.SettingsDao
import org.koin.core.context.GlobalContext

/** Restores the user-enabled notification monitor after reboot or an in-place app update. */
class NotificationMonitorBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESTORE_ACTIONS) return
        val koin = GlobalContext.getOrNull() ?: return
        if (koin.get<SettingsDao>().settings.isSystemNotificationsEnabled) {
            koin.get<AppPlatform>().setBackgroundFriendMonitoringEnabled(true)
        }
    }

    private companion object {
        val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

package io.github.vrcmteam.vrcm.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.websocket.WebSocketApi
import io.github.vrcmteam.vrcm.presentation.notifications.AndroidPlatformNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * User-enabled Android foreground service that keeps the VRChat friend feed alive while the app is
 * backgrounded. Android may still stop it after a force-stop, reboot, permission change, or under
 * device-specific power management, so the UI deliberately presents this as best-effort monitoring.
 */
class FriendActivityForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startMonitoringForeground()

        val koin = GlobalContext.get()
        // Install collectors before authentication is emitted when Android recreates only this service.
        koin.get<WebSocketApi>()
        koin.get<FriendService>()
        koin.get<SocialNotificationService>().start()
        val authService = koin.get<AuthService>()

        serviceScope.launch {
            val authenticated = authService
                .reTryAuthCatching { authService.isAuthed() }
                .getOrDefault(false)
            if (!authenticated) stopSelf()
        }
        serviceScope.launch {
            SharedFlowCentre.logout.collect {
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoringForeground() {
        val notification = AndroidPlatformNotificationService(this)
            .buildBackgroundMonitoringNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 0x4D4F4D4F
    }
}

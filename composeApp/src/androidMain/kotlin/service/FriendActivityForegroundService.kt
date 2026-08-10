package io.github.vrcmteam.vrcm.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.websocket.WebSocketApi
import io.github.vrcmteam.vrcm.presentation.notifications.AndroidPlatformNotificationService
import io.github.vrcmteam.vrcm.storage.SettingsDao
import io.github.vrcmteam.vrcm.storage.data.LanSyncStatus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * User-enabled Android foreground service that keeps the VRChat friend feed alive while the app is
 * backgrounded. Android may still stop it after a force-stop, reboot, permission change, or under
 * device-specific power management, so the UI deliberately presents this as best-effort monitoring.
 */
@OptIn(ExperimentalTime::class)
class FriendActivityForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var incomingBoopNotificationService: IncomingBoopNotificationService? = null

    override fun onCreate() {
        super.onCreate()
        startMonitoringForeground()

        val koin = GlobalContext.get()
        // Install collectors before authentication is emitted when Android recreates only this service.
        koin.get<WebSocketApi>()
        val friendService = koin.get<FriendService>()
        koin.get<SocialNotificationService>().start()
        incomingBoopNotificationService = koin.get<IncomingBoopNotificationService>().also { it.start() }
        val authService = koin.get<AuthService>()

        serviceScope.launch {
            // A short network loss must not destroy the foreground service. Keep retrying session
            // restoration while the user has explicitly enabled monitoring; WebSocketApi reconnects
            // independently once currentSession becomes available.
            if (authService.accountDtoOrNull() == null) {
                stopSelf()
                return@launch
            }
            while (isActive && SharedFlowCentre.currentSession.value == null) {
                val authenticated = authService
                    .reTryAuthCatching { authService.isAuthed() }
                    .getOrDefault(false)
                if (authenticated) break
                delay(AUTH_RETRY_DELAY_MILLIS)
            }
        }
        serviceScope.launch {
            // WebSocket events are the fast path, but reconnecting does not replay transitions that
            // happened while the phone had no network. A low-frequency full refresh repairs that
            // gap and feeds the same friendState-based notification tracker.
            while (isActive) {
                delay(FRIEND_REFRESH_INTERVAL_MILLIS)
                if (SharedFlowCentre.currentSession.value != null) {
                    friendService.refreshFriendList()
                }
            }
        }
        val settingsDao = koin.get<SettingsDao>()
        val lanBridgeClient = koin.get<LanActivityBridgeClient>()
        val activityService = koin.get<FriendActivityService>()
        serviceScope.launch {
            // A launch-time sync is enough for cross-device history and avoids waking the
            // radio every few minutes while friend monitoring is otherwise idle.
            SharedFlowCentre.currentSession.filterNotNull().first()
            if (!settingsDao.settings.isLanSyncAutoEnabled) return@launch
            runCatching {
                val pairing = LanBridgePairing.fromInput(
                    settingsDao.lanBridgeUrl.orEmpty(),
                    settingsDao.lanBridgeToken.orEmpty(),
                )
                val preview = activityService.previewVrcxActivityImport(lanBridgeClient.fetchVrcxActivity(pairing))
                activityService.applyVrcxActivityImport(preview)
                lanBridgeClient.uploadVrcmomoActivity(pairing, activityService.exportLanActivitySync())
            }.onSuccess {
                settingsDao.lanSyncStatus = LanSyncStatus(
                    lastSuccessAtMillis = Clock.System.now().toEpochMilliseconds(),
                    lastDirection = "automatic",
                )
            }.onFailure { error ->
                settingsDao.lanSyncStatus = settingsDao.lanSyncStatus.copy(
                    lastError = error.message?.take(MAX_SYNC_ERROR_LENGTH) ?: "自动同步失败",
                )
            }
        }
        serviceScope.launch {
            SharedFlowCentre.logout.collect {
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        incomingBoopNotificationService?.stop()
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
        const val AUTH_RETRY_DELAY_MILLIS = 30_000L
        const val FRIEND_REFRESH_INTERVAL_MILLIS = 120_000L
        const val MAX_SYNC_ERROR_LENGTH = 160
    }
}

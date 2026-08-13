package io.github.vrcmteam.vrcm

import android.app.Application
import io.github.vrcmteam.vrcm.di.commonModules
import io.github.vrcmteam.vrcm.di.modules.platformModule
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.service.LanBridgePairing
import io.github.vrcmteam.vrcm.service.LanActivitySyncService
import io.github.vrcmteam.vrcm.storage.SettingsDao
import io.github.vrcmteam.vrcm.storage.data.LanSyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

@OptIn(ExperimentalTime::class)
class VRCMApplication : Application() {
    private val launchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidLogger()
            androidContext(this@VRCMApplication)
            modules(commonModules + platformModule)
        }
        val koin = koinApplication.koin
        if (koin.get<SettingsDao>().settings.isSystemNotificationsEnabled) {
            // Do not request permission automatically. If Android revoked it, the service remains
            // stopped and the user can review the setting the next time they open the app.
            koin.get<AppPlatform>().setBackgroundFriendMonitoringEnabled(true)
        }
        launchScope.launch {
            // This runs once per process launch after authentication is available. It deliberately
            // does not depend on foreground monitoring or use a repeating background timer.
            SharedFlowCentre.currentSession.filterNotNull().first()
            val settingsDao = koin.get<SettingsDao>()
            if (!settingsDao.settings.isLanSyncAutoEnabled) return@launch
            runCatching {
                val pairing = LanBridgePairing.fromInput(
                    settingsDao.lanBridgeUrl.orEmpty(),
                    settingsDao.lanBridgeToken.orEmpty(),
                )
                koin.get<LanActivitySyncService>().sync(pairing)
            }.onSuccess {
                settingsDao.lanSyncStatus = LanSyncStatus(
                    lastSuccessAtMillis = Clock.System.now().toEpochMilliseconds(),
                    lastDirection = "automatic",
                    // A successful exchange supersedes any previous transient
                    // network error (for example, a VPN-blocked LAN request).
                    lastError = null,
                )
            }.onFailure { error ->
                settingsDao.lanSyncStatus = settingsDao.lanSyncStatus.copy(
                    lastError = error.message?.take(MAX_SYNC_ERROR_LENGTH) ?: "自动同步失败",
                )
            }
        }
    }

    private companion object {
        const val MAX_SYNC_ERROR_LENGTH = 160
    }
}

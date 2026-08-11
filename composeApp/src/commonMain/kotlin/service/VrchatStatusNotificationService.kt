package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.status.VrchatStatusApi
import io.github.vrcmteam.vrcm.presentation.notifications.PlatformNotificationService
import io.github.vrcmteam.vrcm.presentation.notifications.SystemNotification
import io.github.vrcmteam.vrcm.storage.SettingsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Polls VRChat's public Statuspage endpoint while Android notification monitoring is active. */
class VrchatStatusNotificationService(
    private val api: VrchatStatusApi,
    private val settingsDao: SettingsDao,
    private val platformNotificationService: PlatformNotificationService,
) {
    private var scope = newScope()
    private var monitorJob: Job? = null

    fun start() {
        if (monitorJob != null) return
        if (!scope.isActive) scope = newScope()
        monitorJob = scope.launch {
            while (isActive) {
                checkOnce()
                delay(STATUS_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        scope.cancel()
    }

    internal suspend fun checkOnce() {
        if (!settingsDao.settings.isSystemNotificationsEnabled) return
        val status = api.fetchStatus().getOrNull()?.status ?: return
        val current = status.indicator.trim().lowercase().ifEmpty { "none" }
        val previous = settingsDao.lastVrchatStatusIndicator
        if (current == previous) return

        settingsDao.lastVrchatStatusIndicator = current
        when {
            current != "none" -> platformNotificationService.show(
                SystemNotification(
                    id = "vrchat-status-$current",
                    title = "VRChat 服务异常",
                    message = statusMessage(current, status.description),
                ),
            )
            previous != null && previous != "none" -> platformNotificationService.show(
                SystemNotification(
                    id = "vrchat-status-restored",
                    title = "VRChat 服务已恢复",
                    message = "所有系统已恢复正常",
                ),
            )
        }
    }

    private fun statusMessage(indicator: String, description: String): String = when (indicator) {
        "minor" -> "部分服务异常"
        "major" -> "服务大范围异常"
        "critical" -> "服务严重中断"
        else -> description.trim().ifEmpty { "服务状态异常" }
    }

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        const val STATUS_REFRESH_INTERVAL_MILLIS = 5 * 60_000L
    }
}

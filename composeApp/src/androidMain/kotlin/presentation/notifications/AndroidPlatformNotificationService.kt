package io.github.vrcmteam.vrcm.presentation.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.github.vrcmteam.vrcm.MainActivity
import io.github.vrcmteam.vrcm.R
import io.github.vrcmteam.vrcm.network.websocket.WebSocketConnectionState
import java.util.Locale

class AndroidPlatformNotificationService(context: Context) : PlatformNotificationService {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureSocialChannel()
    }

    override fun requestPermission() = Unit

    override fun show(notification: SystemNotification) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureSocialChannel()
        notificationManager.notify(
            notification.id.hashCode(),
            newBuilder(SOCIAL_CHANNEL_ID, notification.id.hashCode())
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .build(),
        )
    }

    fun buildBackgroundMonitoringNotification(
        state: WebSocketConnectionState = WebSocketConnectionState.Connecting,
    ): Notification {
        ensureBackgroundMonitoringChannel()
        val builder = newBuilder(BACKGROUND_MONITORING_CHANNEL_ID, BACKGROUND_NOTIFICATION_REQUEST_CODE)
            .setContentTitle(appContext.getString(R.string.background_monitoring_notification_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
        when (state) {
            WebSocketConnectionState.Idle,
            WebSocketConnectionState.Connecting -> builder
                .setContentText(appContext.getString(R.string.background_monitoring_connecting))
                .setShowWhen(false)
            is WebSocketConnectionState.Connected -> builder
                .setContentText(appContext.getString(R.string.background_monitoring_connected))
                .setWhen(state.connectedAtEpochMillis)
                .setUsesChronometer(true)
                .setShowWhen(true)
            is WebSocketConnectionState.Disconnected -> builder
                .setContentText(
                    appContext.getString(
                        R.string.background_monitoring_disconnected,
                        formatDuration(state.connectedDurationMillis),
                    ),
                )
                .setUsesChronometer(false)
                .setShowWhen(false)
        }
        return builder.build()
    }

    fun updateBackgroundMonitoringNotification(state: WebSocketConnectionState) {
        notificationManager.notify(
            BACKGROUND_NOTIFICATION_ID,
            buildBackgroundMonitoringNotification(state),
        )
    }

    private fun newBuilder(channelId: String, requestCode: Int): Notification.Builder {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(appContext, channelId)
        } else {
            Notification.Builder(appContext)
        }
        return builder
            .setSmallIcon(R.drawable.ic_stat_vrcmomo)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_HIGH)
    }

    private fun ensureSocialChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SOCIAL_CHANNEL_ID,
                appContext.getString(R.string.social_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = appContext.getString(R.string.social_notification_channel_description)
            },
        )
    }

    private fun ensureBackgroundMonitoringChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                BACKGROUND_MONITORING_CHANNEL_ID,
                appContext.getString(R.string.background_monitoring_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = appContext.getString(R.string.background_monitoring_channel_description)
                setShowBadge(false)
            },
        )
    }

    private fun formatDuration(durationMillis: Long?): String {
        if (durationMillis == null) return "--:--"
        val totalSeconds = durationMillis.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    companion object {
        // Android channels cannot change importance after creation. A new ID upgrades users who
        // previously had the old, quiet VRCMomo social channel.
        const val SOCIAL_CHANNEL_ID = "vrcmomo_social_v2"
        const val BACKGROUND_MONITORING_CHANNEL_ID = "vrcmomo_background_monitoring"
        const val BACKGROUND_NOTIFICATION_REQUEST_CODE = 0x4D4F
        const val BACKGROUND_NOTIFICATION_ID = 0x4D4F4D4F
    }
}

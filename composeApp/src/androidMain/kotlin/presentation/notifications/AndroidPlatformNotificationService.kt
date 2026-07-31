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

class AndroidPlatformNotificationService(context: Context) : PlatformNotificationService {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureChannel()
    }

    override fun requestPermission() = Unit

    override fun show(notification: SystemNotification) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel()
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(appContext, CHANNEL_ID)
        } else {
            Notification.Builder(appContext)
        }
        notificationManager.notify(
            notification.id.hashCode(),
            builder
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .build(),
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "VRCMomo \u793e\u4ea4\u901a\u77e5",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Boop \u548c\u6536\u85cf\u597d\u53cb\u72b6\u6001\u63d0\u9192"
            },
        )
    }

    private companion object {
        const val CHANNEL_ID = "vrcmomo_social"
    }
}

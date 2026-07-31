package io.github.vrcmteam.vrcm.presentation.notifications

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosPlatformNotificationService : PlatformNotificationService {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override fun requestPermission() {
        notificationCenter.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
            completionHandler = { _, _ -> },
        )
    }

    override fun show(notification: SystemNotification) {
        val content = UNMutableNotificationContent().apply {
            title = notification.title
            body = notification.message
            sound = UNNotificationSound.defaultSound
            threadIdentifier = "vrcmomo-social"
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = 1.0,
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notification.id,
            content = content,
            trigger = trigger,
        )
        notificationCenter.addNotificationRequest(request) { _ -> }
    }
}

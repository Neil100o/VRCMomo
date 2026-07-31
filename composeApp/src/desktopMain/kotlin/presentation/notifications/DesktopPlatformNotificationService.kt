package io.github.vrcmteam.vrcm.presentation.notifications

/** Desktop currently keeps social alerts inside the app UI. */
class DesktopPlatformNotificationService : PlatformNotificationService {
    override fun requestPermission() = Unit

    override fun show(notification: SystemNotification) = Unit
}

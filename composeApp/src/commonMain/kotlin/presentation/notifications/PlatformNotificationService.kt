package io.github.vrcmteam.vrcm.presentation.notifications

/** A user-visible notification that can be delivered by the current platform. */
data class SystemNotification(
    val id: String,
    val title: String,
    val message: String,
)

/**
 * Platform bridge for local notifications.
 *
 * Notifications are generated while VRCMomo is running and receiving VRChat
 * websocket events; this is intentionally not a remote-push abstraction.
 */
interface PlatformNotificationService {
    fun requestPermission()

    fun show(notification: SystemNotification)
}

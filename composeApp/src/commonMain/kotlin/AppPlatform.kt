package io.github.vrcmteam.vrcm

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.getKoin
import org.koin.core.component.KoinComponent

interface AppPlatform: KoinComponent {
    val name: String
    val version: String
    val type: AppPlatformType

    /** App-private, non-cache directory for durable VRCMomo data. */
    val persistentDataDirectory: String

    /** Background friend monitoring is intentionally opt-in and platform-specific. */
    val supportsBackgroundFriendMonitoring: Boolean
        get() = false

    /** Returns true when the platform can currently show the required monitoring notification. */
    fun hasBackgroundFriendMonitoringPermission(): Boolean = true

    /** Ask the platform to show the permission flow, if one exists. */
    fun requestBackgroundFriendMonitoringPermission() = Unit

    /** Start or stop the opt-in background monitor. */
    fun setBackgroundFriendMonitoringEnabled(enabled: Boolean): BackgroundFriendMonitoringResult =
        BackgroundFriendMonitoringResult.Unsupported

    /** Android-only shortcut for reviewing battery optimization restrictions. */
    val supportsBatteryOptimizationSettings: Boolean
        get() = false

    /** Non-Android platforms are treated as unrestricted because the setting is not shown there. */
    fun isIgnoringBatteryOptimizations(): Boolean = true

    /** Open the platform battery optimization page after an explicit user action. */
    fun openBatteryOptimizationSettings() = Unit

    /** MomoCall is deliberately Android-first in the experimental phase. */
    val supportsMomoCall: Boolean
        get() = false

    val momoCallState: StateFlow<MomoCallState>
        get() = unsupportedMomoCallState

    /** Starts the separate MomoCall relay connection; it never uses the VRChat session cookie. */
    suspend fun connectMomoCall(): MomoCallActionResult = MomoCallActionResult.Unsupported

    /** Starts an audio-only call to the selected VRChat contact identity. */
    suspend fun callMomoUser(userId: String): MomoCallActionResult = MomoCallActionResult.Unsupported

    suspend fun acceptMomoCall(): MomoCallActionResult = MomoCallActionResult.Unsupported

    suspend fun rejectMomoCall(): MomoCallActionResult = MomoCallActionResult.Unsupported

    suspend fun hangUpMomoCall(): MomoCallActionResult = MomoCallActionResult.Unsupported
}

sealed interface MomoCallState {
    data object Idle : MomoCallState
    data class Connecting(val targetUserId: String) : MomoCallState
    data class Incoming(val callId: String, val fromUserId: String) : MomoCallState
    data class InCall(val peerUserId: String) : MomoCallState
    data class Failed(val message: String) : MomoCallState
    data object Unsupported : MomoCallState
}

enum class MomoCallActionResult {
    Started,
    PermissionRequired,
    Failed,
    Unsupported,
}

private val unsupportedMomoCallState = MutableStateFlow<MomoCallState>(MomoCallState.Unsupported)

enum class BackgroundFriendMonitoringResult {
    Started,
    Stopped,
    PermissionRequired,
    Unsupported,
}

enum class AppPlatformType {
    Android,
    Desktop,
    Ios,
    Web
}

@Composable
fun getAppPlatform(): AppPlatform = getKoin().get()

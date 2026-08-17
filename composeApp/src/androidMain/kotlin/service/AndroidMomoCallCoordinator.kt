package io.github.vrcmteam.vrcm.service

import android.content.Context
import io.github.vrcmteam.vrcm.MomoCallState
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallIceCandidate
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSessionDescription
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSignal
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Android-only call coordinator. It translates generic MomoCall relay messages into WebRTC
 * audio operations. The shared transport contains no VRChat session cookie.
 */
class AndroidMomoCallCoordinator(
    private val context: Context,
    private val authService: AuthService,
    private val signalService: MomoCallSignalService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var audioEngine: AndroidMomoCallAudioEngine? = null
    private var activeCallId: String? = null
    private var peerUserId: String? = null
    private val _state = MutableStateFlow<MomoCallState>(MomoCallState.Idle)
    val state: StateFlow<MomoCallState> = _state.asStateFlow()

    init {
        scope.launch {
            signalService.signals.filterNotNull().collect(::handleSignal)
        }
    }

    suspend fun connect() {
        signalService.connect(authService.accountDto().userId)
    }

    suspend fun placeCall(targetUserId: String) {
        val callId = signalService.startCall(targetUserId)
        activeCallId = callId
        peerUserId = targetUserId
        _state.value = MomoCallState.Connecting(targetUserId)
    }

    suspend fun acceptIncoming() {
        val current = _state.value as? MomoCallState.Incoming ?: return
        activeCallId = current.callId
        peerUserId = current.fromUserId
        signalService.sendCallSignal(MomoCallSignalType.Accept, current.callId, current.fromUserId)
        _state.value = MomoCallState.Connecting(current.fromUserId)
    }

    suspend fun rejectIncoming() {
        val current = _state.value as? MomoCallState.Incoming ?: return
        signalService.sendCallSignal(MomoCallSignalType.Reject, current.callId, current.fromUserId)
        clearCall()
    }

    suspend fun hangUp() {
        val callId = activeCallId
        val peer = peerUserId
        if (callId != null && peer != null) {
            signalService.sendCallSignal(MomoCallSignalType.Hangup, callId, peer)
        }
        clearCall()
    }

    private suspend fun handleSignal(signal: MomoCallSignal) {
        when (signal.type) {
            MomoCallSignalType.Invite -> {
                val callId = signal.callId ?: return
                val sender = signal.fromUserId ?: return
                if (activeCallId == null) _state.value = MomoCallState.Incoming(callId, sender)
            }
            MomoCallSignalType.Accept -> if (isActiveSignal(signal)) {
                ensureAudioEngine().createOffer()
            }
            MomoCallSignalType.Offer -> if (isActiveSignal(signal)) {
                signal.sdp?.let { ensureAudioEngine().acceptOffer(it) }
            }
            MomoCallSignalType.Answer -> if (isActiveSignal(signal)) {
                signal.sdp?.let { ensureAudioEngine().acceptAnswer(it) }
            }
            MomoCallSignalType.Ice -> if (isActiveSignal(signal)) {
                signal.candidate?.let { ensureAudioEngine().addIceCandidate(it) }
            }
            MomoCallSignalType.Reject,
            MomoCallSignalType.Hangup -> if (isActiveSignal(signal)) clearCall()
        }
    }

    private fun isActiveSignal(signal: MomoCallSignal): Boolean =
        signal.callId == activeCallId && signal.fromUserId == peerUserId

    private fun ensureAudioEngine(): AndroidMomoCallAudioEngine = audioEngine ?: AndroidMomoCallAudioEngine(
        context = context,
        onLocalDescription = { description -> sendDescription(description) },
        onIceCandidate = { candidate -> sendCandidate(candidate) },
        onConnected = {
            peerUserId?.let { _state.value = MomoCallState.InCall(it) }
        },
    ).also { audioEngine = it }

    private fun sendDescription(description: MomoCallSessionDescription) {
        scope.launch {
            val type = when (description.type) {
                "offer" -> MomoCallSignalType.Offer
                "answer" -> MomoCallSignalType.Answer
                else -> return@launch
            }
            sendCallPayload(type) { copy(sdp = description) }
        }
    }

    private fun sendCandidate(candidate: MomoCallIceCandidate) {
        scope.launch {
            sendCallPayload(MomoCallSignalType.Ice) { copy(candidate = candidate) }
        }
    }

    private suspend fun sendCallPayload(
        type: String,
        update: MomoCallSignal.() -> MomoCallSignal,
    ) {
        val callId = activeCallId ?: return
        val target = peerUserId ?: return
        val base = MomoCallSignal(
            type = type,
            callId = callId,
            targetUserId = target,
        )
        signalService.send(update(base))
    }

    private suspend fun clearCall() {
        audioEngine?.close()
        audioEngine = null
        activeCallId = null
        peerUserId = null
        _state.value = MomoCallState.Idle
    }
}

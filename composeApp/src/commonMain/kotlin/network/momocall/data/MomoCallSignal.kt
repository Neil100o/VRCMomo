package io.github.vrcmteam.vrcm.network.momocall.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MOMO_CALL_PROTOCOL_VERSION = 1

@Serializable
data class MomoCallSignal(
    val version: Int = MOMO_CALL_PROTOCOL_VERSION,
    val type: String,
    val userId: String? = null,
    val deviceId: String? = null,
    val sharedSecret: String? = null,
    val callId: String? = null,
    val targetUserId: String? = null,
    val fromUserId: String? = null,
    val sdp: MomoCallSessionDescription? = null,
    val candidate: MomoCallIceCandidate? = null,
    val message: String? = null,
)

@Serializable
data class MomoCallSessionDescription(
    val type: String,
    val sdp: String,
)

@Serializable
data class MomoCallIceCandidate(
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0,
    val candidate: String,
)

object MomoCallSignalType {
    const val Register = "register"
    const val Registered = "registered"
    const val Error = "error"
    const val Invite = "call.invite"
    const val Accept = "call.accept"
    const val Reject = "call.reject"
    const val Offer = "call.offer"
    const val Answer = "call.answer"
    const val Ice = "call.ice"
    const val Hangup = "call.hangup"
}

fun MomoCallSignal.isValid(): Boolean = when (type) {
    MomoCallSignalType.Register -> !userId.isNullOrBlank() && !deviceId.isNullOrBlank()
    MomoCallSignalType.Registered,
    MomoCallSignalType.Error -> true
    MomoCallSignalType.Invite,
    MomoCallSignalType.Accept,
    MomoCallSignalType.Reject,
    MomoCallSignalType.Offer,
    MomoCallSignalType.Answer,
    MomoCallSignalType.Ice,
    MomoCallSignalType.Hangup -> !callId.isNullOrBlank() && !fromUserId.isNullOrBlank()
    else -> false
}

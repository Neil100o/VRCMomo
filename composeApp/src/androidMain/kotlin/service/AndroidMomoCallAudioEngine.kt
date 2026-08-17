package io.github.vrcmteam.vrcm.service

import android.content.Context
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallIceCandidate
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSessionDescription
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule

/** Android-only WebRTC audio engine. No VRChat credential reaches this layer. */
class AndroidMomoCallAudioEngine(
    context: Context,
    private val onLocalDescription: (MomoCallSessionDescription) -> Unit,
    private val onIceCandidate: (MomoCallIceCandidate) -> Unit,
    private val onConnected: () -> Unit,
) {
    private val applicationContext = context.applicationContext
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null

    fun createOffer() {
        val peer = ensurePeerConnection()
        peer.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(description: SessionDescription) {
                peer.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() = onLocalDescription(description.toMomoCall())
                }, description)
            }
        }, MediaConstraints())
    }

    fun acceptOffer(offer: MomoCallSessionDescription) {
        val peer = ensurePeerConnection()
        peer.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                peer.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(description: SessionDescription) {
                        peer.setLocalDescription(object : SimpleSdpObserver() {
                            override fun onSetSuccess() = onLocalDescription(description.toMomoCall())
                        }, description)
                    }
                }, MediaConstraints())
            }
        }, offer.toWebRtc())
    }

    fun acceptAnswer(answer: MomoCallSessionDescription) {
        ensurePeerConnection().setRemoteDescription(object : SimpleSdpObserver() {}, answer.toWebRtc())
    }

    fun addIceCandidate(candidate: MomoCallIceCandidate) {
        ensurePeerConnection().addIceCandidate(
            IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate),
        )
    }

    fun close() {
        peerConnection?.dispose()
        peerConnection = null
        audioSource?.dispose()
        audioSource = null
        factory?.dispose()
        factory = null
        audioDeviceModule?.release()
        audioDeviceModule = null
    }

    private fun ensurePeerConnection(): PeerConnection = peerConnection ?: run {
        if (factory == null) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions(),
            )
            audioDeviceModule = JavaAudioDeviceModule.builder(applicationContext).createAudioDeviceModule()
            factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory()
        }
        val newPeer = requireNotNull(factory).createPeerConnection(
            PeerConnection.RTCConfiguration(emptyList()),
            object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) = Unit
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
                override fun onIceCandidate(candidate: IceCandidate) = onIceCandidate(
                    MomoCallIceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp),
                )
                override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) = Unit
                override fun onAddStream(stream: org.webrtc.MediaStream) = Unit
                override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
                override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(receiver: org.webrtc.RtpReceiver, mediaStreams: Array<org.webrtc.MediaStream>) = Unit
                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    if (newState == PeerConnection.PeerConnectionState.CONNECTED) onConnected()
                }
            },
        ) ?: error("Unable to create WebRTC peer connection")
        audioSource = requireNotNull(factory).createAudioSource(MediaConstraints())
        val track: AudioTrack = requireNotNull(factory).createAudioTrack("momo-call-audio", requireNotNull(audioSource))
        newPeer.addTrack(track)
        peerConnection = newPeer
        newPeer
    }
}

private abstract class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(description: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String) = Unit
    override fun onSetFailure(error: String) = Unit
}

private fun SessionDescription.toMomoCall() = MomoCallSessionDescription(
    type = type.canonicalForm(),
    sdp = description,
)

private fun MomoCallSessionDescription.toWebRtc() = SessionDescription(
    SessionDescription.Type.fromCanonicalForm(type),
    sdp,
)

package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.momocall.data.MOMO_CALL_PROTOCOL_VERSION
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSignal
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSignalType
import io.github.vrcmteam.vrcm.network.momocall.data.isValid
import io.github.vrcmteam.vrcm.storage.SettingsDao
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.logger.Logger
import kotlin.random.Random
import kotlinx.serialization.json.Json

sealed interface MomoCallConnectionState {
    data object Idle : MomoCallConnectionState
    data object Connecting : MomoCallConnectionState
    data class Connected(val userId: String) : MomoCallConnectionState
    data class Failed(val message: String) : MomoCallConnectionState
}

/**
 * Independent MomoCall signalling transport.
 *
 * This client deliberately uses a fresh HttpClient rather than the VRChat API client, so neither
 * request defaults nor VRChat cookies are part of the MomoCall connection.
 */
class MomoCallSignalService(
    private val settingsDao: SettingsDao,
    private val logger: Logger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var connectionJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    private var currentUserId: String? = null
    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        }
    }

    private val _connectionState = MutableStateFlow<MomoCallConnectionState>(MomoCallConnectionState.Idle)
    val connectionState: StateFlow<MomoCallConnectionState> = _connectionState.asStateFlow()

    private val _signals = MutableStateFlow<MomoCallSignal?>(null)
    val signals: StateFlow<MomoCallSignal?> = _signals.asStateFlow()

    suspend fun connect(userId: String) {
        mutex.withLock {
            require(userId.isNotBlank()) { "MomoCall requires a VRChat user ID." }
            val signalingUrl = settingsDao.momoCallSignalingUrl
                ?: error("MomoCall signalling URL has not been configured.")
            connectionJob?.cancelAndJoin()
            _connectionState.value = MomoCallConnectionState.Connecting
            currentUserId = userId
            connectionJob = scope.launch {
            try {
                val opened = client.webSocketSession(urlString = signalingUrl)
                session = opened
                opened.sendSerialized(
                    MomoCallSignal(
                        type = MomoCallSignalType.Register,
                        userId = userId,
                        deviceId = settingsDao.momoCallDeviceId,
                        sharedSecret = settingsDao.momoCallSharedSecret.orEmpty(),
                    ),
                )
                while (true) {
                    val signal = opened.receiveDeserialized<MomoCallSignal>()
                    if (!signal.isValid()) continue
                    when (signal.type) {
                        MomoCallSignalType.Registered -> _connectionState.value = MomoCallConnectionState.Connected(userId)
                        MomoCallSignalType.Error -> _connectionState.value = MomoCallConnectionState.Failed(signal.message ?: "MomoCall signalling error")
                        else -> _signals.value = signal
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logger.error("MomoCall signalling stopped: ${error.message.orEmpty()}")
                _connectionState.value = MomoCallConnectionState.Failed(error.message ?: "MomoCall connection failed")
            } finally {
                session = null
            }
            }
        }
        when (val result = withTimeout(CONNECTION_TIMEOUT_MILLIS) {
            connectionState.first { it is MomoCallConnectionState.Connected || it is MomoCallConnectionState.Failed }
        }) {
            is MomoCallConnectionState.Connected -> Unit
            is MomoCallConnectionState.Failed -> error(result.message)
            else -> error("MomoCall connection did not settle.")
        }
    }

    suspend fun startCall(targetUserId: String): String {
        val userId = currentUserId ?: error("Connect MomoCall before placing a call.")
        require(targetUserId.isNotBlank()) { "A call target is required." }
        val callId = "call-${Random.nextLong().toString(36)}-${Random.nextLong().toString(36)}"
        send(
            MomoCallSignal(
                type = MomoCallSignalType.Invite,
                callId = callId,
                targetUserId = targetUserId,
                fromUserId = userId,
            ),
        )
        return callId
    }

    suspend fun sendCallSignal(
        type: String,
        callId: String,
        targetUserId: String,
    ) {
        send(
            MomoCallSignal(
                type = type,
                callId = callId,
                targetUserId = targetUserId,
            ),
        )
    }

    suspend fun send(signal: MomoCallSignal) {
        val activeSession = session ?: error("MomoCall is not connected.")
        activeSession.sendSerialized(signal.copy(version = MOMO_CALL_PROTOCOL_VERSION))
    }

    suspend fun disconnect() = mutex.withLock {
        connectionJob?.cancelAndJoin()
        connectionJob = null
        session = null
        currentUserId = null
        _connectionState.value = MomoCallConnectionState.Idle
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLIS = 10_000L
    }
}

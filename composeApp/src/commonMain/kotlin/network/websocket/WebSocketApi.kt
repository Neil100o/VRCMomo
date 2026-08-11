package io.github.vrcmteam.vrcm.network.websocket

import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.core.shared.AccountSessionToken
import io.github.vrcmteam.vrcm.core.shared.AccountWebSocketEvent
import io.github.vrcmteam.vrcm.network.api.attributes.AUTH_API_PREFIX
import io.github.vrcmteam.vrcm.network.api.attributes.VRC_WSS_URL
import io.github.vrcmteam.vrcm.network.api.auth.data.AuthData
import io.github.vrcmteam.vrcm.network.websocket.data.WebSocketEvent
import io.github.vrcmteam.vrcm.presentation.compoments.ToastText
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.logger.Logger
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface WebSocketConnectionState {
    data object Idle : WebSocketConnectionState
    data object Connecting : WebSocketConnectionState
    data class Connected(val connectedAtEpochMillis: Long) : WebSocketConnectionState
    data class Disconnected(
        val connectedDurationMillis: Long?,
        val consecutiveFailures: Int,
    ) : WebSocketConnectionState
}

@OptIn(ExperimentalTime::class)
class WebSocketApi(
    private val apiClient: HttpClient,
    private val logger: Logger,
) {

    private var currentJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Idle)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    init {
        // StateFlow replays the active account to a foreground service that is created after
        // authentication, unlike the one-shot `authed` event. This is essential after Android
        // recreates only the background service process.
        scope.launch {
            SharedFlowCentre.currentSession.collect { session ->
                currentJob?.cancelAndJoin()
                _connectionState.value = if (session == null) {
                    WebSocketConnectionState.Idle
                } else {
                    WebSocketConnectionState.Connecting
                }
                currentJob = session?.let { authenticated ->
                    launch { startWebSocket(authenticated.token) }
                }
            }
        }
        scope.launch {
            SharedFlowCentre.logout.collect {
                currentJob?.cancelAndJoin()
                currentJob = null
                _connectionState.value = WebSocketConnectionState.Idle
            }
        }
    }

    private suspend fun startWebSocket(sessionToken: AccountSessionToken) {
        retryWebSocketConnection(onFailure = ::reportConnectionFailure) {
            val authResponse = apiClient.get(AUTH_API_PREFIX)
            check(authResponse.status == HttpStatusCode.OK) {
                "WebSocket auth failed with HTTP ${authResponse.status.value}"
            }
            val authData = authResponse.body<AuthData>()
            val token = authData.token.takeIf { authData.ok == true && !it.isNullOrBlank() }
                ?: error("WebSocket auth response did not contain a token")
            apiClient.ws(
                urlString = VRC_WSS_URL,
                request = {
                    parameter("auth", token)
                }) {
                _connectionState.value = WebSocketConnectionState.Connected(nowMillis())
                while (true) {
                    val othersMessage = receiveDeserialized<WebSocketEvent>()
                    SharedFlowCentre.emitWebSocket(
                        AccountWebSocketEvent(sessionToken, othersMessage)
                    )
                }
            }
        }
    }

    private suspend fun reportConnectionFailure(error: Exception, consecutiveFailures: Int) {
        val previousState = _connectionState.value
        val connectedDuration = (previousState as? WebSocketConnectionState.Connected)
            ?.let { (nowMillis() - it.connectedAtEpochMillis).coerceAtLeast(0L) }
            ?: (previousState as? WebSocketConnectionState.Disconnected)?.connectedDurationMillis
        _connectionState.value = WebSocketConnectionState.Disconnected(
            connectedDurationMillis = connectedDuration,
            consecutiveFailures = consecutiveFailures,
        )
        val errorType = error::class.simpleName ?: "Exception"
        logger.error(
            "WebSocket reconnect attempt $consecutiveFailures failed ($errorType): ${error.message.orEmpty()}"
        )
        if (consecutiveFailures == 1 || consecutiveFailures % USER_NOTICE_INTERVAL == 0) {
            SharedFlowCentre.toastText.emit(
                ToastText.Error(
                    "好友实时连接失败，正在重试: ${error.message.orEmpty()}"
                )
            )
        }
    }

    private companion object {
        const val USER_NOTICE_INTERVAL = 12
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

internal suspend fun retryWebSocketConnection(
    retryDelayMillis: Long = 5_000L,
    onFailure: suspend (Exception, consecutiveFailures: Int) -> Unit,
    connect: suspend () -> Unit,
) {
    var consecutiveFailures = 0
    while (currentCoroutineContext().isActive) {
        try {
            connect()
            consecutiveFailures = 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            consecutiveFailures++
            onFailure(e, consecutiveFailures)
        }
        delay(retryDelayMillis)
    }
}

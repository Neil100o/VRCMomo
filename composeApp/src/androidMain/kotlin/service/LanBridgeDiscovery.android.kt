package io.github.vrcmteam.vrcm.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

private const val DISCOVERY_PORT = 38672
private const val DISCOVERY_TIMEOUT_MILLIS = 2_500
private val discoveryJson = Json { ignoreUnknownKeys = true }

internal actual suspend fun discoverLanBridges(): List<LanBridgeCandidate> = withContext(Dispatchers.IO) {
    val deadline = System.currentTimeMillis() + DISCOVERY_TIMEOUT_MILLIS
    val candidates = linkedSetOf<LanBridgeCandidate>()
    DatagramSocket().use { socket ->
        socket.broadcast = true
        socket.soTimeout = 350
        val request = LAN_BRIDGE_DISCOVERY_REQUEST.encodeToByteArray()
        socket.send(DatagramPacket(request, request.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT))
        while (System.currentTimeMillis() < deadline) {
            val buffer = ByteArray(1024)
            val reply = DatagramPacket(buffer, buffer.size)
            runCatching { socket.receive(reply) }.getOrNull() ?: continue
            val payload = reply.data.decodeToString(0, reply.length)
            val response = runCatching { discoveryJson.decodeFromString<LanBridgeDiscoveryResponse>(payload) }.getOrNull()
                ?: continue
            if (response.service != "vrcmomo-lan-bridge" || response.protocol != 1 || response.port !in 1..65535) continue
            candidates += LanBridgeCandidate("http://${reply.address.hostAddress}:${response.port}")
        }
    }
    candidates.toList()
}

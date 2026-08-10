package io.github.vrcmteam.vrcm.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

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
        discoveryTargets().forEach { target ->
            socket.send(DatagramPacket(request, request.size, target, DISCOVERY_PORT))
        }
        while (System.currentTimeMillis() < deadline) {
            val buffer = ByteArray(1024)
            val reply = DatagramPacket(buffer, buffer.size)
            runCatching { socket.receive(reply) }.getOrNull() ?: continue
            val payload = reply.data.decodeToString(0, reply.length)
            val response = runCatching { discoveryJson.decodeFromString<LanBridgeDiscoveryResponse>(payload) }.getOrNull()
                ?: continue
            if (response.service != "vrcmomo-lan-bridge" || response.protocol != 1 || response.port !in 1..65535) continue
            candidates += LanBridgeCandidate(
                baseUrl = "http://${reply.address.hostAddress}:${response.port}",
                pairingUrl = response.pairingUrl,
            )
        }
    }
    candidates.toList()
}


private fun discoveryTargets(): Set<InetAddress> = buildSet {
    // Some access points drop the global broadcast but accept a subnet-specific one.
    add(InetAddress.getByName("255.255.255.255"))
    val interfaces = runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
    interfaces
        .filter { network -> runCatching { network.isUp && !network.isLoopback }.getOrDefault(false) }
        .flatMap { network -> network.interfaceAddresses }
        .mapNotNull { address -> address.broadcast }
        .forEach(::add)
}

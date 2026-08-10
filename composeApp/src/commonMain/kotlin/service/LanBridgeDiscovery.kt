package io.github.vrcmteam.vrcm.service

import kotlinx.serialization.Serializable

internal const val LAN_BRIDGE_DISCOVERY_REQUEST = "VRCMOMO-LAN-DISCOVERY-V1"

@Serializable
internal data class LanBridgeDiscoveryResponse(
    val service: String,
    val protocol: Int,
    val port: Int,
    val pairingUrl: String? = null,
)

internal data class LanBridgeCandidate(
    val baseUrl: String,
    val pairingUrl: String? = null,
)

/** Finds local bridges and, when supported, returns their short-lived pairing link. */
internal expect suspend fun discoverLanBridges(): List<LanBridgeCandidate>

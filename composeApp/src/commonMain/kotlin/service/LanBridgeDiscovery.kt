package io.github.vrcmteam.vrcm.service

import kotlinx.serialization.Serializable

internal const val LAN_BRIDGE_DISCOVERY_REQUEST = "VRCMOMO-LAN-DISCOVERY-V1"

@Serializable
internal data class LanBridgeDiscoveryResponse(
    val service: String,
    val protocol: Int,
    val port: Int,
)

internal data class LanBridgeCandidate(
    val baseUrl: String,
)

/** Finds bridge addresses only. Pairing tokens are intentionally never broadcast over the LAN. */
internal expect suspend fun discoverLanBridges(): List<LanBridgeCandidate>

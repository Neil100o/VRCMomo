package io.github.vrcmteam.vrcm.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.*

/** Pairing details for the optional PC-hosted, LAN-only VRCX activity bridge. */
internal data class LanBridgePairing(
    val baseUrl: String,
    val token: String,
) {
    companion object {
        fun fromInput(rawUrl: String, fallbackToken: String): LanBridgePairing {
            val url = Url(rawUrl.trim())
            require(url.protocol.name in setOf("http", "https")) { "Bridge URL must use HTTP or HTTPS" }
            val portPart = when {
                url.port == url.protocol.defaultPort -> ""
                else -> ":${url.port}"
            }
            return LanBridgePairing(
                baseUrl = "${url.protocol.name}://${url.host}$portPart",
                token = url.parameters["token"]?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: fallbackToken.trim(),
            ).also { require(it.token.isNotBlank()) { "Pairing token is required" } }
        }
    }
}

/** Reads the existing VRCX export format from the user's paired desktop bridge. */
internal class LanActivityBridgeClient(
    private val client: HttpClient,
) {
    suspend fun uploadVrcmomoActivity(pairing: LanBridgePairing, payload: String) {
        val response = client.post("${pairing.baseUrl}/v1/vrcmomo-activity") {
            header("X-VRCMomo-Bridge-Token", pairing.token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(payload)
        }
        val text = response.body<String>()
        check(response.status.isSuccess()) { "Bridge upload failed (${response.status.value}): ${text.take(160)}" }
    }

    suspend fun fetchVrcxActivity(pairing: LanBridgePairing): String {
        val response = client.get("${pairing.baseUrl}/v1/vrcx-activity") {
            header("X-VRCMomo-Bridge-Token", pairing.token)
        }
        val text = response.body<String>()
        check(response.status.isSuccess()) { "Bridge request failed (${response.status.value}): ${text.take(160)}" }
        return text
    }
}

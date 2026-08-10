package io.github.vrcmteam.vrcm.service

import kotlin.test.Test
import kotlin.test.assertEquals

class LanActivityBridgeClientTest {
    @Test
    fun `pairing URL supplies base address and token`() {
        val pairing = LanBridgePairing.fromInput(
            rawUrl = "http://192.168.1.8:38671/v1/health?token=from-url",
            fallbackToken = "ignored",
        )

        assertEquals("http://192.168.1.8:38671", pairing.baseUrl)
        assertEquals("from-url", pairing.token)
    }

    @Test
    fun `base address uses manually entered token`() {
        val pairing = LanBridgePairing.fromInput(
            rawUrl = "http://192.168.1.8:38671",
            fallbackToken = "manual-token",
        )

        assertEquals("http://192.168.1.8:38671", pairing.baseUrl)
        assertEquals("manual-token", pairing.token)
    }
}

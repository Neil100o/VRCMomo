package io.github.vrcmteam.vrcm.network.momocall

import io.github.vrcmteam.vrcm.network.momocall.data.MOMO_CALL_PROTOCOL_VERSION
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSignal
import io.github.vrcmteam.vrcm.network.momocall.data.MomoCallSignalType
import io.github.vrcmteam.vrcm.network.momocall.data.isValid
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MomoCallSignalTest {
    @Test
    fun `invite needs call and sender ids`() {
        assertTrue(
            MomoCallSignal(
                version = MOMO_CALL_PROTOCOL_VERSION,
                type = MomoCallSignalType.Invite,
                callId = "call-1",
                fromUserId = "usr_sender",
            ).isValid(),
        )
        assertFalse(MomoCallSignal(type = MomoCallSignalType.Invite, callId = "call-1").isValid())
    }

    @Test
    fun `register needs user and device ids`() {
        assertTrue(
            MomoCallSignal(
                type = MomoCallSignalType.Register,
                userId = "usr_owner",
                deviceId = "device_phone",
            ).isValid(),
        )
        assertFalse(MomoCallSignal(type = MomoCallSignalType.Register, userId = "usr_owner").isValid())
    }
}

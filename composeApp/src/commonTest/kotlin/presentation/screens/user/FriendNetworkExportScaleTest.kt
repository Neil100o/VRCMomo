package io.github.vrcmteam.vrcm.presentation.screens.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FriendNetworkExportScaleTest {

    @Test
    fun `keeps ordinary exports at original resolution`() {
        assertEquals(1f, friendNetworkExportScale(1_920f, 1_080f))
    }

    @Test
    fun `caps a very large layout by side and pixel budget`() {
        val scale = friendNetworkExportScale(10_000f, 8_000f)

        assertTrue(scale in 0f..1f)
        assertTrue(10_000f * scale <= 2_048f)
        assertTrue(10_000f * 8_000f * scale * scale <= 4_000_000f)
    }

    @Test
    fun `handles empty dimensions without an invalid scale`() {
        assertEquals(1f, friendNetworkExportScale(0f, 100f))
    }
}

package io.github.vrcmteam.vrcm.presentation.compoments

import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveCardLayoutTest {

    @Test
    fun mediaCardsUseMoreColumnsOnWideScreens() {
        assertEquals(2, mediaCardColumnCount(360f))
        assertEquals(2, mediaCardColumnCount(839f))
        assertEquals(3, mediaCardColumnCount(840f))
        assertEquals(4, mediaCardColumnCount(1080f))
    }

    @Test
    fun friendCardsRemainReadableAcrossWidthClasses() {
        assertEquals(1, friendCardColumnCount(360f))
        assertEquals(2, friendCardColumnCount(520f))
        assertEquals(3, friendCardColumnCount(840f))
        assertEquals(4, friendCardColumnCount(1080f))
    }
}

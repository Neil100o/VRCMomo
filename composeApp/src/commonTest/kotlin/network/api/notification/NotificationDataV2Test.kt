package io.github.vrcmteam.vrcm.network.api.notification

import io.github.vrcmteam.vrcm.network.api.notification.data.NotificationDataV2
import io.github.vrcmteam.vrcm.presentation.screens.home.data.NotificationItemData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationDataV2Test {
    @Test
    fun boopDetailsExposeTheSelectedBuiltInEmoji() {
        val notification = notification(
            details = """{"emojiId":"default_heart","emojiVersion":1}""",
        )

        assertEquals("default_heart", notification.boopDetail()?.emojiId)
        assertEquals(1, notification.boopDetail()?.emojiVersion)
        assertEquals("Heart", NotificationItemData(notification).boopEmojiLabel)
    }

    @Test
    fun inventoryOnlyBoopsAreShownAsCustomEmoji() {
        val notification = notification(
            details = """{"inventoryItemId":"inv_custom"}""",
        )

        assertEquals("inv_custom", notification.boopDetail()?.inventoryItemId)
        assertEquals("Custom emoji", NotificationItemData(notification).boopEmojiLabel)
    }

    @Test
    fun invalidOrNonBoopDetailsAreIgnored() {
        assertNull(notification(details = "not-json").boopDetail())
        assertNull(notification(details = "{}", type = "friendRequest").boopDetail())
    }

    private fun notification(
        details: String,
        type: String = "boop",
    ) = NotificationDataV2(
        createdAt = "2026-07-31T00:00:00Z",
        details = details,
        id = "not_1",
        message = "sent you a boop",
        seen = false,
        senderUserId = "usr_sender",
        receiverUserId = "usr_receiver",
        type = type,
    )
}

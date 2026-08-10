package io.github.vrcmteam.vrcm.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class VrcmomoActivitySyncTest {
    @Test
    fun `mobile sync export keeps the documented envelope format`() {
        val raw = VrcmomoActivitySyncEnvelope(
            ownerUserId = "usr_owner",
            exportedAtMillis = 1_000L,
            statsByFriendId = emptyMap(),
            activityEvents = emptyList(),
        ).encode()

        val document = Json.parseToJsonElement(raw).jsonObject
        assertEquals(VRCMOMO_ACTIVITY_SYNC_FORMAT_V1, document["format"]?.jsonPrimitive?.content)
        assertEquals("usr_owner", document["ownerUserId"]?.jsonPrimitive?.content)
    }
}

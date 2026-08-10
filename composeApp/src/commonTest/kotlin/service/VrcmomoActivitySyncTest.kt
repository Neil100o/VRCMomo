package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityEventType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VrcmomoActivitySyncTest {
    @Test
    fun `mobile sync export carries a source device identity`() {
        val raw = VrcmomoActivitySyncEnvelope(
            ownerUserId = "usr_owner",
            exportedAtMillis = 1_000L,
            sourceDeviceId = "phone-a",
            statsByFriendId = emptyMap(),
            activityEvents = emptyList(),
        ).encode()

        val document = Json.parseToJsonElement(raw).jsonObject
        assertEquals(VRCMOMO_ACTIVITY_SYNC_FORMAT_V2, document["format"]?.jsonPrimitive?.content)
        assertEquals("phone-a", document["sourceDeviceId"]?.jsonPrimitive?.content)
    }

    @Test
    fun `archive keeps a shared event only once across retry and device snapshots`() {
        val event = FriendActivityEvent(
            userId = "usr_friend",
            displayName = "Friend",
            type = FriendActivityEventType.Online,
            occurredAtMillis = 2_000L,
            currentValue = "wrld_test:1",
        )
        val raw = Json.encodeToString(
            VrcmomoActivitySyncArchive(
                documents = listOf(
                    VrcmomoActivitySyncEnvelope("vrcmomo-activity-sync-v2", "usr_owner", 3_000L, "phone-a", emptyMap(), listOf(event)),
                    VrcmomoActivitySyncEnvelope("vrcmomo-activity-sync-v2", "usr_owner", 4_000L, "desktop-b", emptyMap(), listOf(event)),
                ),
            ),
        )

        val first = VrcmomoActivityImporter.preview(raw, emptySet())
        assertEquals(1, first.acceptedEvents)
        assertEquals(1, first.alreadyKnownEvents)
        assertTrue(first.acceptedEventKeys.isNotEmpty())

        val retry = VrcmomoActivityImporter.preview(raw, first.acceptedEventKeys)
        assertEquals(0, retry.acceptedEvents)
        assertEquals(2, retry.alreadyKnownEvents)
    }

    @Test
    fun `legacy v1 document remains readable`() {
        val raw = VrcmomoActivitySyncEnvelope(
            format = VRCMOMO_ACTIVITY_SYNC_FORMAT_V1,
            ownerUserId = "usr_owner",
            exportedAtMillis = 1_000L,
            statsByFriendId = emptyMap(),
            activityEvents = emptyList(),
        ).encode()

        assertEquals(0, VrcmomoActivityImporter.preview(raw, emptySet()).acceptedEvents)
    }
}

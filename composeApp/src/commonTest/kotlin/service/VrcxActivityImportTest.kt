package io.github.vrcmteam.vrcm.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VrcxActivityImportTest {
    private val userId = "usr_12345678-1234-1234-1234-123456789abc"

    @Test
    fun previewsPresenceAndCompletedMeeting() {
        val preview = VrcxActivityImporter.preview(
            """
            {
              "format":"vrcmomo-vrcx-activity-v1",
              "events": {
                "presence":[
                  {"created_at":"2026-01-01T00:00:00Z","user_id":"$userId","type":"Online"},
                  {"created_at":"2026-01-01T01:00:00Z","user_id":"$userId","type":"Offline"}
                ],
                "instanceJoinLeave":[
                  {"created_at":"2026-01-01T00:30:00Z","user_id":"$userId","type":"OnPlayerLeft","location":"wrld_x:1","time":120000}
                ]
              }
            }
            """.trimIndent(),
            emptySet(),
        )

        val stats = preview.result.updates.getValue(userId)
        assertEquals(2, preview.presenceEvents)
        assertEquals(1, preview.completedMeetings)
        assertEquals(1, stats.meetingCount)
        assertEquals(120000, stats.togetherDurationMillis)
        assertTrue(stats.lastOnlineAtMillis != null)
        assertTrue(stats.lastOfflineAtMillis != null)
    }

    @Test
    fun skipsPreviouslyImportedEvents() {
        val raw = """{"format":"vrcmomo-vrcx-activity-v1","events":{"presence":[{"created_at":"2026-01-01T00:00:00Z","user_id":"$userId","type":"Online"}]}}"""
        val first = VrcxActivityImporter.preview(raw, emptySet())
        val repeat = VrcxActivityImporter.preview(raw, first.result.acceptedEventKeys)

        assertEquals(1, repeat.alreadyImportedEvents)
        assertTrue(repeat.result.updates.isEmpty())
    }
}

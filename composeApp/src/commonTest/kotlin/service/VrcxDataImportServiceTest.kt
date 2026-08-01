package io.github.vrcmteam.vrcm.service

import kotlin.test.Test
import kotlin.test.assertEquals

class VrcxDataImportServiceTest {
    @Test
    fun parsesOfficialVrcxFriendJsonExport() {
        val result = parseVrcxFriendIds(
            """{ "friends": ["usr_12345678-1234-1234-1234-123456789abc", "usr_12345678-1234-1234-1234-123456789abc"] }""",
        )

        assertEquals(listOf("usr_12345678-1234-1234-1234-123456789abc"), result)
    }

    @Test
    fun parsesUserIdCsvExport() {
        val result = parseVrcxFriendIds(
            "UserID,Name\nusr_aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee,Example",
        )

        assertEquals(listOf("usr_aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), result)
    }
}

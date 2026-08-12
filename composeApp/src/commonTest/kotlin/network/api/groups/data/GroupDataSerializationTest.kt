package io.github.vrcmteam.vrcm.network.api.groups.data

import io.github.vrcmteam.vrcm.presentation.screens.group.data.GroupProfileVo
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupDataSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun partialNonMemberGroupResponseCanBeDecoded() {
        val group = json.decodeFromString<GroupData>(
            """{
                "id":"grp_test",
                "name":"Test Group",
                "joinState":"open",
                "memberCount":35,
                "myMember":{
                    "groupId":"grp_test",
                    "userId":"usr_test",
                    "membershipStatus":"member"
                },
                "galleries":[{"id":"gal_test","name":"Gallery"}]
            }"""
        )

        assertEquals("Test Group", group.name)
        assertEquals("member", group.myMember?.membershipStatus)
        assertEquals("Gallery", group.galleries.single().name)
        assertFalse(group.myMember?.isRepresenting ?: true)
    }

    @Test
    fun limitedSearchResultPreservesVisibleProfileData() {
        val profile = GroupProfileVo(
            LimitedGroup(
                id = "grp_test",
                name = "Search Result",
                description = "Visible before profile refresh",
                iconUrl = "https://example.test/icon.png",
                memberCount = 71,
                membershipStatus = "inactive",
            )
        )

        assertEquals("Search Result", profile.name)
        assertEquals(71, profile.memberCount)
        assertTrue(profile.iconUrl.orEmpty().contains("icon.png"))
    }
}

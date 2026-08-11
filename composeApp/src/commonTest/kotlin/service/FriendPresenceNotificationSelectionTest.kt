package io.github.vrcmteam.vrcm.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FriendPresenceNotificationSelectionTest {
    @Test
    fun emptySelectionDoesNotNotifyAnyone() {
        val selection = FriendPresenceNotificationSelection()

        assertFalse(selection.allows("usr_friend", setOf("grp_favorite")))
        assertTrue(selection.selectedUserIds(mapOf("usr_friend" to setOf("grp_favorite"))).isEmpty())
    }

    @Test
    fun enabledFavoriteGroupSelectsItsMembers() {
        val selection = FriendPresenceNotificationSelection(groupIds = setOf("grp_close"))

        assertEquals(
            setOf("usr_close"),
            selection.selectedUserIds(
                mapOf(
                    "usr_close" to setOf("grp_close"),
                    "usr_other" to setOf("grp_other"),
                )
            ),
        )
    }

    @Test
    fun individualChoiceOverridesFavoriteGroups() {
        val selection = FriendPresenceNotificationSelection(
            groupIds = setOf("grp_close"),
            userOverrides = mapOf(
                "usr_muted" to false,
                "usr_extra" to true,
            ),
        )

        assertEquals(
            setOf("usr_normal", "usr_extra"),
            selection.selectedUserIds(
                mapOf(
                    "usr_normal" to setOf("grp_close"),
                    "usr_muted" to setOf("grp_close"),
                )
            ),
        )
    }
}

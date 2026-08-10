package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.attributes.UserStatus
import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FriendGamePresenceTest {
    @Test
    fun `website activity is not game presence`() {
        assertTrue(!isInGameLocation("offline"))
        assertTrue(!isInGameLocation("web"))
        assertTrue(isInGameLocation("private"))
        assertTrue(isInGameLocation("traveling"))
        assertTrue(isInGameLocation("wrld_home:instance"))
    }

    @Test
    fun `favorites loaded before friends establish a baseline without startup notifications`() {
        val tracker = FavoriteFriendPresenceTracker()
        tracker.updateFavorites(favoriteIds = setOf("usr_friend"), friends = emptyMap())

        assertTrue(
            tracker.observe(
                mapOf("usr_friend" to friend(location = "wrld_home:instance", status = UserStatus.Active))
            ).isEmpty()
        )
        assertEquals(
            listOf(FriendPresenceTransition("usr_friend", "Friend", false)),
            tracker.observe(mapOf("usr_friend" to friend(location = "offline", status = UserStatus.Active))),
        )
    }

    @Test
    fun `live baseline replaces restored offline cache without a false online transition`() {
        val tracker = FavoriteFriendPresenceTracker()
        tracker.updateFavorites(
            favoriteIds = setOf("usr_friend"),
            friends = mapOf("usr_friend" to friend(location = "offline", status = UserStatus.Offline)),
        )

        tracker.establishLiveBaseline(
            mapOf("usr_friend" to friend(location = "wrld_home:instance", status = UserStatus.Active)),
        )
        assertTrue(tracker.observe(mapOf("usr_friend" to friend(location = "wrld_home:instance", status = UserStatus.Active))).isEmpty())
        assertEquals(
            listOf(FriendPresenceTransition("usr_friend", "Friend", false)),
            tracker.observe(mapOf("usr_friend" to friend(location = "offline", status = UserStatus.Active))),
        )
    }

    @Test
    fun `favorite tracker reports game transitions across website activity`() {
        val tracker = FavoriteFriendPresenceTracker()
        tracker.updateFavorites(
            favoriteIds = setOf("usr_friend"),
            friends = mapOf("usr_friend" to friend(location = "offline", status = UserStatus.Offline)),
        )

        assertEquals(
            listOf(FriendPresenceTransition("usr_friend", "Friend", true)),
            tracker.observe(mapOf("usr_friend" to friend(location = "wrld_home:instance", status = UserStatus.Active))),
        )
        assertEquals(
            listOf(FriendPresenceTransition("usr_friend", "Friend", false)),
            tracker.observe(mapOf("usr_friend" to friend(location = "offline", status = UserStatus.Active))),
        )
        assertTrue(tracker.observe(mapOf("usr_friend" to friend(location = "offline", status = UserStatus.Active))).isEmpty())
    }

    private fun friend(location: String, status: UserStatus) = FriendData(
        bio = null,
        currentAvatarImageUrl = "",
        currentAvatarThumbnailImageUrl = null,
        developerType = "none",
        displayName = "Friend",
        friendKey = "friend-key",
        id = "usr_friend",
        imageUrl = "",
        isFriend = true,
        lastLogin = "",
        lastPlatform = "standalonewindows",
        location = location,
        profilePicOverride = "",
        status = status,
        statusDescription = "",
        userIcon = "",
        pronouns = null,
    )
}

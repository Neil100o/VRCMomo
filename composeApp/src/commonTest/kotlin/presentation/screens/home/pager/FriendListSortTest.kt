package io.github.vrcmteam.vrcm.presentation.screens.home.pager

import io.github.vrcmteam.vrcm.network.api.attributes.LocationType
import io.github.vrcmteam.vrcm.network.api.attributes.UserStatus
import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlin.test.Test
import kotlin.test.assertEquals

class FriendListSortTest {
    @Test
    fun offlineFriendsFallBackToLastLoginWhenLastActivityIsMissing() {
        val older = friend(
            id = "usr_older",
            lastActivity = "",
            lastLogin = "2026-07-20T08:00:00Z",
        )
        val newer = friend(
            id = "usr_newer",
            lastActivity = "",
            lastLogin = "2026-07-29T08:00:00Z",
        )

        val sorted = listOf(older, newer).sortedUserByStatus()

        assertEquals(listOf("usr_newer", "usr_older"), sorted.map(FriendData::id))
    }

    @Test
    fun frequentContactsUseLocalActivityInsteadOfOnlineStateAlone() {
        val now = 1_000_000_000L
        val closeOfflineFriend = friend("usr_close", "", "2026-07-20T08:00:00Z")
        val unknownOnlineFriend = friend("usr_online", "", "2026-07-29T08:00:00Z").copy(
            status = UserStatus.Active,
            location = "wrld_example:1",
        )
        val activity = mapOf(
            closeOfflineFriend.id to FriendActivityStats(
                userId = closeOfflineFriend.id,
                lastSeenTogetherAtMillis = now - 60_000L,
                lastActivityAtMillis = now - 60_000L,
                meetingCount = 12,
                togetherDurationMillis = 8 * 3_600_000L,
            ),
        )

        val sorted = listOf(unknownOnlineFriend, closeOfflineFriend).sortedByFriendMode(
            mode = FriendSortMode.Frequent,
            activityByFriendId = activity,
            nowMillis = now,
        )

        assertEquals(listOf("usr_close", "usr_online"), sorted.map(FriendData::id))
    }

    @Test
    fun recentMeetingModeUsesLastSharedSession() {
        val older = friend("usr_older", "", "2026-07-29T08:00:00Z")
        val newer = friend("usr_newer", "", "2026-07-20T08:00:00Z")
        val activity = mapOf(
            older.id to FriendActivityStats(userId = older.id, lastSeenTogetherAtMillis = 10L),
            newer.id to FriendActivityStats(userId = newer.id, lastSeenTogetherAtMillis = 20L),
        )

        val sorted = listOf(older, newer).sortedByFriendMode(
            mode = FriendSortMode.RecentMet,
            activityByFriendId = activity,
            nowMillis = 100L,
        )

        assertEquals(listOf("usr_newer", "usr_older"), sorted.map(FriendData::id))
    }

    private fun friend(id: String, lastActivity: String, lastLogin: String) = FriendData(
        bio = null,
        currentAvatarImageUrl = "",
        currentAvatarThumbnailImageUrl = "",
        developerType = "none",
        displayName = id,
        friendKey = "",
        id = id,
        imageUrl = "",
        isFriend = true,
        lastActivity = lastActivity,
        lastLogin = lastLogin,
        lastPlatform = "standalonewindows",
        location = LocationType.Offline.value,
        profilePicOverride = "",
        status = UserStatus.Offline,
        statusDescription = "",
        userIcon = "",
        pronouns = null,
    )
}

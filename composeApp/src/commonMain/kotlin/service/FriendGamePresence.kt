package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.network.api.attributes.LocationType
import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData

/**
 * VRChat reports website-only activity as location "offline" with a non-offline user status.
 * VRCMomo deliberately treats that as out of game: only a private/traveling/world location means
 * the friend is currently inside VRChat.
 */
internal fun isInGameLocation(location: String?): Boolean {
    val normalized = location?.trim().orEmpty()
    return when {
        normalized.isEmpty() -> false
        normalized.equals(LocationType.Offline.value, ignoreCase = true) -> false
        normalized.equals(LocationType.Web.value, ignoreCase = true) -> false
        normalized.equals(LocationType.Private.value, ignoreCase = true) -> true
        normalized.equals(LocationType.Traveling.value, ignoreCase = true) -> true
        normalized.startsWith(LocationType.Instance.value, ignoreCase = true) -> true
        else -> false
    }
}

internal fun FriendData.isInGamePresence(): Boolean = isInGameLocation(location)

internal data class FriendPresenceTransition(
    val userId: String,
    val displayName: String,
    val inGame: Boolean,
)

/**
 * Keeps a baseline for favorite friends and emits only real in-game presence transitions.
 * Favorites and friend snapshots may arrive in either order, so every favorites refresh seeds the
 * missing baseline from the latest complete friend map instead of depending on one WebSocket event.
 */
internal class FavoriteFriendPresenceTracker {
    private var favoriteFriendIds: Set<String> = emptySet()
    private val knownPresence = mutableMapOf<String, Boolean>()
    private val knownDisplayNames = mutableMapOf<String, String>()

    fun reset() {
        favoriteFriendIds = emptySet()
        knownPresence.clear()
        knownDisplayNames.clear()
    }

    /**
     * Replaces any cache-derived baseline with the first live server snapshot.
     * This intentionally emits no transition: reopening the app must not look like every
     * favorite left and re-entered VRChat.
     */
    fun establishLiveBaseline(friends: Map<String, FriendData>) {
        knownPresence.clear()
        knownDisplayNames.clear()
        favoriteFriendIds.forEach { userId ->
            val friend = friends[userId] ?: return@forEach
            knownPresence[userId] = friend.isInGamePresence()
            friend.displayName.takeIf(String::isNotBlank)?.let { knownDisplayNames[userId] = it }
        }
    }

    fun updateFavorites(
        favoriteIds: Set<String>,
        friends: Map<String, FriendData>,
    ) {
        favoriteFriendIds = favoriteIds
        knownPresence.keys.retainAll(favoriteIds)
        knownDisplayNames.keys.retainAll(favoriteIds)
        favoriteIds.forEach { userId ->
            val friend = friends[userId] ?: return@forEach
            knownPresence.putIfAbsent(userId, friend.isInGamePresence())
            friend.displayName.takeIf(String::isNotBlank)?.let { knownDisplayNames[userId] = it }
        }
    }

    fun observe(friends: Map<String, FriendData>): List<FriendPresenceTransition> = buildList {
        favoriteFriendIds.forEach { userId ->
            val friend = friends[userId]
            if (friend == null) {
                // A missing friend is meaningful only after we have seen a real baseline. During
                // startup, favorites often arrive before the friend snapshot; treating that as
                // offline would later generate a false "entered VRChat" notification.
                if (knownPresence[userId] == true) {
                    knownPresence[userId] = false
                    add(
                        FriendPresenceTransition(
                            userId = userId,
                            displayName = knownDisplayNames[userId] ?: userId,
                            inGame = false,
                        )
                    )
                }
                return@forEach
            }

            val inGame = friend.isInGamePresence()
            val displayName = friend.displayName
                .takeIf(String::isNotBlank)
                ?: knownDisplayNames[userId]
                ?: userId
            val previous = knownPresence.put(userId, inGame)
            knownDisplayNames[userId] = displayName
            if (previous != null && previous != inGame) {
                add(FriendPresenceTransition(userId, displayName, inGame))
            }
        }
    }
}

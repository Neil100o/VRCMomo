package io.github.vrcmteam.vrcm.service

import kotlinx.serialization.Serializable

/**
 * 好友上下线通知对象。
 *
 * 收藏组提供批量选择，单人设置优先于收藏组：true 强制开启，false 强制关闭，缺省则跟随收藏组。
 */
@Serializable
data class FriendPresenceNotificationSelection(
    val groupIds: Set<String> = emptySet(),
    val userOverrides: Map<String, Boolean> = emptyMap(),
) {
    fun allows(userId: String, userGroupIds: Set<String>): Boolean =
        userOverrides[userId] ?: userGroupIds.any(groupIds::contains)

    fun selectedUserIds(groupIdsByUser: Map<String, Set<String>>): Set<String> = buildSet {
        groupIdsByUser.forEach { (userId, userGroupIds) ->
            if (allows(userId, userGroupIds)) add(userId)
        }
        userOverrides.forEach { (userId, enabled) ->
            if (enabled) add(userId) else remove(userId)
        }
    }
}

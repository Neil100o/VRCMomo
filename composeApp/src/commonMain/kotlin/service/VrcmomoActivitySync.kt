package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.storage.data.FriendActivityEvent
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val VRCMOMO_ACTIVITY_SYNC_FORMAT_V1 = "vrcmomo-activity-sync-v1"

/**
 * Portable, credential-free export of the phone's observed friend activity.
 * The PC bridge stores this document separately and never injects it into VRCX's SQLite tables.
 */
@Serializable
internal data class VrcmomoActivitySyncEnvelope(
    val format: String = VRCMOMO_ACTIVITY_SYNC_FORMAT_V1,
    val ownerUserId: String,
    val exportedAtMillis: Long,
    val statsByFriendId: Map<String, FriendActivityStats>,
    val activityEvents: List<FriendActivityEvent>,
)

internal fun VrcmomoActivitySyncEnvelope.encode(): String = syncJson.encodeToString(this)

private val syncJson = Json {
    encodeDefaults = true
    explicitNulls = false
}

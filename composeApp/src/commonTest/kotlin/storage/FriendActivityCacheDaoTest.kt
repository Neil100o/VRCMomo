package io.github.vrcmteam.vrcm.storage

import com.russhwolf.settings.MapSettings
import io.github.vrcmteam.vrcm.storage.data.FriendActivityCache
import io.github.vrcmteam.vrcm.storage.data.FriendActivityStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FriendActivityCacheDaoTest {
    @Test
    fun legacyActivityRecordMigratesWithoutLosingStats() {
        val settings = MapSettings()
        val userId = "usr_momo"
        settings.putString(
            "${DaoKeys.FriendActivity.KEY_PREFIX}.$userId",
            """{"statsByFriendId":{"usr_friend":{"userId":"usr_friend","meetingCount":7,"togetherDurationMillis":12345}}}""",
        )
        val dao = FriendActivityCacheDao(settings)

        val loaded = assertNotNull(dao.load(userId))

        assertEquals(FriendActivityCache.CURRENT_SCHEMA_VERSION, loaded.schemaVersion)
        assertEquals(7, loaded.statsByFriendId.getValue("usr_friend").meetingCount)
        assertEquals(12345, loaded.statsByFriendId.getValue("usr_friend").togetherDurationMillis)

        dao.save(userId, loaded)
        assertTrue(settings.getString("${DaoKeys.FriendActivity.KEY_PREFIX}.$userId", "").contains("\"schemaVersion\":2"))
    }

    @Test
    fun newerSchemaIsNeverOverwrittenByThisBuild() {
        val settings = MapSettings()
        val userId = "usr_future"
        val key = "${DaoKeys.FriendActivity.KEY_PREFIX}.$userId"
        val futureRecord = """{"schemaVersion":99,"statsByFriendId":{}}"""
        settings.putString(key, futureRecord)
        val dao = FriendActivityCacheDao(settings)

        dao.save(userId, FriendActivityCache(statsByFriendId = mapOf(
            "usr_friend" to FriendActivityStats(userId = "usr_friend", meetingCount = 1),
        )))

        assertEquals(futureRecord, settings.getString(key, ""))
    }
}

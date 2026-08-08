package io.github.vrcmteam.vrcm.storage

import androidx.room.RoomDatabase
import io.github.vrcmteam.vrcm.AppPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal const val VRCMOMO_ACTIVITY_DATABASE_NAME = "vrcmomo-activity.db"

internal expect fun platformVrcmomoActivityDatabaseBuilder(
    appPlatform: AppPlatform,
): RoomDatabase.Builder<VrcmomoActivityDatabase>

internal fun buildVrcmomoActivityDatabase(
    builder: RoomDatabase.Builder<VrcmomoActivityDatabase>,
): VrcmomoActivityDatabase = builder
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

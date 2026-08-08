package io.github.vrcmteam.vrcm.storage

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.vrcmteam.vrcm.AppPlatform
import okio.Path.Companion.toPath

internal actual fun platformVrcmomoActivityDatabaseBuilder(
    appPlatform: AppPlatform,
): RoomDatabase.Builder<VrcmomoActivityDatabase> = Room.databaseBuilder<VrcmomoActivityDatabase>(
    name = (appPlatform.persistentDataDirectory.toPath() / "VRCMomo" / VRCMOMO_ACTIVITY_DATABASE_NAME).toString(),
).setDriver(BundledSQLiteDriver())

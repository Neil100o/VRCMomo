package io.github.vrcmteam.vrcm.storage

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.github.vrcmteam.vrcm.AndroidAppPlatform
import io.github.vrcmteam.vrcm.AppPlatform

internal actual fun platformVrcmomoActivityDatabaseBuilder(
    appPlatform: AppPlatform,
): RoomDatabase.Builder<VrcmomoActivityDatabase> {
    val context = (appPlatform as AndroidAppPlatform).context
    return Room.databaseBuilder(
        context = context,
        klass = VrcmomoActivityDatabase::class.java,
        name = context.getDatabasePath(VRCMOMO_ACTIVITY_DATABASE_NAME).absolutePath,
    ).setDriver(AndroidSQLiteDriver())
}

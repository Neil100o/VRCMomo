package io.github.vrcmteam.vrcm.storage

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.vrcmteam.vrcm.AppPlatform
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformVrcmomoActivityDatabaseBuilder(
    appPlatform: AppPlatform,
): RoomDatabase.Builder<VrcmomoActivityDatabase> {
    val root = NSSearchPathForDirectoriesInDomains(
        directory = NSApplicationSupportDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).first() as String
    val directory = "$root/VRCMomo"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = directory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return Room.databaseBuilder<VrcmomoActivityDatabase>(
        name = "$directory/$VRCMOMO_ACTIVITY_DATABASE_NAME",
    ).setDriver(BundledSQLiteDriver())
}

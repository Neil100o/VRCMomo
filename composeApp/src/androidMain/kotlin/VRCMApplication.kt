package io.github.vrcmteam.vrcm

import android.app.Application
import io.github.vrcmteam.vrcm.di.commonModules
import io.github.vrcmteam.vrcm.di.modules.platformModule
import io.github.vrcmteam.vrcm.storage.SettingsDao
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class VRCMApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidLogger()
            androidContext(this@VRCMApplication)
            modules(commonModules + platformModule)
        }
        val koin = koinApplication.koin
        if (koin.get<SettingsDao>().settings.isBackgroundFriendMonitoringEnabled) {
            // Do not request permission automatically. If Android revoked it, the service remains
            // stopped and the user can review the setting the next time they open the app.
            koin.get<AppPlatform>().setBackgroundFriendMonitoringEnabled(true)
        }
    }
}

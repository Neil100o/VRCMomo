package io.github.vrcmteam.vrcm

import platform.Foundation.NSHomeDirectory
import platform.UIKit.UIDevice

class IosAppPlatform : AppPlatform {
    override val name: String = UIDevice.currentDevice.systemName()
    override val version: String = UIDevice.currentDevice.systemVersion
    override val type: AppPlatformType = AppPlatformType.Ios
    override val persistentDataDirectory: String =
        "${NSHomeDirectory()}/Library/Application Support"
}

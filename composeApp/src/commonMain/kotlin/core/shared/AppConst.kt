package io.github.vrcmteam.vrcm.core.shared

object AppConst {
    const val APP_NAME = "VRCMomo"

    // Keep this in sync with gradle/libs.versions.toml until common BuildConfig generation is introduced.
    const val APP_VERSION = "0.3.12"

    const val APP_GITHUB_URL = "https://github.com/Neil100o/VRCMomo"

    const val APP_GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/Neil100o/VRCMomo/releases/latest"

    /** Update metadata published beside the APK so testers do not need a formal GitHub Release. */
    const val APP_TESTING_CHANNEL_URL =
        "https://raw.githubusercontent.com/Neil100o/VRCMomo/main/downloads/testing-channel.json"
}

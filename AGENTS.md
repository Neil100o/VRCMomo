# VRCMomo development notes

- Build entry: `gradlew.bat :composeApp:assembleDebug`
- Version: `gradle/libs.versions.toml` (`app-version`, `app-code`)
- Android APK naming: `composeApp/build.gradle.kts`, `VRCMomo-v<app-version>.apk`
- Avatar API: `composeApp/src/commonMain/kotlin/network/api/avatars/`
- Avatar editor UI: `composeApp/src/commonMain/kotlin/presentation/screens/avatar/`
- User Gallery API/UI: `network/api/files/` and `presentation/screens/gallery/`
- Friend activity persistence: `service/FriendActivityService.kt`, `storage/FriendActivityCacheDao.kt`
- Persistent data root: `AppPlatform.persistentDataDirectory`
- Tests: `gradlew.bat :composeApp:testDebugUnitTest`

The avatar editor only updates avatars owned by the signed-in account. VRChat does not expose a model-bound album endpoint in the API used by this app, so Gallery remains a user file gallery and is not presented as an avatar-specific album.

VRCM-origin code remains attributed to the VRCM team in README files. VRCMomo changes are additive fork work and should be described separately in commits and release notes.

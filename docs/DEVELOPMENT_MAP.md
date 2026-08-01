# VRCMomo development map

## Build and release
- `gradle/libs.versions.toml`: `app-version`, `app-code`
- `composeApp/src/androidMain/AndroidManifest.xml`: Android identity/icon entry
- `composeApp/build.gradle.kts`: APK output name and release packaging
- Debug output: `composeApp/build/outputs/apk/debug/VRCMomo-v<version>.apk`

## Avatar editing
- API: `composeApp/src/commonMain/kotlin/network/api/avatars/AvatarsApi.kt`
- Request model: `network/api/avatars/data/AvatarUpdateData.kt`
- Profile state: `presentation/screens/avatar/AvatarProfileScreenModel.kt`
- Editor dialog: `presentation/screens/avatar/AvatarProfileScreen.kt`
- Current editor accepts avatar name and image FileID/URL. Owner-only check is enforced before update.

## Gallery scope
- API: `network/api/files/FileApi.kt`
- Gallery UI: `presentation/screens/gallery/`
- `FileTagType.Gallery` is the user's Gallery file category. There is no avatar-specific album association in the currently supported VRChat API, so do not claim that Gallery files belong to a particular avatar.

## Friend activity and search
- Service: `service/FriendActivityService.kt`
- Tracker: `service/FriendActivityTracker.kt`
- File storage: `storage/FriendActivityCacheDao.kt`
- Search integration: `presentation/screens/home/pager/SearchListPagerModel.kt`

## Attribution
The original VRCM application structure and VRCM-authored features remain credited to the VRCM team. VRCMomo-specific UI, branding, friend activity persistence, notification/background work, and avatar editing are fork modifications.

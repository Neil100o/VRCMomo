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
- VRCX bridge: `tools/export_vrcx_activity.py` (read-only SQLite export) and `service/VrcxActivityImport.kt` (Android merge). Bridge v2 carries presence, locations, social status, BIO diffs, avatar changes, friendship history and completed shared sessions. Credentials, cookies, notes and moderation data are excluded.

## LAN VRCX sync
- PC bridge launcher: `tools/Start-VRCMomoLanBridge.bat`
- PC bridge server and UDP discovery responder: `tools/vrcmomo_lan_bridge.py`
- Optional PC dependency list: `tools/requirements-lan-bridge.txt`
- Shared pairing parser and HTTP client: `service/LanActivityBridgeClient.kt`
- Shared UDP discovery contract: `service/LanBridgeDiscovery.kt`
- Android subnet broadcast discovery: `src/androidMain/kotlin/service/LanBridgeDiscovery.android.kt`
- Android QR scanner: `src/androidMain/kotlin/service/LanBridgeQrScanner.android.kt`
- Settings UI and import confirmation: `presentation/screens/home/sheet/SettingsBottomSheet.kt`
- Persisted pair and last-sync state: `storage/SettingsDao.kt`, `storage/data/LanSyncStatus.kt`

Discovery returns a short-lived pairing URL from the locally running bridge. If exactly one bridge is found, Android pairs and pulls the import preview immediately; if multiple are found, each discovered bridge has its own connect action. QR scanning follows the same pairing path. Do not add credential or VRChat-cookie transfer to this flow.

When `isLanSyncAutoEnabled` is on, Android performs one pull-and-upload cycle after the foreground monitoring service receives an authenticated session. It does not run on a repeating timer; the implementation is in `src/androidMain/kotlin/service/FriendActivityForegroundService.kt`.

## Attribution
The original VRCM application structure and VRCM-authored features remain credited to the VRCM team. VRCMomo-specific UI, branding, friend activity persistence, notification/background work, and avatar editing are fork modifications.

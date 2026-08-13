# VRCMomo development map

> 当前 main 的功能回查、遗留分支结论和逐项路径见 `docs/FEATURE_AUDIT_AND_PATHS.md`。继续开发前先读该文件，避免把旧分支、旧 APK 或已经重放的提交误认为待合并内容。

## Build and release
- **Version source of truth:** `gradle/libs.versions.toml`: raise both `app-version` and monotonic `app-code`.
- **In-app version / updater comparison:** `composeApp/src/commonMain/kotlin/core/shared/AppConst.kt`: set `APP_VERSION` to exactly the same value as `app-version`. This is what Settings and the GitHub Release comparison use.
- **Before a tester package:** verify the built APK with `aapt dump badging`; its `versionName`, `versionCode` and application ID must match the two files above. Do not create a GitHub Release unless the maintainer explicitly asks to publish one.
- `composeApp/src/androidMain/AndroidManifest.xml`: Android identity/icon entry
- `composeApp/build.gradle.kts`: APK output name and release packaging
- Debug output: `composeApp/build/outputs/apk/debug/VRCMomo-v<version>.apk`
- User installation, fixed-signature release and old-test-track migration: `docs/INSTALL_AND_MIGRATION.md`
- User-visible release history: `CHANGELOG.md`; individual release notes: `docs/RELEASE_NOTES_<version>.md`
- GitHub Release workflow: `.github/workflows/Android_Build_Release.yml` (requires `contents: write` to create a Release and upload assets)

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

## LAN timeline aggregation baseline

- **Protocol / event fingerprints:** `composeApp/src/commonMain/kotlin/service/VrcmomoActivitySync.kt`
- **Durable per-account dedupe keys:** `composeApp/src/commonMain/kotlin/storage/data/FriendActivityCache.kt` and `storage/FriendActivityCacheDao.kt`
- **Phone orchestration:** `service/FriendActivityService.kt`, then Android process-launch sync in `src/androidMain/kotlin/VRCMApplication.kt`
- **Desktop archive endpoint:** `tools/vrcmomo_lan_bridge.py` (`GET/POST /v1/vrcmomo-activity`)

The bridge retains phone timelines and rebuilds one canonical document per VRChat account. Exact
events and adjacent same-transition observations within 120 seconds are folded, status/location
values are normalized, and `Met -> Left` episodes rebuild a conservative meeting baseline. V1
phone envelopes remain readable; new exports are V2 and include an installation ID.

Complete `FriendActivityStats` snapshots are merged by maximum counters and latest timestamps,
never summed. Event-derived meeting count/duration may raise that baseline when the timeline proves
a larger value. The same archive can therefore be imported repeatedly without inflating totals.

- Desktop merge core: `tools/vrcmomo_activity_merge.py`
- Merge tests: `tools/test_vrcmomo_activity_merge.py`
- Rebuilt audit copy: `<bridge folder>/vrcmomo-lan-inbox/archive-rebuilt.json`
## LAN VRCX sync
- PC bridge launcher: `tools/Start-VRCMomoLanBridge.bat`
- PC bridge server and UDP discovery responder: `tools/vrcmomo_lan_bridge.py`
- Windows bridge UI and archive controls: `tools/vrcmomo_lan_bridge.py` (`run_gui`)
- Optional PC dependency list: `tools/requirements-lan-bridge.txt`
- Shared pairing parser and HTTP client: `service/LanActivityBridgeClient.kt`
- Shared UDP discovery contract: `service/LanBridgeDiscovery.kt`
- Android subnet broadcast discovery: `src/androidMain/kotlin/service/LanBridgeDiscovery.android.kt`
- Android QR scanner: `src/androidMain/kotlin/service/LanBridgeQrScanner.android.kt`
- Settings UI and import confirmation: `presentation/screens/home/sheet/SettingsBottomSheet.kt`
- Persisted pair and last-sync state: `storage/SettingsDao.kt`, `storage/data/LanSyncStatus.kt`

Discovery returns a short-lived pairing URL from the locally running bridge. If exactly one bridge is found, Android pairs and pulls the import preview immediately; if multiple are found, each discovered bridge has its own connect action. QR scanning follows the same pairing path. Do not add credential or VRChat-cookie transfer to this flow.

When `isLanSyncAutoEnabled` is on, Android performs one pull-and-upload cycle after the app process receives an authenticated session. It does not depend on foreground monitoring and does not run on a repeating timer; the implementation is in `src/androidMain/kotlin/VRCMApplication.kt`.

## Attribution
The original VRCM application structure and VRCM-authored features remain credited to the VRCM team. VRCMomo-specific UI, branding, friend activity persistence, notification/background work, and avatar editing are fork modifications.

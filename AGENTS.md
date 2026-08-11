# VRCMomo agent and contributor guide

This is the short entry point for future maintenance. Read this file first, then open only the path relevant to the task. It deliberately avoids a full-repository scan.

## Project boundaries

- **Product priority:** Android first. Keep common code KMP-safe; do not introduce Android APIs into `commonMain`.
- **UI:** Compose Multiplatform + Voyager + Koin. State belongs in ScreenModel/service; Composables render state and emit events.
- **Network:** Ktor API clients live in `composeApp/src/commonMain/kotlin/network/api/`.
- **Storage:** account-scoped durable data belongs in `storage/`, not transient settings/cache only.
- **Attribution:** VRCM is the upstream base. Describe VRCMomo changes as additive fork work; do not claim upstream functions as new.
- **Privacy:** never export, log, or persist VRChat cookies, credentials, tokens, moderation data, or unrelated private notes.

## Fast path map

| Task | Start here | Usually also inspect |
| --- | --- | --- |
| Version / APK name | `gradle/libs.versions.toml` | `composeApp/build.gradle.kts` |
| Test update channel | `downloads/testing-channel.json` | `core/shared/AppConst.kt`, `service/VersionService.kt` |
| Android build / tests | `gradlew.bat` | `docs/DEVELOPMENT_MAP.md` |
| UI screen / navigation | `presentation/screens/` | `presentation/components/`, `presentation/settings/locale/` |
| Theme / Ark-style colors | `presentation/settings/theme/` | `presentation/settings/` |
| VRChat API endpoint | `network/api/<area>/` | matching `data/` and ScreenModel |
| Friend activity / timeline | `service/FriendActivityService.kt` | `FriendActivityTracker.kt`, `storage/FriendActivityCacheDao.kt` |
| Activity log UI | `presentation/screens/home/sheet/SettingsBottomSheet.kt` | locale strings |
| Android notifications / background work | `src/androidMain/kotlin/presentation/notifications/` | `service/`, settings UI |
| Boop | `network/api/users/UsersApi.kt` | `presentation/components/BoopSelector.kt` |
| Avatar ownership / edit | `network/api/avatars/` | `presentation/screens/avatar/` |
| VRCX migration | `tools/export_vrcx_activity.py` | `service/VrcxActivityImport.kt` |
| LAN VRCX sync / pairing | `tools/vrcmomo_lan_bridge.py` | `service/LanActivityBridgeClient.kt`, `service/LanBridgeDiscovery.kt`, `presentation/screens/home/sheet/SettingsBottomSheet.kt` |
| Android LAN discovery / QR scan | `src/androidMain/kotlin/service/LanBridgeDiscovery.android.kt` | `LanBridgeQrScanner.android.kt`, `AndroidManifest.xml` |
| Persistent data migration | `storage/data/` | `storage/FriendActivityCacheDao.kt` |
| README / contribution credit | `README_ZH.md` | `docs/DEVELOPMENT_MAP.md` |

## Mandatory implementation rules

1. **Small, isolated change:** one concern per commit. Do not mix UI restyling, API behavior and storage migration unless they are inseparable.
2. **Kotlin idioms:** prefer `val`, data classes and exhaustive `when`; avoid `!!`; name constants instead of adding unexplained literals.
3. **Coroutine safety:** use structured scopes; IO/file/network work stays off the UI thread; cancellation must be allowed to propagate.
4. **Compose safety:** hoist state where practical; do not start network/file work directly during composition; use `LaunchedEffect`/ScreenModel events for side effects.
5. **Persistence compatibility:** when changing a serialized cache model, increment `FriendActivityCache.CURRENT_SCHEMA_VERSION` and add an explicit migration step. Do not rename/remove saved fields without preserving old data.
6. **API resilience:** validate external values, use nullable/default-compatible DTO fields for historical data, and expose user-friendly errors instead of raw stack traces.
7. **Locale coverage:** new user-visible strings go through `LocaleStrings`; Chinese Simplified must be updated in the same change.
8. **Tests:** add or update `commonTest` for pure shared logic. Run the smallest relevant test first, then build Android before release.

## Verification commands

```powershell
# Focused unit test while editing a service
.\gradlew.bat :composeApp:testDebugUnitTest --tests "io.github.vrcmteam.vrcm.service.YourTest"

# Android compilation and APK
.\gradlew.bat :composeApp:assembleDebug

# Full Android unit suite before a release candidate
.\gradlew.bat :composeApp:testDebugUnitTest
```

APK output: `composeApp/build/outputs/apk/debug/VRCMomo-v<version>.apk`.

## Reference documents

- `docs/CURRENT_WORK_STATUS.md`: current branch state, unfinished local UI batch, verification boundary and exact resume order. Read this before continuing unfinished work.
- `docs/DEVELOPMENT_MAP.md`: build, storage, API and feature ownership map.
- `docs/CODE_STANDARDS.md`: Kotlin/KMP/Compose review checklist and refactoring limits.
- `docs/VRCX_JIRAI_FEATURE_REVIEW.md`: source attribution and feature-boundary review.

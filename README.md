<div align="center">

# <img src="image/VRCMomoLogo.png" width="50" height="50" alt="VRCMomo logo" /> VRCMomo

[English](README.md) | [中文](README_ZH.md) | [日本語](README_JP.md)

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Neil100o/VRCMomo)](https://github.com/Neil100o/VRCMomo/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Neil100o/VRCMomo/total)](https://github.com/Neil100o/VRCMomo/releases/latest)

</div>

VRCMomo is an Android-first VRChat companion continued from [VRCM](https://github.com/vrcm-team/VRCM). It combines the existing mobile experience for friends, groups, worlds and Gallery with local friend activity history, notifications, Boop support, VRCX activity migration and LAN archiving.

VRCMomo is not affiliated with VRChat Inc. It does not upload VRChat cookies, passwords, tokens, notes or moderation data. The LAN bridge works only between your own computer and phone.

## Downloads and updates

Current release: **0.3.21**

| Need | Download | Purpose |
| --- | --- | --- |
| Android APK | `VRCMomo-v0.3.21.apk` in [Releases](https://github.com/Neil100o/VRCMomo/releases/latest) | Permanent-signature release. Future updates are checked through GitHub Releases. |
| Windows LAN bridge | `VRCMomo-LAN-Bridge.exe` in the same Release | Run beside VRCX to export, archive and synchronize activity history with a phone. |
| Firewall helper | `Allow-VRCMomoLanBridgeFirewall.bat` in the same Release | Use when the bridge starts but the phone cannot find or reach it; opens LAN-only ports. |
| Legacy migration APK | [VRCMomo-v0.3.20-legacy-migration.apk](downloads/VRCMomo-v0.3.20-legacy-migration.apk) | Only for the old 0.3.16 automatic-update track. |

Read the [feature log](CHANGELOG.md) for release changes and [installation and migration guide](docs/INSTALL_AND_MIGRATION.md) for signing and migration steps.

## Migrating from the old 0.3.16 testing track

The old testing app and the permanent release use different Android signing certificates, so they cannot replace one another directly.

1. Update the old app in place with `VRCMomo-v0.3.20-legacy-migration.apk`.
2. Run `VRCMomo-LAN-Bridge.exe` on a PC and complete one LAN sync from the old app.
3. Verify that the bridge archive exists, then install the 0.3.21 release.
4. Pair the release app with the same bridge and pull the archive. Check the activity log and relationship totals before removing the old app.

Do not uninstall the old app before step 2. The bridge deduplicates events and merges cumulative totals by maximum baseline, so repeated syncs do not inflate play time or meeting count.

## VRCMomo additions over upstream VRCM

This list intentionally covers additions, maintenance and improvements made in VRCMomo. Core account, friend/world browsing, favorites, baseline groups, relationship graph, mutual friends and VRChat+ Gallery features are primarily upstream VRCM work.

### Friend activity and history

- Local, account-scoped history for presence, location, social status, profile changes, shared play, last activity and relationship statistics.
- Filterable activity log, retention cleanup and per-friend profile history; bio/status changes can show their latest diff.
- Empty Users search prioritizes people met during the past 24 hours.
- Stable event fingerprints and maximum-baseline merging prevent duplicate history and cumulative-total inflation across restarts and devices.

### Android notifications and Boop

- System notification master switch; favorite-group/per-friend presence selection; friend request, friend add/remove, group event and Boop notifications.
- Android foreground monitoring service plus notification-permission and battery-setting entry points. Web presence is not treated as game online.
- Boop selector, in-app receive card and system notification. Version 0.3.21 fixes loss of emoji metadata from the legacy notification feed.

### Phone and PC activity aggregation

- Read-only VRCX SQLite export for presence, location, status, bio, avatar, friendship and completed shared-session history.
- Windows LAN Bridge with QR/LAN discovery, mobile upload/download and archive rebuild.
- The bridge never writes VRCX native tables or exports login credentials.

### Mobile experience and profile editing

- Independent VRCMomo identity, icon, version line and permanent-signature release channel.
- Light/dark themes, multiple color schemes, and responsive world, avatar and friend cards for phones and wider screens.
- Separate owned-avatar section with name, description and cover editing; self-profile bio and social-link editing.

## Documentation

| Document | Purpose |
| --- | --- |
| [CHANGELOG.md](CHANGELOG.md) | Release feature log and fixes. |
| [docs/INSTALL_AND_MIGRATION.md](docs/INSTALL_AND_MIGRATION.md) | Installation, signing, migration, LAN Bridge usage and troubleshooting. |
| [docs/FEATURE_AUDIT_AND_PATHS.md](docs/FEATURE_AUDIT_AND_PATHS.md) | Main-branch feature audit, branch reconciliation and exact source paths. |
| [docs/LAN_SYNC_DESIGN.md](docs/LAN_SYNC_DESIGN.md) | Data boundaries and merge rules for the phone, bridge and VRCX export. |
| [docs/STORAGE_COMPATIBILITY.md](docs/STORAGE_COMPATIBILITY.md) | Storage compatibility and migration rules. |

## Attribution

VRCMomo is a fork and independent continuation of [VRCM](https://github.com/vrcm-team/VRCM). The Kotlin Multiplatform/Compose foundation, authentication, VRChat API/network layer, core friend/world functionality, favorites, groups, baseline notifications, Gallery and relationship features are primarily VRCM Team and upstream contributor work.

VRCMomo maintains its own branding, mobile UI, activity history, notification/background work, Boop support, VRCX/LAN migration, profile editing, release signing and stability fixes. Upstream features adapted here must not be presented as VRCMomo work from scratch.

## Development

Read [AGENTS.md](AGENTS.md) and [docs/FEATURE_AUDIT_AND_PATHS.md](docs/FEATURE_AUDIT_AND_PATHS.md) before editing. Typical Android verification from the repository root:

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :composeApp:assembleRelease
```

Tester packages must use the permanent release signature; see [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md).

## License

VRCMomo is licensed under the [MIT License](LICENSE). Reproducible reports with device details and logs are welcome through [Issues](https://github.com/Neil100o/VRCMomo/issues).

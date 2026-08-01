<div align="center">

# <img src="image/VRCMomoLogo.png" width="50" height="50"  alt="logo"/> VRCMomo

<!-- Language Selection -->
**Languages / 语言 / 言語:**
[English](README.md) • [中文](README_ZH.md) • [日本語](README_JP.md)


[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![GitHub release](https://img.shields.io/github/release/Neil100o/VRCMomo.svg)](https://github.com/Neil100o/VRCMomo/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Neil100o/VRCMomo/total?color=6451f1)](https://github.com/Neil100o/VRCMomo/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-blue)](https://www.jetbrains.com/lp/compose-multiplatform/)

## Mobile VRChat Companion

A mobile-first VRChat companion, continued from VRCM, for managing friends, Gallery, notifications, and recent shared play.

> **Current test version: 0.3.11.** Please report reproducible issues with device, Android version, steps and logs through [Issues](https://github.com/Neil100o/VRCMomo/issues).

## Test downloads

- **Android APK:** [VRCMomo-v0.3.11.apk](downloads/VRCMomo-v0.3.11.apk) — download it to an Android device, allow installation from the browser/file manager when Android asks, then install.
- **Optional VRCX exporter (Windows):** [VRCMomo-VRCX-Activity-Export.exe](downloads/VRCMomo-VRCX-Activity-Export.exe) — run it on the computer with VRCX, then import its JSON file from VRCMomo settings.
- See [downloads/README.md](downloads/README.md) for privacy boundaries and testing notes. These are test files, not a stable release.
- The app checks this testing channel for newer Android builds; use the update prompt to open the APK directly.

</div>

<div align="center">

## VRCMomo changes over upstream VRCM

</div>

### Friend activity and relationship history
- Durable, account-scoped activity records: observed online/offline state, last activity, last meeting, meeting count and shared-play duration survive app upgrades.
- A filterable local timeline for presence, location, social status, shared play and profile changes; BIO/status changes show before/after values and line diffs.
- The empty Users search tab shows up to 20 players met in the last 24 hours after local activity data has been collected.

### Android notifications and monitoring
- Optional native notifications for Boops and favorited friends entering/leaving VRChat.
- Optional foreground monitoring and battery-optimization guidance to improve background recording reliability.
- In-app activity log retention/cleanup controls that preserve relationship totals.

### Boop, avatar and data migration additions
- Default-emoji Boop selection, received-Boop in-app reaction card, and notification recovery after opening the app.
- Separate owned-avatar section with editing for a creator's avatar name, description and cover information.
- Read-only VRCX activity exporter plus Android merge import for personal presence, location, status, BIO, avatar, friendship and completed shared-session history. Credentials and cookies are excluded.

### Product identity and mobile UI maintenance
- Independent VRCMomo name, icon, 0.x version line and APK naming.
- Dark/light themes, multiple color schemes, and ongoing mobile navigation, presentation and stability maintenance.

> **Attribution:** This section intentionally lists only VRCMomo additions or maintenance. Accounts, friend/world browsing, favorites, baseline groups/notifications, VRChat+ Gallery, relationship graph and mutual-friend features are primarily upstream VRCM work.

<div align="center">

## Platform Support

</div>

- **Android** - Current primary testing and maintenance platform

<div align="center">

## Development Roadmap

</div>

### Current Focus
- **Mobile Stability** - Prioritize Android device fixes, background monitoring, and notification reliability
- **Community Testing** - Improve the existing friend, group, Gallery, and profile features from real-world feedback

<div align="center">

## Technical Architecture

</div>

### Core Technology Stack
- **[Kotlin Multiplatform](https://kotlinlang.org/multiplatform/)** - Cross-platform development framework
- **[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)** - Modern UI framework
- **[Ktor](https://ktor.io/)** - Network requests and API communication
- **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** - JSON data serialization

### Architecture Components
- **[Koin](https://github.com/InsertKoinIO/koin)** - Dependency injection framework
- **[Voyager](https://github.com/adrielcafe/voyager)** - Navigation and state management
- **[Multiplatform-Settings](https://github.com/russhwolf/multiplatform-settings)** - Cross-platform configuration storage
- **[Coil](https://github.com/coil-kt/coil)** - High-performance image loading

### Development Environment
- **Kotlin API**: 2.1
- **Android SDK Target**: 35
- **Java SDK**: 21
- **Compose**: 1.8.2

<div align="center">

## Disclaimer

</div>

- VRCMomo is not affiliated with VRChat Inc and does not represent the views or opinions of VRChat Inc
- VRCMomo does not store or collect any data outside of your device
- The application author is not responsible for any damage caused by this application
- VRCMomo does not modify or tamper with the game and does not violate [VRChat Terms of Service](https://hello.vrchat.com/legal)
- Please use this application responsibly and comply with relevant laws, regulations, and platform rules

<div align="center">

## Attribution and Project Origin

</div>

VRCMomo is a fork and independent continuation of [VRCM](https://github.com/vrcm-team/VRCM), originally developed by the VRCM Team and its contributors.

The following parts of this project are primarily derived from the upstream VRCM project:

- The Kotlin Multiplatform and Compose Multiplatform application foundation
- Android and iOS project structure, authentication, and account management
- The foundational VRChat API/networking layer
- Friend lists, friend status/location, user profiles, and world/user search
- Favorites, groups, notifications, and related existing UI components
- The VRChat+ Gallery and friend relationship / mutual-friend pages

VRCMomo's own work is focused on the new branding and app identity, theme and navigation changes, Boop support and related notification improvements, Gallery fixes and presentation changes, iOS branding, release packaging, and other changes documented in this repository's commit history. Existing upstream features may also be adapted or maintained here; they should not be interpreted as being reimplemented from scratch by VRCMomo.

We are grateful to the VRCM Team and all upstream contributors. Please refer to the [upstream repository](https://github.com/vrcm-team/VRCM) for the original project history and contribution attribution.

For the planned review of VRCX and VRCX-jirai-inspired features, see [VRCX-jirai feature review](docs/VRCX_JIRAI_FEATURE_REVIEW.md). The review separates upstream VRCX ideas, VRCX-jirai-specific work, VRCM contributions, and VRCMomo-only work, and keeps privacy-sensitive automation out of the stable line.

<div align="center">

## License

</div>

This project is open source under the [MIT License](LICENSE).

<div align="center">

## Contributing

</div>

Contributions of code, bug reports, or feature suggestions are welcome! Please check our contribution guidelines for more information.

---

<div align="center">

**If this project is helpful to you, please give us a ⭐**

[Download Latest Release](https://github.com/Neil100o/VRCMomo/releases/latest) • [Report Issues](https://github.com/Neil100o/VRCMomo/issues) • [Feature Requests](https://github.com/Neil100o/VRCMomo/discussions)

</div>

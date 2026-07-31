<div align="center">

# <img src="image/Logo.png" width="50" height="50"  alt="logo"/> VRCMomo

<!-- Language Selection -->
**🌐 Languages / 语言 / 言語:**  
[English](README.md) • [中文](README_ZH.md) • [日本語](README_JP.md)


[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![GitHub release](https://img.shields.io/github/release/Neil100o/VRCMoskavis.svg)](https://github.com/Neil100o/VRCMoskavis/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Neil100o/VRCMoskavis/total?color=6451f1)](https://github.com/Neil100o/VRCMoskavis/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-blue)](https://www.jetbrains.com/lp/compose-multiplatform/)

## Multi-platform VRChat Friend "Monitoring" Application

A feature-rich cross-platform VRChat friend management application that lets you keep track of your friends' activities anytime, anywhere.

</div>

<div align="center">

## ✨ Core Features

</div>

### 🔐 Account Management
- **Multi-account Support** - Quickly switch between different VRChat accounts
- **Login Authentication** - Support for email and 2FA login verification

### 👥 Friend System
- **Friend List** - Real-time view of all friends' online status and activity information
- **Friend Location** - Track friends' current worlds and rooms
- **Friend Profile** - View detailed user information, status, and bio
- **Friend Management** - Complete operations including adding new friends and removing friends

### 🔍 Search Functionality
- **User Search** - Quickly find VRChat users by username
- **World Search** - Discover and search various worlds in VRChat

### 🌍 World Features
- **World Details** - View detailed world information, descriptions, tags, and preview images
- **World Favorites** - Favorite preferred worlds with support for multiple collection groups
- **World Browsing** - Browse popular and recommended worlds
- **Room Invitations** - Invite yourself to rooms

### 🔔 Notification System
- **Real-time Notifications** - Receive friend requests, invitations, group notifications, and other types of notifications
- **Notification Management** - Display in chronological order with support for marking as read and deletion
- **Friend Requests** - Handle friend requests, accept or decline invitations

### 🎨 Interface Experience
- **Modern Design** - Follows Material Design principles
- **Multi-theme Support** - Switch between dark/light themes and various color themes
- **Internationalization** - Support for multiple language interfaces
- **Smooth Animations** - Shared element transitions and elegant interactive animations

### 🖼️ VRChat+ Gallery
- **Photo Browsing** - View all photos taken in-game
- **Photo Download** - Save favorite photos to local device
- **Zoom Preview** - Support for zooming and detailed photo viewing

  <img src="image/Gallery-1.png" width="201" height="437"  alt="Gallery-1"/>
  <img src="image/Gallery-2.png" width="201" height="437"  alt="Gallery-2"/>

<div align="center">

## 📱 Platform Support

</div>

- ✅ **Android** - Full feature support
- ✅ **iOS** - Full feature support (requires [self-signing](self-signing.md))

<div align="center">

## 🖥️ Interface Preview

</div>

### Multi-platform Preview:

<div align="center">

![MultiPlatformPreview.png](image/MultiPlatformPreview.png)

</div>

### UI Interface Preview:

<div align="center">

![UIPreview.png](image/UIPreview.png)

</div>

<div align="center">

## 📋 Development Roadmap

</div>

### Coming Soon:
- 📷 **Gallery Upload Functionality** - Support for uploading images from local devices through gallery or camera to VRChat+ gallery
- 👤 **User Profile Editing** - Support for users to modify their personal bio, avatar, and other profile information
- 👥 **Group Features** - Complete group functionality including group profile viewing and group room viewing

### Future Possibilities?
- 📱 **Widescreen Adaptation** - Perfect adaptation for tablets and foldable devices with dual-screen layout and multi-window operations
- 🖥️ **Complete Desktop Support** - Full platform support for Windows, macOS, and Linux
- 📊 **Activity History Records** - Background persistent recording of friend activity history with long-term data storage and query support
- 📢 **System Notifications** - Native system notification support
- 🤖 **Smart Assistant** - AI-driven friend activity analysis

<div align="center">

## 🛠️ Technical Architecture

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

## ⚠️ Disclaimer

</div>

- VRCMomo is not affiliated with VRChat Inc and does not represent the views or opinions of VRChat Inc
- VRCMomo does not store or collect any data outside of your device
- The application author is not responsible for any damage caused by this application
- VRCMomo does not modify or tamper with the game and does not violate [VRChat Terms of Service](https://hello.vrchat.com/legal)
- Please use this application responsibly and comply with relevant laws, regulations, and platform rules

<div align="center">

## 🙏 Attribution and Project Origin

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

## 📄 License

</div>

This project is open source under the [MIT License](LICENSE).

<div align="center">

## 🤝 Contributing

</div>

Contributions of code, bug reports, or feature suggestions are welcome! Please check our contribution guidelines for more information.

---

<div align="center">

**If this project is helpful to you, please give us a ⭐**

[Download Latest Release](https://github.com/Neil100o/VRCMoskavis/releases/latest) • [Report Issues](https://github.com/Neil100o/VRCMoskavis/issues) • [Feature Requests](https://github.com/Neil100o/VRCMoskavis/discussions)

</div>

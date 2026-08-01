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

## VRCMomo：面向日常社交的 VRChat 移动 Companion

一个基于 VRCM 延续开发的移动端 VRChat Companion，帮助你在手机上管理好友、相册、通知与最近共同游玩记录。

> **当前测试版本：0.3.0**。欢迎通过 [Issues](https://github.com/Neil100o/VRCMoskavis/issues) 反馈问题、设备信息和复现步骤。

</div>

<div align="center">

## ✨ 核心功能

</div>

### 🔐 账户管理
- **多账户支持** - 快速切换不同的 VRChat 账户
- **登录认证** - 支持邮箱、2FA登陆验证

### 👥 好友系统
- **好友列表** - 实时查看所有好友的在线状态和活动信息
- **好友位置** - 追踪好友当前所在的世界和房间
- **好友资料** - 查看详细的用户信息、状态和BIO等
- **好友管理** - 添加新好友、删除好友等完整操作
- **活动记录** - 持久化保存实际观察到的上下线、最后活动、见面次数与共同游玩时长

### 🔍 搜索功能
- **用户搜索** - 通过用户名快速查找 VRChat 用户
- **世界搜索** - 发现和搜索 VRChat 中的各种世界

### 🌍 世界功能
- **世界详情** - 查看世界的详细信息、描述、标签和预览图
- **世界收藏** - 收藏喜欢的世界，支持多个收藏组管理
- **世界浏览** - 浏览热门和推荐世界
- **房间邀请** - 可以邀请自己进入房间

### 👥 群组功能
- **群组资料** - 查看群组简介、图标、横幅与基础资料
- **成员与帖子** - 分页浏览成员、公告与帖子
- **群组画廊与房间** - 浏览群组画廊，并查看可见的群组房间状态

### 🔔 通知系统
- **实时通知** - 接收好友请求、邀请、群组通知等各类通知
- **通知管理** - 按时间排序显示，支持标记已读和删除操作
- **好友请求** - 处理好友请求，接受或拒绝邀请
- **原生系统通知** - Android 支持 Boop 与收藏好友上下线的本地系统通知

### 👤 用户资料与模型管理
- **资料编辑** - 编辑自己的状态、简介等资料
- **自制模型管理** - 单独查看自己上传的模型，并编辑名称、介绍和封面

### 🎨 界面体验
- **现代化设计** - 遵循 Material Design 设计规范
- **多主题支持** - 深色/浅色与各种配色主题切换
- **国际化** - 支持多种语言界面
- **流畅动画** - 共享元素过渡和精美的交互动画

### 🖼️ VRChat+ 画廊
- **照片浏览** - 查看游戏内拍摄的所有照片
- **照片下载** - 将喜欢的照片保存到本地设备
- **缩放预览** - 支持照片的缩放和详细查看

  <img src="image/Gallery-1.png" width="201" height="437"  alt="Gallery-1"/>
  <img src="image/Gallery-2.png" width="201" height="437"  alt="Gallery-2"/>

<div align="center">

## 📱 平台支持

</div>

- ✅ **Android** - 当前主要测试与维护平台

<div align="center">

## 🖥️ 界面预览

</div>

### 多平台预览:

<div align="center">

![MultiPlatformPreview.png](image/MultiPlatformPreview.png)

</div>

### UI 界面预览:

<div align="center">

![UIPreview.png](image/UIPreview.png)

</div>

<div align="center">

## 📋 开发路线图

</div>

### 当前重点
- 📱 **移动端稳定性** - 优先修复 Android 实机问题、后台监测与通知可靠性
- 🧪 **群友测试** - 根据实际使用反馈完善已有的好友、群组、相册与资料管理功能

<div align="center">

## 🛠️ 技术架构

</div>

### 核心技术栈
- **[Kotlin Multiplatform](https://kotlinlang.org/multiplatform/)** - 跨平台开发框架
- **[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)** - 现代化UI框架
- **[Ktor](https://ktor.io/)** - 网络请求和API通信
- **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** - JSON数据序列化

### 架构组件
- **[Koin](https://github.com/InsertKoinIO/koin)** - 依赖注入框架
- **[Voyager](https://github.com/adrielcafe/voyager)** - 导航和状态管理
- **[Multiplatform-Settings](https://github.com/russhwolf/multiplatform-settings)** - 跨平台配置存储
- **[Coil](https://github.com/coil-kt/coil)** - 高性能图片加载

### 开发环境
- **Kotlin API**: 2.1
- **Android SDK Target**: 35
- **Java SDK**: 21
- **Compose**: 1.8.2

<div align="center">

## 🙏 项目来源与致谢

</div>

VRCMomo 是基于 [VRCM](https://github.com/vrcm-team/VRCM) 开发的 fork 和独立延续项目。VRCM 最初由 VRCM Team 及其贡献者开发。

关于后续参考 VRCX 与 VRCX-jirai 的功能审查、来源归属和隐私边界，请参阅 [VRCX-jirai 功能审查与移植计划](docs/VRCX_JIRAI_FEATURE_REVIEW.md)。其中会区分 VRCM 上游、VRCX 原版、VRCX-jirai 特有思路与 VRCMomo 自行实现，并把敏感的自动化追踪功能留在实验分支之外。


下列基础工作主要来源于上游 VRCM：Kotlin Multiplatform 与 Compose Multiplatform 基础工程、Android / iOS 项目结构、登录认证与账户管理、VRChat API 和网络层、好友与世界管理、收藏、群组、通知、已有 UI 组件、VRChat+ Gallery、好友关系网与共同好友页面。

VRCMomo 在此基础上加入并维护了新的品牌、主题与导航调整、Boop 与通知改进、Gallery 修复与展示调整、iOS 品牌适配、发行打包等更改。上游已有功能可能会在此继续适配和维护，但这些不应被视为 VRCMomo 从零开始实现的功能。

感谢 VRCM Team 以及所有上游贡献者的原始工作。完整历史和贡献归属，请以[上游仓库](https://github.com/vrcm-team/VRCM)为准。

<div align="center">

## ⚠️ 免责声明

</div>

- VRCMomo 与 VRChat Inc 无关联，不代表 VRChat Inc 的观点或意见
- VRCMomo 不会在您的设备之外存储或收集任何数据
- 应用作者不对此应用造成的任何损害负责
- VRCMomo 不修改或篡改游戏，不违反 [VRChat 服务条款](https://hello.vrchat.com/legal)
- 请合理使用此应用，遵守相关法律法规和平台规定

<div align="center">

## 📄 许可证

</div>

本项目基于 [MIT 许可证](LICENSE) 开源。

<div align="center">

## 🤝 贡献

</div>

欢迎贡献代码、报告问题或提出功能建议！请查看我们的贡献指南了解更多信息。

---

<div align="center">

**如果这个项目对您有帮助，请给我们一个 ⭐**

[下载最新版本](https://github.com/Neil100o/VRCMoskavis/releases/latest) • [反馈问题](https://github.com/Neil100o/VRCMoskavis/issues) • [功能建议](https://github.com/Neil100o/VRCMoskavis/discussions)

</div>

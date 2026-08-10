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

## VRCMomo：面向日常社交的 VRChat 移动 Companion

一个基于 VRCM 延续开发的移动端 VRChat Companion，帮助你在手机上管理好友、相册、通知与最近共同游玩记录。

> **当前测试版本：0.3.16**。欢迎通过 [Issues](https://github.com/Neil100o/VRCMomo/issues) 反馈问题、设备信息和复现步骤。

## 测试版下载

- **Android 安装包：**[VRCMomo-v0.3.15.apk](downloads/VRCMomo-v0.3.15.apk) —— 下载到 Android 设备；系统提示时，允许浏览器或文件管理器安装未知来源应用后安装。
- **可选 VRCX 导出工具（Windows）：**[VRCMomo-VRCX-Activity-Export.exe](downloads/VRCMomo-VRCX-Activity-Export.exe) —— 在安装了 VRCX 的电脑上运行，再从 VRCMomo 设置页导入生成的 JSON 文件。
- **局域网桥接器（Windows）：**[VRCMomo-LAN-Bridge.exe](downloads/VRCMomo-LAN-Bridge.exe) —— 在装有 VRCX 的电脑上直接运行，再由同一局域网中的 VRCMomo 配对同步；已内置 Python 与二维码支持。
- 隐私边界和测试注意事项见 [downloads/README.md](downloads/README.md)。以上均为测试文件，不是稳定发行版。
- 客户端会检查该测试通道；发现新版 Android 安装包时，可直接通过更新提示打开下载地址。

</div>

<div align="center">

## VRCMomo 差异功能概览

</div>

### 好友活动与关系记录
- **本地持久化活动档案** - 保存实际观察到的上下线、最后活动、最后见面、见面次数与共同游玩时长；更新应用不会清空既有记录。
- **可筛选活动时间线** - 记录上下线、位置、社交状态、共同游玩与资料变化；支持分类/多选筛选、按天自动清理或永久保存。
- **精确变化展示** - BIO 与状态变化以类似 diff 的方式显示：绿色为新增、红色为删除；新产生的位置和状态事件会保留前后值。
- **最近共同游玩入口** - 首页“用户”页签在搜索框留空时，显示近 24 小时内共同游玩的玩家（最多 20 人，需先积累活动数据）。

### Android 通知与后台监测
- **可选系统通知** - 可单独关闭系统通知；支持 Boop、收藏好友进入/离开游戏等本地通知。
- **后台监测提示** - 提供前台服务与电池优化白名单的可选开关和说明，尽量提升后台活动记录与通知的存活率。
- **应用内活动日志** - 在客户端查看好友活动，并可只清除日志而保留关系统计摘要。

### Boop 体验补全
- **表情 Boop 选择** - 支持 VRChat 默认 Boop 表情常量，而不是只发送空白默认戳。
- **接收体验** - 收到 Boop 后可显示应用内反应卡片，并支持 Android 系统通知；从通知打开应用后也会尝试恢复未读 Boop。

### 自制模型与资料管理增强
- **我的模型分类** - 将当前账户上传的模型单独归类。
- **模型编辑** - 可编辑自己上传模型的名称、介绍与封面信息。

### VRCX 活动数据导入
- **桌面导出 + 手机导入** - 提供只读导出工具与 Android 导入合并流程，用于迁移自己的 VRCX 好友活动历史。
- **不迁移敏感登录信息** - 该流程只处理活动记录，不导出 Cookie、密码或账号凭据。

### 移动端主题与稳定性维护
- **VRCMomo 品牌与版本体系** - 独立名称、图标、0.x 测试版版本号与 APK 文件名。
- **主题与信息层级调整** - 提供明暗模式和多套配色，并持续针对移动端调整导航、好友信息呈现与稳定性。

> **归属说明：**本页只列出 VRCMomo 相对上游 VRCM 的新增、改进或维护内容。账户、好友/世界浏览、收藏、基础群组、基础通知、VRChat+ Gallery、关系网和共同好友等基础能力主要来自上游 VRCM，并不在此重复列为 VRCMomo 功能。

<div align="center">

## 平台支持

</div>

- **Android** - 当前主要测试与维护平台

<div align="center">

## 开发路线图

</div>

### 当前重点
- **移动端稳定性** - 优先修复 Android 实机问题、后台监测与通知可靠性
- **群友测试** - 根据实际使用反馈完善已有的好友、群组、相册与资料管理功能

<div align="center">

## 技术架构

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

## 项目来源与致谢

</div>

VRCMomo 是基于 [VRCM](https://github.com/vrcm-team/VRCM) 开发的 fork 和独立延续项目。VRCM 最初由 VRCM Team 及其贡献者开发。

关于后续参考 VRCX 与 VRCX-jirai 的功能审查、来源归属和隐私边界，请参阅 [VRCX-jirai 功能审查与移植计划](docs/VRCX_JIRAI_FEATURE_REVIEW.md)。其中会区分 VRCM 上游、VRCX 原版、VRCX-jirai 特有思路与 VRCMomo 自行实现，并把敏感的自动化追踪功能留在实验分支之外。


下列基础工作主要来源于上游 VRCM：Kotlin Multiplatform 与 Compose Multiplatform 基础工程、Android / iOS 项目结构、登录认证与账户管理、VRChat API 和网络层、好友与世界管理、收藏、群组、通知、已有 UI 组件、VRChat+ Gallery、好友关系网与共同好友页面。

VRCMomo 在此基础上加入并维护了新的品牌、主题与导航调整、Boop 与通知改进、Gallery 修复与展示调整、iOS 品牌适配、发行打包等更改。上游已有功能可能会在此继续适配和维护，但这些不应被视为 VRCMomo 从零开始实现的功能。

感谢 VRCM Team 以及所有上游贡献者的原始工作。完整历史和贡献归属，请以[上游仓库](https://github.com/vrcm-team/VRCM)为准。

<div align="center">

## 免责声明

</div>

- VRCMomo 与 VRChat Inc 无关联，不代表 VRChat Inc 的观点或意见
- VRCMomo 不会在您的设备之外存储或收集任何数据
- 应用作者不对此应用造成的任何损害负责
- VRCMomo 不修改或篡改游戏，不违反 [VRChat 服务条款](https://hello.vrchat.com/legal)
- 请合理使用此应用，遵守相关法律法规和平台规定

<div align="center">

## 许可证

</div>

本项目基于 [MIT 许可证](LICENSE) 开源。

<div align="center">

## 贡献

</div>

欢迎贡献代码、报告问题或提出功能建议！请查看我们的贡献指南了解更多信息。

---

<div align="center">

**如果这个项目对您有帮助，请给我们一个 ⭐**

[下载最新版本](https://github.com/Neil100o/VRCMomo/releases/latest) • [反馈问题](https://github.com/Neil100o/VRCMomo/issues) • [功能建议](https://github.com/Neil100o/VRCMomo/discussions)

</div>

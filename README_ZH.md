<div align="center">

# <img src="image/VRCMomoLogo.png" width="50" height="50" alt="VRCMomo logo" /> VRCMomo

[English](README.md) | [中文](README_ZH.md) | [日本語](README_JP.md)

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Neil100o/VRCMomo)](https://github.com/Neil100o/VRCMomo/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Neil100o/VRCMomo/total)](https://github.com/Neil100o/VRCMomo/releases/latest)

</div>

VRCMomo 是基于 [VRCM](https://github.com/vrcm-team/VRCM) 继续维护的 Android 优先 VRChat Companion。它把好友、群组、世界、Gallery 等原有移动端能力，与本地好友活动记录、通知、Boop、VRCX 活动迁移和局域网归档整合在一起。


VRCMomo 的 Android 包名为 `io.github.neil100o.vrcmomo`，已与上游 VRCM 完全分开；两者可以在同一台设备上同时安装。

VRCMomo 与 VRChat Inc. 没有关联。它不会上传 VRChat Cookie、密码、令牌、备注或管理数据；局域网桥接器仅在你的电脑和手机之间工作。

## 下载与更新

当前发行版：**0.3.23**

| 需要什么 | 从哪里下载 | 用途 |
| --- | --- | --- |
| Android 安装包 | [Releases](https://github.com/Neil100o/VRCMomo/releases/latest) 的 `VRCMomo-v0.3.23.apk` | 固定签名正式发行版；后续在应用内检查 GitHub Releases 更新。 |
| Windows 局域网桥接器 | 同一 Release 的 `VRCMomo-LAN-Bridge.exe` | 在装有 VRCX 的电脑上运行，用于导出/汇总活动记录并与手机同步。 |
| Windows 防火墙辅助脚本 | 同一 Release 的 `Allow-VRCMomoLanBridgeFirewall.bat` | 桥接器能运行但手机找不到或连不上时使用；只放行局域网端口。 |
| 旧测试轨迁移包 | [downloads/VRCMomo-v0.3.20-legacy-migration.apk](downloads/VRCMomo-v0.3.20-legacy-migration.apk) | **仅**供旧 0.3.16 自动更新轨覆盖安装、导出旧记录使用。 |

完整更新内容见 [功能日志](CHANGELOG.md)。安装、签名差异和旧记录迁移步骤见 [安装与迁移说明](docs/INSTALL_AND_MIGRATION.md)。

## 从旧 0.3.16 测试版迁移

旧测试版与正式版的 Android 签名不同，因此不能直接覆盖安装；请按顺序操作：

1. 旧应用先更新到 `VRCMomo-v0.3.20-legacy-migration.apk`。
2. 在电脑运行 `VRCMomo-LAN-Bridge.exe`，在旧应用的设置中完成一次局域网同步。
3. 在桥接器中确认活动归档已经建立后，安装 0.3.23 正式版。
4. 在正式版再次连接同一桥接器并拉取归档；核对活动日志和关系统计后，再自行决定是否删除旧应用。

迁移前不要卸载旧应用。桥接归档采用事件去重和最大基线合并，重复同步不会重复累计共同游玩时长或次数。

## VRCMomo 相对 VRCM 的改进

这里只列出 VRCMomo 的新增、维护和改进；好友/世界浏览、收藏、基础群组、关系网、共同好友、VRChat+ Gallery 等基础能力主要来自上游 VRCM。

### 好友活动与记录

- 本地、账号范围的好友活动档案：上下线、位置、社交状态、资料变动、共同游玩、最后活动和关系统计。
- 活动日志支持分类筛选、保留期清理与单人资料页查看；简介与状态可以显示最近一次的差异内容。
- 空搜索的“用户”页签会优先显示最近 24 小时共同游玩的玩家。
- 跨应用重启、跨设备同步时，活动事件按稳定指纹去重；累计统计使用最大基线合并，不会相加膨胀。

### Android 通知与 Boop

- 系统通知总开关、收藏夹/单好友的上下线提醒选择，以及好友请求、好友增删、群组事件和 Boop 通知。
- Android 前台监测服务、通知权限与耗电设置入口；网页在线不会作为“游戏上线”通知。
- Boop 表情选择、应用内接收卡片和系统通知。0.3.23 修复了旧通知接口的表情数据在合并时丢失的问题。

### 手机与电脑记录汇总

- VRCX SQLite 只读导出：活动、位置、状态、BIO、头像、好友关系和已结束共同游玩记录。
- Windows LAN Bridge：二维码/局域网发现、手机上传与下载、电脑端归档重建。
- 桥接器不会写入 VRCX 原始数据库，也不会导出登录凭据。

### 移动端体验与资料管理

- VRCMomo 独立名称、图标、版本和固定签名发行通道。
- 明暗模式、多套配色、手机与宽屏自适应的世界、模型、好友卡片布局。
- 自己上传模型的独立分类与名称、介绍、封面编辑；自己资料页支持简介和社交链接编辑。

## 文档入口

| 文档 | 内容 |
| --- | --- |
| [CHANGELOG.md](CHANGELOG.md) | 各发行版的功能日志与修复记录。 |
| [docs/INSTALL_AND_MIGRATION.md](docs/INSTALL_AND_MIGRATION.md) | 正式版安装、旧签名迁移、桥接器使用和排错。 |
| [docs/FEATURE_AUDIT_AND_PATHS.md](docs/FEATURE_AUDIT_AND_PATHS.md) | 当前 main 的功能回查、遗留分支结论、精确源码路径。 |
| [docs/LAN_SYNC_DESIGN.md](docs/LAN_SYNC_DESIGN.md) | 手机、桥接器、VRCX 导出之间的数据边界与合并规则。 |
| [docs/STORAGE_COMPATIBILITY.md](docs/STORAGE_COMPATIBILITY.md) | 存档兼容与迁移约束。 |
| [docs/CODE_STANDARDS.md](docs/CODE_STANDARDS.md) | Kotlin/KMP/Compose 修改与验证要求。 |

## 项目来源与贡献归属

VRCMomo 是 [VRCM](https://github.com/vrcm-team/VRCM) 的 fork 和独立延续项目。Kotlin Multiplatform / Compose 工程、认证、VRChat API 网络层、好友与世界基础功能、收藏、群组、基础通知、Gallery、关系网等主要来自 VRCM Team 及上游贡献者。

VRCMomo 在此基础上维护自己的品牌、移动端 UI、活动记录、通知与后台支持、Boop、VRCX/LAN 迁移、资料编辑、发行签名和稳定性修复。请勿将上游能力描述为 VRCMomo 从零实现。

## 开发

开发入口和精确路径见 [AGENTS.md](AGENTS.md)。Android 验证通常从仓库根目录执行：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :composeApp:assembleRelease
```

发行测试包必须使用固定签名 release 构建；详细规则见 [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md)。

## 许可证

本项目使用 [MIT License](LICENSE)。欢迎通过 [Issues](https://github.com/Neil100o/VRCMomo/issues) 提交可复现的问题、设备信息和日志。

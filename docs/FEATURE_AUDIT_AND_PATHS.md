# VRCMomo 功能回查与路径索引

最后回查：2026-08-13
仓库根目录：`F:\vrcmoskavis\VRCMomoLanSync`
基线：`main` / `origin/main` 的 `6b96c2d4`（`fix-boop-details-from-legacy-feed`）

这份文档回答两件事：

1. 之前做过的修复是否仍在当前 `main`；
2. 下一次继续时应该从哪一个**具体文件**开始，而不是重新扫描整个仓库。

## 1. 回查结论

### 仍在 main 的主要能力

| 能力 | 当前状态 | 主要实现路径 |
| --- | --- | --- |
| 好友活动持续记录、去重、旧数据兼容 | 已在 main | `composeApp/src/commonMain/kotlin/service/FriendActivityService.kt`、`FriendActivityTracker.kt`、`storage/FriendActivityCacheDao.kt` |
| SQLite 活动镜像与恢复 | 已在 main | `composeApp/src/commonMain/kotlin/storage/RoomFriendActivityMirror.kt`、`VrcmomoActivityDatabase.kt`、平台对应的 `VrcmomoActivityDatabaseFactory.*.kt` |
| 好友活动总览与单人资料活动区 | 已在 main | `presentation/screens/home/FriendActivityOverviewScreen.kt`、`presentation/screens/user/UserProfileScreen.kt` |
| 近 24 小时共同游玩的人默认出现在空搜索 | 已在 main | `presentation/screens/home/pager/SearchListPagerModel.kt` 的 `recentPlayersSince(...)` 调用 |
| 好友上下线、好友请求、增删好友、群组事件、Boop 的 Android 通知 | 已在 main | `service/SocialNotificationService.kt`、`service/IncomingBoopNotificationService.kt`、`src/androidMain/kotlin/presentation/notifications/AndroidPlatformNotificationService.kt` |
| 收藏夹/单好友的上下线通知选择 | 已在 main | `service/FriendPresenceNotificationSelection.kt`、`presentation/screens/home/pager/FriendListPager.kt`、`presentation/screens/user/UserProfileScreen.kt` |
| 前台监测、开机恢复、后台设置状态 | 已在 main | `src/androidMain/kotlin/service/FriendActivityForegroundService.kt`、`NotificationMonitorBootReceiver.kt`、`presentation/screens/home/sheet/SettingsBottomSheet.kt` |
| 官方链接解析、剪贴板一次性提示、复制官网链接 | 已在 main | `presentation/compoments/OfficialLinkPrompt.kt`、`OfficialLinkPromptController.kt`、`service/OfficialLinkService.kt`、`service/OfficialLinkInbox.kt` |
| VRCX 活动导出/导入与 LAN 桥接 | 已在 main | `tools/export_vrcx_activity.py`、`service/VrcxActivityImport.kt`、`tools/vrcmomo_lan_bridge.py`、`service/LanActivityBridgeClient.kt` |
| 局域网事件合并基线、旧快照迁移、空闲超时 | 已在 main | `tools/vrcmomo_activity_merge.py`、`tools/test_vrcmomo_activity_merge.py`、`service/LanActivityBridgeClient.kt` |
| 世界/模型/好友响应式卡片与宽屏布局 | 已在 main | `presentation/compoments/AdaptiveCardLayout.kt`、`SearchItemRenderers.kt`、`StandardSearchList.kt` |
| 自己资料页编辑简介与社交链接 | 已在 main | `presentation/screens/user/EditProfileSheet.kt`、`UserProfileScreen.kt`、`UserProfileScreenModel.kt` |
| 自己上传模型的资料编辑 | 已在 main | `network/api/avatars/AvatarsApi.kt`、`presentation/screens/avatar/AvatarProfileScreen.kt`、`AvatarProfileScreenModel.kt` |

### 已发现并补回的一项遗漏

旧分支的提交 `c18fc83a` 没有成为当前 `main` 的祖先，导致通知旧接口返回的 `details.emojiId` 曾在合并 V2 通知时被丢掉。它已由当前基线提交 `6b96c2d4` 补回：

- `composeApp/src/commonMain/kotlin/network/api/notification/data/NotificationData.kt`：保留旧通知的 `details`。
- `composeApp/src/commonMain/kotlin/presentation/screens/home/data/NotificationItemData.kt`：优先读取 `details` 内的 Boop 数据。
- `composeApp/src/commonMain/kotlin/presentation/screens/home/HomeScreenModel.kt`：合并旧/V2 通知时不以空字段覆盖表情数据。
- `composeApp/src/commonMain/kotlin/service/IncomingBoopNotificationService.kt`：后台轮询走同一保留逻辑。
- `composeApp/src/commonTest/kotlin/presentation/screens/home/data/BoopNotificationResolverTest.kt`：覆盖旧接口 `details` 的回归测试。

注意：VRChat 服务端若只发送普通 Boop 而没有任何 emoji 字段，客户端只能显示默认“戳一下”；历史通知也无法凭空恢复服务端未提供的表情。

## 2. 遗留分支回查

以下分支**不是应该直接合并的待办**。它们与 `main` 的共同祖先较旧，里面的有效功能大多已经被重放、改写或继续迭代到 `main`；直接合并会引入旧 UI、旧依赖或重复历史。

| 分支 | 回查结果 | 处理规则 |
| --- | --- | --- |
| `codex/room-activity-upgrade` | SQLite 镜像、活动总览、资料页活动样式已经以 `cd23ae2d`、`a5c2fbf6`、`d8b6aab9`、`6830b92c`、`f3956f48` 的后续版本进入 main。 | 不合并整分支。 |
| `codex/momo-notification-target-management` | 单好友/收藏夹通知选择已经在 main 的 `76c9e308` 及后续通知实现中。固定签名部分也已等价进入 main。 | 不合并整分支。 |
| `codex/release-signing` | 固定签名补丁已经等价存在。 | 不合并整分支。 |
| `codex/activity-timeline-baseline`、`codex/lan-vrcx-sync`、`codex/lan-vrcx-sync-main`、`codex/momo-integration`、`codex/presence-normalization`、`codex/ui-foundation`、`codex/vrcx-feature-review` | 都已经是当前 main 的祖先。 | 无需动作。 |
| `origin/codex/friend-activity-notifications` | 混有较长的上游历史、桌面/iOS/身份卡等大量不属于当前 VRCMomo 发行线的改动。 | 禁止整分支合并；若未来需要某个能力，只挑单一提交并重新审计。 |

## 3. 继续开发的精确入口

### A. Boop 发送、接收和表情显示

1. 发送 API：`composeApp/src/commonMain/kotlin/network/api/users/UsersApi.kt`
2. 请求体：`composeApp/src/commonMain/kotlin/network/api/users/data/BoopData.kt`
3. 表情选择 UI：`composeApp/src/commonMain/kotlin/presentation/screens/user/BoopSelectorDialog.kt`
4. 通知 DTO：`composeApp/src/commonMain/kotlin/network/api/notification/data/NotificationData.kt` 和 `NotificationDataV2.kt`
5. 通知解析与内置符号映射：`composeApp/src/commonMain/kotlin/presentation/screens/home/data/NotificationItemData.kt`
6. 前台通知合并：`composeApp/src/commonMain/kotlin/presentation/screens/home/HomeScreenModel.kt`
7. 后台/轮询通知合并：`composeApp/src/commonMain/kotlin/service/IncomingBoopNotificationService.kt`
8. 测试：`composeApp/src/commonTest/kotlin/presentation/screens/home/data/BoopNotificationResolverTest.kt`、`network/api/users/UsersApiBoopTest.kt`

### B. 好友活动、重复记录与时间线

1. 观察 VRChat 好友快照和生成事件：`composeApp/src/commonMain/kotlin/service/FriendActivityTracker.kt`
2. 账号启停、保存节流、导入、保留天数：`composeApp/src/commonMain/kotlin/service/FriendActivityService.kt`
3. JSON 存档读取、schema 迁移、事件键：`composeApp/src/commonMain/kotlin/storage/FriendActivityCacheDao.kt`、`storage/data/FriendActivityCache.kt`
4. Room 镜像、索引、恢复：`composeApp/src/commonMain/kotlin/storage/RoomFriendActivityMirror.kt`、`VrcmomoActivityDatabase.kt`
5. 总览日志页：`composeApp/src/commonMain/kotlin/presentation/screens/home/FriendActivityOverviewScreen.kt`
6. 单好友活动、简介 Diff、见面和时间分布 UI：`composeApp/src/commonMain/kotlin/presentation/screens/user/UserProfileScreen.kt`
7. 纯逻辑回归测试：`composeApp/src/commonTest/kotlin/service/FriendActivityTrackerTest.kt`、`storage/RoomFriendActivityMirrorTest.kt`

### C. Android 通知与后台连接

1. 系统通知总线：`composeApp/src/commonMain/kotlin/service/SocialNotificationService.kt`
2. 游戏在线识别、网页在线不算上线：`composeApp/src/commonMain/kotlin/service/FriendGamePresence.kt`
3. Android 通知渠道、通知样式、权限：`composeApp/src/androidMain/kotlin/presentation/notifications/AndroidPlatformNotificationService.kt`
4. 前台服务和连接状态通知：`composeApp/src/androidMain/kotlin/service/FriendActivityForegroundService.kt`
5. 开机恢复：`composeApp/src/androidMain/kotlin/service/NotificationMonitorBootReceiver.kt`
6. Android 启动、登录后自动同步入口：`composeApp/src/androidMain/kotlin/VRCMApplication.kt`
7. 用户设置入口：`composeApp/src/commonMain/kotlin/presentation/screens/home/sheet/SettingsBottomSheet.kt`

### D. VRCX 导入与电脑/手机桥接

1. 从 VRCX SQLite 只读导出：`tools/export_vrcx_activity.py`
2. 桥接器 GUI、HTTP 接口、UDP 广播：`tools/vrcmomo_lan_bridge.py`
3. 跨设备事件规范化、基线重建、去重：`tools/vrcmomo_activity_merge.py`
4. 合并器测试：`tools/test_vrcmomo_activity_merge.py`
5. Android/共享端解析 VRCX 导出：`composeApp/src/commonMain/kotlin/service/VrcxActivityImport.kt`
6. 配对 URL、拉取、上传、空闲超时：`composeApp/src/commonMain/kotlin/service/LanActivityBridgeClient.kt`
7. UDP 协议：`composeApp/src/commonMain/kotlin/service/LanBridgeDiscovery.kt`
8. Android 局域网发现和扫码：`composeApp/src/androidMain/kotlin/service/LanBridgeDiscovery.android.kt`、`LanBridgeQrScanner.android.kt`
9. 同步设置和导入预览文案：`composeApp/src/commonMain/kotlin/presentation/screens/home/sheet/SettingsBottomSheet.kt`
10. 可发给用户的桥接器：`downloads/VRCMomo-LAN-Bridge.exe`；旧快照兼容版：`downloads/VRCMomo-LAN-Bridge-compat-r1.exe`。

### E. 包、版本、签名与更新通道

1. 版本和 `versionCode`：`gradle/libs.versions.toml`
2. APK 输出名、签名开关：`composeApp/build.gradle.kts`
3. Android 包名、图标、前台服务声明：`composeApp/src/androidMain/AndroidManifest.xml`
4. 测试通道元数据：`downloads/testing-channel.json`
5. 下载页说明：`downloads/README.md`
6. 固定签名规则：`docs/RELEASE_SIGNING.md`
7. 老 0.3.16/0.3.17 测试线迁移规则：`docs/LEGACY_TESTING_TRACK_MIGRATION.md`

测试者升级包必须使用固定签名的 release 构建；普通 Debug 签名不能覆盖已安装的 VRCMomo。构建及验签方式见 `AGENTS.md`。

## 4. 当前仍需真实设备验证的边界

以下不是“遗漏”，而是需要真实 VRChat 事件或不同厂商系统验证，不能仅凭 Kotlin 单测判定完成：

1. 选定的 Boop 表情是否在对方通知详情中真实带回；普通 Boop 与历史旧通知没有表情字段时应回退默认文字。
2. Android 厂商后台策略下，前台服务和通知渠道是否会被用户系统限制；应用内开关、系统通知权限、耗电白名单仍应逐台设备确认。
3. LAN 传输大归档时，电脑和手机必须在可互通的同一局域网；VPN、访客 Wi-Fi、AP 隔离会阻止发现或 HTTP 连接。
4. VRCX 导出中存在但手机尚未有界面的字段会保留在桥接归档中，不应声称全部已在手机客户端展示。

## 5. 本次回查的验证方式

建议在修改上述任一能力后按影响范围执行：

```powershell
Set-Location F:\vrcmoskavis\VRCMomoLanSync

# 活动合并器
python tools/test_vrcmomo_activity_merge.py

# Android/Kotlin 逻辑
.\gradlew.bat :composeApp:testDebugUnitTest

# Android 安装包
.\gradlew.bat :composeApp:assembleDebug

# 最后防止空白或行尾问题
git diff --check
```

当前工作区中 `tools/diagnostic-vrcx-activity.json` 是本地导出诊断文件，不属于发行物；提交时不要使用 `git add .`。

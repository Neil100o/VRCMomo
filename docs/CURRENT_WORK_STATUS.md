# VRCMomo 当前工作状态

最后更新：2026-08-11  
工作目录：`F:\vrcmoskavis\VRCMomoLanSync`  
当前分支：`main`（跟踪 `origin/main`）  
当前版本：`0.3.19`，Android `versionCode = 31`

本文是继续开发时的第一入口，用来区分已经进入 `main` 的功能、尚未提交的本地改动、测试包实际包含的内容，以及下一步应当从哪里继续。不要仅根据页面效果或旧 APK 判断源码状态。

## 1. 已进入 main 的近期功能

以下内容已经提交到当前 `main`：

- 模型资料页管理收藏：提交 `718defe8`。
- 更新元数据、测试通道与 APK 下载地址对齐：提交 `2bad29d8`。
- VRChat 官方链接复制：提交 `9ccbc622`。
- Windows 局域网桥接工具打包：提交 `a867c1be`。
- VRChat 官方链接在应用内解析并打开：提交 `2e0a04d6`。
- 好友关系统计四格化、活动时间图标签和设置/资料页精简：提交 `667e8a5a`、`2e730a0f`、`73a7cfbb`。
- 好友活动持久化、日志时间线去重、VRCX 活动导入、局域网配对与手机/电脑记录汇总等基础能力已经在 `main`，详细路径见 `docs/DEVELOPMENT_MAP.md` 和 `docs/LAN_SYNC_DESIGN.md`。

## 2. 当前尚未提交的本地 UI 批次

这批改动已经完成源码实现并通过 Android 编译、单元测试和 Debug 打包，但仍未提交或发布，最终视觉效果仍需设备检查。

### 已完成或基本完成

| 内容 | 主要路径 | 状态 |
| --- | --- | --- |
| 平板上的官方链接弹窗限制宽度 | `presentation/compoments/OfficialLinkPrompt.kt` | 已修改，等待本批次总体验证 |
| 首页去掉顶部“现在/好友/搜索”标题并压缩头部 | `presentation/screens/home/HomeScreen.kt` | 已修改 |
| 设置总览增加分类图标和底部安全间距 | `presentation/screens/home/sheet/SettingsBottomSheet.kt` | 已修改 |
| 删除 LAN 同步、自动同步和导入区的重复说明 | `presentation/screens/home/sheet/SettingsBottomSheet.kt` | 已修改 |
| “寻找附近电脑”和“扫描配对二维码”同排 | `presentation/screens/home/sheet/SettingsBottomSheet.kt` | 已修改，需在窄屏复核 |
| 群组页错误的 `? BACK` 改为返回图标 | `presentation/screens/group/GroupProfileScreen.kt` | 已修改 |
| Boop 选择器在名称旁显示可识别符号 | `presentation/screens/user/BoopSelectorDialog.kt` | 已修改，之前的引号编译错误已修复 |
| 编辑自己资料时支持社交链接，一行一个 URL | `presentation/screens/user/EditProfileSheet.kt`、`UserProfileScreen.kt` | 已修改 |
| 修复自己的资料页被当成好友页 | `presentation/screens/user/UserProfileScreenModel.kt`、`UserProfileScreen.kt` | 已修改，通过账号 ID 判断 `isSelf` |
| 活动时间图精简说明并改善格子/时间标签可读性 | `presentation/screens/user/UserProfileScreen.kt` | 已修改 |
| 最近访问世界改为响应式圆角封面卡片 | `presentation/screens/world/RecentWorldsScreen.kt`、`presentation/compoments/AdaptiveCardLayout.kt` | 已完成；手机双列，宽屏按可用宽度增加为三至四列 |
| 收藏/搜索世界改为响应式圆角封面卡片 | `presentation/compoments/SearchItemRenderers.kt`、`StandardSearchList.kt` | 已完成；显示名称、作者和真实平台角标，宽屏不再固定双列拉伸 |
| 收藏/搜索模型改为响应式圆角封面卡片 | `presentation/compoments/SearchItemRenderers.kt`、`StandardSearchList.kt` | 已完成；显示名称、作者和真实平台角标，宽屏不再固定双列拉伸 |
| 世界与模型平台角标统一组件 | `presentation/compoments/PlatformBadges.kt` | 已完成 |
| 好友列表改为响应式圆角联系人卡 | `presentation/compoments/SearchItemRenderers.kt`、`AdaptiveCardLayout.kt` | 已完成；窄屏单列，宽屏二至四列，保留头像、信任等级、名字和状态；群组成员和共同好友页共用同一分栏规则 |
| Lab Debug 包固定签名 | `composeApp/build.gradle.kts` | 已完成；配置本地发布密钥时 Debug 包也使用固定签名，避免不同 Gradle 环境产生覆盖安装冲突 |
| 资料页封面下拉约束 | `presentation/compoments/ProfileScaffold.kt` | 已完成；内容未回到顶部时禁止展开封面，避免拖动封面后资料区停留在空白滚动位置 |
| 好友“常联系”本地排序 | `presentation/screens/home/pager/FriendListPagerModel.kt`、`FriendListPager.kt` | 已完成；默认启用，可切换状态、最近见面和名称排序 |
| LAN 发现/扫码和拉取/上传按钮分别同排 | `presentation/screens/home/sheet/SettingsBottomSheet.kt` | 已完成，仍需窄屏视觉复核 |
| 剪贴板官方链接只提示一次 | `presentation/compoments/OfficialLinkPrompt.kt`、`storage/SettingsDao.kt` | 已完成；目标键持久化，应用重启后相同链接不重复询问 |

### 本批次之后仍未实现

1. **回忆页面内容**  
   当前回忆入口主要仍是 Gallery。把最近世界或其他记忆数据纳入回忆页尚未实现，需要另开独立改动，避免和本次列表样式重构混在一起。

## 3. 当前验证边界

- Android Kotlin 编译通过：`:composeApp:compileDebugKotlinAndroid`。
- Android 单元测试通过：包含好友常联系与最近见面排序测试，0 失败。
- Debug APK 打包通过：`:composeApp:assembleDebug`。
- 当前完整测试 APK：  
  `F:\vrcmoskavis\VRCMomoLanSync\testing\local-apks\VRCMomo-Lab-v0.3.17-adaptive-cards.apk`
- 正式 Release 尚未更新；Debug 应用名为 `VRCMomo Lab`，正式包为 `VRCMomo`。

## 4. 下一步固定顺序

1. 安装到 MuMu，逐页检查：
   - 搜索页世界列表；
   - 收藏页世界列表；
   - 搜索/收藏模型列表；
   - 最近世界；
   - 自己的资料页菜单；
   - 编辑社交链接；
   - Boop 选择器；
   - LAN 同步设置；
   - 群组页返回按钮；
   - 竖屏手机与宽屏/平板布局。
2. 用户确认效果后，再按关注点拆分提交；不要把构建日志、`testing/`、`tools/__pycache__/` 或 `tools/vrcmomo-lan-inbox/` 混入提交。

## 5. 数据兼容和发布约束

- UI 重构不得改变好友活动存档结构。
- 修改序列化模型时，必须增加明确迁移，遵守 `FriendActivityCache.CURRENT_SCHEMA_VERSION` 规则，确保旧手机记录不会因更新消失。
- 局域网同步继续采用事件指纹和账号范围去重，不直接累加其他设备的汇总统计。
- VRCM 是上游基础；README 和发布说明必须区分 VRCM 团队原始功能与 VRCMomo 的增量改动。
- 在完整构建、MuMu 实机检查和用户确认前，不升级到 `1.0`，也不更新正式下载通道。

## 6. 工作区清洁提醒

当前未跟踪项包含本地构建日志、测试包、Python 缓存和 LAN 收件箱。这些是本地测试产物，不应使用 `git add .` 直接提交。提交时只添加明确检查过的源码和文档路径。

# Android 通知可靠性方案

本文约束 VRCMomo Android 端的通知入口、实时连接、漏事件补偿和持久化边界。实现时先读本文，再按末尾路径表定位，避免重新扫描仓库。

## 目标

- Android 只提供一个“系统通知”总开关。
- 开启后启动前台监测，并接收 Boop、好友请求、群组消息、成为好友、删除好友、好友改名、VRChat 服务异常及用户选定范围内的进入/离开 VRChat 通知。
- WebSocket 提供低延迟事件；HTTP 定时核对负责修复断线期间遗漏的事件。
- 常驻通知显示本次连续连接时间；断线时冻结计时并显示正在重连。
- 所有业务事件先去重再通知。进程重建、断线重连和完整列表刷新不得制造重复通知。
- 不把 VRChat 登录凭据上传到 VRCMomo 自有服务器。本阶段保持纯本地实现。

## 非目标与边界

- Android 强行停止应用、厂商彻底冻结进程后，纯本地方案无法保证实时送达。
- FCM 或厂商推送只能负责把服务器已有事件送到设备；若未来采用云推送，还需要可信后端代为保持 VRChat 会话，涉及账号安全、隐私和维护成本，当前不实现。
- 好友网页在线仍不算进入 VRChat；从游戏在线切换到网页在线视为离开 VRChat。

## 单一开关

`SettingsData.isSystemNotificationsEnabled` 是 Android 通知唯一用户开关：

- 开启：请求通知权限，保存设置，启动 `FriendActivityForegroundService`。
- 关闭：停止前台服务，不再产生系统通知。
- 旧版 `isBackgroundFriendMonitoringEnabled` 仅用于一次兼容迁移；新 UI 不再单独展示。
- 收藏夹/单个好友的选择只决定哪些好友产生上下线通知，不控制服务是否运行。

## 连接与补偿链路

```text
VRChat WebSocket ──实时触发──┐
                            ├─> 统一事件解析/去重 ─> Android 高优先级通知
通知 API 定时核对 ──补漏────┤
好友完整列表刷新 ──补漏─────┘
```

建议节奏：

- WebSocket：持续连接，失败后重连。
- 通知 API：服务启动、WebSocket 状态改变时立即核对；连接正常时每 5 分钟补漏，断线时每 60 秒核对一次，避免正常状态下制造大量 API 请求。
- 好友完整列表：后台每 120 秒刷新一次。
- 网络调用使用互斥锁，避免 WebSocket 触发和定时任务并发重复请求。

## 常驻通知状态

常驻通知使用低重要性通道，业务事件使用高重要性通道。

- 正在连接：`正在连接`
- 已连接：系统 Chronometer 显示本次连续连接时间，不由应用每秒刷新。
- 已断开：冻结上次连续连接时长，并显示 `连接中断 · 正在重连`。
- 重连成功：计时从零开始。

WebSocket 层发布结构化连接状态，前台服务只负责渲染状态；不得根据日志字符串猜测连接状态。

## 事件范围

| 事件 | 快速路径 | 补偿路径 | 去重依据 |
| --- | --- | --- | --- |
| Boop | WebSocket notification | 通知 API | notification ID |
| 好友请求 | WebSocket notification | 通知 API | notification ID |
| 群组公告/活动/管理消息 | WebSocket notification | 通知 API | notification ID |
| 成为好友 | friend-add | 好友集合差异 | user ID + 变化时间窗 |
| 删除好友 | friend-delete | 好友集合差异 | user ID + 变化时间窗 |
| 好友改名 | 好友资料更新 | 完整好友状态 | user ID + 前后名称 |
| 进入/离开 VRChat | friend presence 事件 | 完整好友状态 | user ID + 游戏在线布尔状态 |
| VRChat 服务异常/恢复 | Statuspage API | 每 5 分钟核对 | 前后 indicator |

邀请、请求邀请和社交状态文字变化不产生 Android 系统通知。群组邀请同样排除。

后续统一事件收件箱需要持久化 `eventId/type/userId/createdAt/notifiedAt/openedAt/payload`。在收件箱落地前，不得仅凭内存集合承担跨重启去重。

## 已知风险

- Android 13 及以上用户可从“活动应用”停止前台服务；被用户停止后系统不会立即自动恢复。
- 国内厂商后台限制仍需用户允许后台耗电、锁定后台或加入白名单。
- 通知渠道重要性创建后不能由应用提升；若旧渠道曾设置过低，需要使用新渠道 ID 或引导用户进入系统设置。

## 实现路径

| 关注点 | 路径 |
| --- | --- |
| 前台服务 | `composeApp/src/androidMain/kotlin/service/FriendActivityForegroundService.kt` |
| Android 通知构建 | `composeApp/src/androidMain/kotlin/presentation/notifications/AndroidPlatformNotificationService.kt` |
| WebSocket 生命周期 | `composeApp/src/commonMain/kotlin/network/websocket/WebSocketApi.kt` |
| Boop 后台核对 | `composeApp/src/commonMain/kotlin/service/IncomingBoopNotificationService.kt` |
| 好友通知 | `composeApp/src/commonMain/kotlin/service/SocialNotificationService.kt` |
| VRChat 状态监测 | `composeApp/src/commonMain/kotlin/service/VrchatStatusNotificationService.kt`、`network/api/status/` |
| 总开关设置 | `composeApp/src/commonMain/kotlin/storage/data/SettingsData.kt`、`storage/SettingsDao.kt` |
| 设置 UI | `composeApp/src/commonMain/kotlin/presentation/screens/home/sheet/SettingsBottomSheet.kt` |
| Android 启停入口 | `composeApp/src/androidMain/kotlin/AppPlatform.android.kt`、`VRCMApplication.kt` |

## 实施顺序

1. WebSocket 连接状态与常驻通知计时。
2. Boop 每分钟补偿和重连立即核对。
3. 合并 Android 总开关并兼容旧设置。
4. 通知 API 扩展到好友请求；好友集合扩展到新增/删除好友。
5. 持久化统一事件收件箱及应用内通知总览。

## 当前落地状态（2026-08-11）

- 已实现单一 Android 通知总开关，并以旧版两个开关同时开启作为安全迁移条件。
- 已实现 WebSocket `Idle / Connecting / Connected / Disconnected` 状态流。
- 已实现常驻通知 Chronometer；断线后冻结连续连接时长并显示重连状态。
- 已实现 Boop/好友请求通知 API 自适应补偿：连接正常每 5 分钟、断线每 60 秒，并在连接状态变化时立即核对。
- 已实现好友完整列表的新增/删除差异通知；缓存好友 ID 可作为冷启动关系基线。
- 已实现群组公告、活动及管理消息通知，并排除群组邀请。
- 已实现好友显示名变化通知。
- 已实现 VRChat Statuspage 每 5 分钟核对，并在异常与恢复时分别通知。
- 已实现最近 256 个通知 ID 的跨进程持久化去重。
- 已注册开机完成和应用更新后的监测恢复入口。
- 待实现完整事件收件箱；当前跨重启去重仍使用有界通知 ID 集合。

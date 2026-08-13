# VRCMomo 0.3.21

发布日期：2026-08-13
版本：`0.3.21`
Android `versionCode`：`33`
发行通道：固定签名 GitHub Release

## 本次热修复

修复旧 VRChat 通知接口中 Boop 表情元数据在旧通知和 V2 通知合并时被空字段覆盖的问题。

修改覆盖以下三个入口：

- 前台通知列表：`composeApp/src/commonMain/kotlin/presentation/screens/home/HomeScreenModel.kt`
- 应用内 Boop 内容解析：`composeApp/src/commonMain/kotlin/presentation/screens/home/data/NotificationItemData.kt`
- 后台 Boop 轮询通知：`composeApp/src/commonMain/kotlin/service/IncomingBoopNotificationService.kt`

旧通知 DTO 现在保留 `details`，并优先从其中读取 `emojiId`、`emojiVersion` 与 `inventoryItemId`。如果 VRChat 服务端发送的是没有 emoji 字段的普通 Boop，应用会正常回退为默认“戳一下”；历史记录中服务端从未提供的表情无法恢复。

## 本次发行包含的文件

| 文件 | 用途 |
| --- | --- |
| `VRCMomo-v0.3.21.apk` | Android 固定签名正式版。 |
| `VRCMomo-LAN-Bridge.exe` | Windows 局域网桥接器，用于 VRCX 只读导出、手机活动归档和旧记录迁移。 |
| `Allow-VRCMomoLanBridgeFirewall.bat` | 为桥接器添加局域网 TCP `38671` 和 UDP `38672` 的 Windows 防火墙放行规则。 |
| `VRCMomo-v0.3.21-feature-log.md` | 本发行与此前功能的简要日志。 |

## 旧 0.3.16 测试轨用户

旧测试轨不能直接覆盖安装本发行版。请先使用 `VRCMomo-v0.3.20-legacy-migration.apk` 覆盖旧应用，在桥接器中同步一次活动记录，再安装本发行版并拉取归档。

完整操作见 [INSTALL_AND_MIGRATION.md](INSTALL_AND_MIGRATION.md)。迁移完成前不要卸载旧应用。

## 更新检查

固定签名客户端会优先检查 GitHub Releases。旧签名测试轨仍固定收到 `0.3.20` 迁移包，避免在未备份旧私有记录时跳转到不能覆盖安装的新签名版本。

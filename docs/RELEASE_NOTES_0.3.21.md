# VRCMomo 0.3.21

Date / 日期 / 日付：2026-08-13<br>
Version / 版本 / バージョン：`0.3.21`<br>
Android `versionCode`：`33`<br>
Channel / 通道 / チャンネル：Permanent-signature GitHub Release / 固定签名 GitHub Release / 固定署名 GitHub Release

## English

### Hotfix

Fixes a legacy VRChat notification merge path that could overwrite Boop emoji metadata with empty V2 fields. The following paths now share the same preservation logic:

- Foreground notification list: `composeApp/src/commonMain/kotlin/presentation/screens/home/HomeScreenModel.kt`
- In-app Boop parser: `composeApp/src/commonMain/kotlin/presentation/screens/home/data/NotificationItemData.kt`
- Background Boop polling: `composeApp/src/commonMain/kotlin/service/IncomingBoopNotificationService.kt`

Legacy notification DTOs retain `details` and prefer its `emojiId`, `emojiVersion` and `inventoryItemId`. Plain Boops without server emoji data still use the default label; unavailable historical metadata cannot be reconstructed.

### Release files

| File | Purpose |
| --- | --- |
| `VRCMomo-v0.3.21.apk` | Android permanent-signature release. |
| `VRCMomo-LAN-Bridge.exe` | Windows bridge for VRCX read-only export, phone activity archiving and legacy migration. |
| `Allow-VRCMomoLanBridgeFirewall.bat` | Adds LAN-only TCP `38671` and UDP `38672` Windows Firewall rules. |
| `VRCMomo-v0.3.21-feature-log.md` | Feature log for this release and prior recent changes. |

Old 0.3.16 testing-track users must first update to `VRCMomo-v0.3.20-legacy-migration.apk`, sync once through the bridge, then install this release and pull the archive. Do not uninstall the old app first.

## 简体中文

### 热修复

修复旧 VRChat 通知接口中 Boop 表情元数据在旧通知和 V2 通知合并时被空字段覆盖的问题。以下入口现在共用同一份保留逻辑：

- 前台通知列表：`composeApp/src/commonMain/kotlin/presentation/screens/home/HomeScreenModel.kt`
- 应用内 Boop 解析：`composeApp/src/commonMain/kotlin/presentation/screens/home/data/NotificationItemData.kt`
- 后台 Boop 轮询：`composeApp/src/commonMain/kotlin/service/IncomingBoopNotificationService.kt`

旧通知 DTO 现在保留 `details`，并优先读取其中的 `emojiId`、`emojiVersion` 与 `inventoryItemId`。服务端没有表情字段的普通 Boop 会正常回退为默认“戳一下”；历史记录中服务端从未提供的表情无法恢复。

### 本次发行文件

| 文件 | 用途 |
| --- | --- |
| `VRCMomo-v0.3.21.apk` | Android 固定签名正式版。 |
| `VRCMomo-LAN-Bridge.exe` | Windows 局域网桥接器，用于 VRCX 只读导出、手机活动归档和旧记录迁移。 |
| `Allow-VRCMomoLanBridgeFirewall.bat` | 为桥接器添加局域网 TCP `38671` 和 UDP `38672` 的 Windows 防火墙规则。 |
| `VRCMomo-v0.3.21-feature-log.md` | 本发行与此前近期改动的功能日志。 |

旧 0.3.16 测试轨需要先更新 `VRCMomo-v0.3.20-legacy-migration.apk`，通过桥接器同步一次后再安装本发行版并拉取归档。迁移前不要卸载旧应用。

## 日本語

### ホットフィックス

旧 VRChat 通知フィードと V2 通知の結合時に、Boop 絵文字情報が空フィールドで上書きされる問題を修正しました。以下の入口で同じ保持処理を使います。

- 前景通知一覧：`composeApp/src/commonMain/kotlin/presentation/screens/home/HomeScreenModel.kt`
- アプリ内 Boop 解析：`composeApp/src/commonMain/kotlin/presentation/screens/home/data/NotificationItemData.kt`
- バックグラウンド Boop ポーリング：`composeApp/src/commonMain/kotlin/service/IncomingBoopNotificationService.kt`

旧通知 DTO は `details` を保持し、`emojiId`、`emojiVersion`、`inventoryItemId` を優先します。サーバーから絵文字情報が来ない通常の Boop は既定表示になります。過去にサーバーが送っていない絵文字は復元できません。

### 同梱ファイル

| ファイル | 用途 |
| --- | --- |
| `VRCMomo-v0.3.21.apk` | Android 固定署名正式版。 |
| `VRCMomo-LAN-Bridge.exe` | VRCX の読み取り専用エクスポート、スマートフォン活動アーカイブ、旧記録移行のための Windows ブリッジ。 |
| `Allow-VRCMomoLanBridgeFirewall.bat` | LAN 用 TCP `38671` と UDP `38672` の Windows ファイアウォール規則を追加します。 |
| `VRCMomo-v0.3.21-feature-log.md` | 本 Release と直近変更の機能ログ。 |

旧 0.3.16 テストトラックは、まず `VRCMomo-v0.3.20-legacy-migration.apk` に更新し、ブリッジで一度同期してから本 Release をインストールし、アーカイブを取得してください。移行前に旧アプリを削除しないでください。

For the complete guide, see [INSTALL_AND_MIGRATION.md](INSTALL_AND_MIGRATION.md) / 完整步骤见 [INSTALL_AND_MIGRATION.md](INSTALL_AND_MIGRATION.md) / 詳細は [INSTALL_AND_MIGRATION.md](INSTALL_AND_MIGRATION.md) を参照してください。

# VRCMomo Feature Log / 功能日志 / 機能ログ

This log records user-visible VRCMomo changes. It covers VRCMomo additions, maintenance and fixes only; pre-existing upstream VRCM functionality is not relisted as VRCMomo work.

本日志记录 VRCMomo 面向用户的改动，只描述 VRCMomo 自己的新增、维护和修复；上游 VRCM 已有能力不会被重复归为 VRCMomo 新功能。

このログは VRCMomo の利用者向け変更を記録します。VRCMomo 自身の追加・保守・修正のみを記載し、上流 VRCM の既存機能を VRCMomo の新機能としては扱いません。

## 0.3.23 - 2026-08-13

### 简体中文

- 修复 LAN Bridge 同步的 Ktor 3 超时配置错误；大归档可持续传输，断开连接仍会在 120 秒无数据后报错。
- Boop 系统通知仅在标题标出表情，去掉重复的表情说明行。
- 固定签名 Release 启用 R8 代码压缩和 Android 资源压缩，通用 APK 从约 17.9 MB 缩小到约 3.7 MB。

### English

- Fixed the Ktor 3 timeout configuration in LAN Bridge sync. Large archives can continue transferring, while dead connections still fail after 120 seconds without data.
- Boop system notifications now show the emoji only in the title and remove the duplicate message line.
- Enabled R8 code shrinking and Android resource shrinking for signed Releases, reducing the universal APK from about 17.9 MB to about 3.7 MB.

### 日本語

- LAN Bridge 同期の Ktor 3 タイムアウト設定を修正しました。大きなアーカイブは継続転送でき、データが 120 秒来ない接続は失敗として扱います。
- Boop システム通知はタイトルだけに絵文字を表示し、重複していた説明行を削除しました。
- 固定署名 Release で R8 コード縮小と Android リソース縮小を有効にし、ユニバーサル APK を約 17.9 MB から約 3.7 MB に縮小しました。
## 0.3.22 - 2026-08-13

### 简体中文

- Android 包名改为独立的 `io.github.neil100o.vrcmomo`，不再与上游 VRCM 的安装身份冲突，可以共存。
- 继续沿用 VRCMomo 固定发行签名；版本升为 `0.3.22`，Android `versionCode = 34`。
- 新包名属于独立应用：从 VRCM、0.3.21 或更早 VRCMomo 迁移本地活动记录时，请先使用 LAN Bridge 建立归档，再在新版本拉取。
- 保留 0.3.21 的 Boop 表情元数据修复。

### English

- Moved to the standalone Android package `io.github.neil100o.vrcmomo`, so VRCMomo no longer conflicts with upstream VRCM and both can coexist.
- Keeps the permanent VRCMomo signing key; version is now `0.3.22` with Android `versionCode = 34`.
- The new package is a separate app. To move local activity from VRCM, 0.3.21 or earlier VRCMomo, create an archive through LAN Bridge first and then pull it in the new app.
- Retains the 0.3.21 Boop emoji metadata fix.

### 日本語

- 独立した Android パッケージ `io.github.neil100o.vrcmomo` に変更し、上流 VRCM と競合せず共存できるようにしました。
- VRCMomo の固定署名を継続使用し、`0.3.22`、Android `versionCode = 34` に更新しました。
- 新パッケージは別アプリです。VRCM、0.3.21 以前の VRCMomo からローカル活動記録を移す場合は、先に LAN Bridge でアーカイブを作成してから新アプリで取得してください。
- 0.3.21 の Boop 絵文字メタデータ修正を維持します。
## 0.3.21 - 2026-08-13

### 简体中文

- 修复旧通知接口中的 Boop 表情元数据在旧/V2 通知合并时被空字段覆盖的问题。
- 前台通知列表、应用内 Boop 卡片和后台通知轮询现在共用同一份表情数据保留逻辑。
- 旧接口确实提供了 `details.emojiId` 时，会优先显示对应表情；服务端没有发送表情的普通 Boop 仍显示默认“戳一下”。
- 固定签名发行版升为 `0.3.21`，Android `versionCode = 33`；Release 标签改为标准 `v0.3.21`，应用内可正常比较后续正式版更新。
- Release 同时提供 Android APK、Windows LAN Bridge、LAN 防火墙辅助脚本和本功能日志。

### English

- Fixed a legacy notification merge that could overwrite Boop emoji metadata with empty V2 fields.
- The foreground notification list, in-app Boop card and background notification polling now preserve the same emoji data.
- If the legacy feed provides `details.emojiId`, VRCMomo uses it first. Plain Boops without server emoji data still use the default label.
- The permanent-signature release is now `0.3.21` with Android `versionCode = 33`. Release tags use normal semantic versions so later release updates compare correctly in-app.
- This Release includes the Android APK, Windows LAN Bridge, LAN firewall helper and this feature log.

### 日本語

- 旧通知フィードと V2 通知を結合するとき、Boop 絵文字情報が空の V2 フィールドで上書きされる問題を修正しました。
- 前景の通知一覧、アプリ内 Boop カード、バックグラウンド通知ポーリングで同じ絵文字保持処理を使用します。
- 旧フィードに `details.emojiId` がある場合はそれを優先します。サーバーから絵文字が来ない通常の Boop は既定表示になります。
- 固定署名版は `0.3.21`、Android `versionCode = 33` です。Release タグを通常のセマンティックバージョンに修正し、アプリ内の更新比較を正常化しました。
- この Release には Android APK、Windows LAN Bridge、LAN ファイアウォール補助、機能ログを含みます。

## 0.3.20 - 2026-08-12

### 简体中文

- 为旧自动更新轨提供兼容旧签名的迁移包，避免旧应用私有活动记录直接丢失。
- LAN Bridge 支持 VRCX SQLite 只读导出、二维码/局域网发现、活动归档重建与手机上传下载。
- 大归档使用连接超时加空闲超时，而不是总传输时限；传输持续有数据时不会因总时间过长中断。
- 桥接器兼容缺少 `pronouns` 字段的旧好友快照，并在同步预览中区分新增和已存在的 VRCX 记录。
- 多设备归档以稳定事件指纹去重，统计采用最大基线合并，避免重复同步累加共同游玩时长和次数。
- 好友活动历史以 JSON 持久化并镜像到 SQLite，支持恢复、导入、筛选、资料 Diff、活动时间图和关系统计。
- Android 通知支持 Boop、选择的好友上下线、好友请求、好友增删、群组事件和 VRChat 服务异常。
- 更新世界、模型、好友列表为响应式卡片，支持官方 VRChat 链接解析与剪贴板一次性询问。

### English

- Added a legacy-signed migration build so old automatic-update installs can preserve private activity records.
- The LAN Bridge supports read-only VRCX SQLite export, QR/LAN discovery, archive rebuilding and phone upload/download.
- Large archives use connection plus idle timeouts rather than a total transfer deadline.
- The bridge accepts old friend snapshots without `pronouns` and the preview distinguishes new versus already-imported VRCX records.
- Event fingerprints and maximum-baseline merging make repeated multi-device sync idempotent.
- Friend activity is persisted as JSON and mirrored to SQLite, with recovery, filtering, profile diffs, activity time charts and relationship statistics.
- Android notifications cover Boop, selected friend presence, friend requests, friend add/remove, group events and VRChat service errors.
- World, avatar and friend lists use responsive cards; official VRChat links can be detected from the clipboard and opened in-app.

### 日本語

- 旧自動更新トラック向けに、活動履歴を保持する旧署名互換の移行 APK を追加しました。
- LAN Bridge は VRCX SQLite の読み取り専用エクスポート、QR/LAN 検出、アーカイブ再構築、スマートフォンとの送受信に対応します。
- 大きなアーカイブは総転送時間ではなく接続とアイドル時間で判定します。
- `pronouns` のない旧フレンドスナップショットに対応し、同期プレビューで新規/既存の VRCX 記録を区別します。
- イベント識別子と最大基線マージにより、複数端末で繰り返し同期しても統計を水増ししません。
- フレンド活動は JSON と SQLite に保存され、復元、絞り込み、プロフィール差分、活動時間図、関係統計に対応します。
- Android 通知は Boop、選択フレンドのオンライン状態、フレンド申請、フレンド増減、グループイベント、VRChat サービスエラーを扱います。
- ワールド、アバター、フレンド一覧をレスポンシブカード化し、公式 VRChat リンクをクリップボードから検出してアプリ内で開けます。

## Migration / 迁移 / 移行

Old 0.3.16 automatic-update installs and permanent releases use different Android signatures. Read [the installation and migration guide](docs/INSTALL_AND_MIGRATION.md), synchronize the old records through the bridge, and only then remove the old app.

旧 0.3.16 自动更新轨与 0.3.20/0.3.21 正式发行版签名不同。请先阅读 [安装与迁移说明](docs/INSTALL_AND_MIGRATION.md)，用桥接器同步旧记录后再删除旧应用。

旧 0.3.16 自動更新版と 0.3.20/0.3.21 正式版は Android 署名が異なります。[インストールと移行ガイド](docs/INSTALL_AND_MIGRATION.md) を読み、ブリッジで旧記録を同期してから旧アプリを削除してください。

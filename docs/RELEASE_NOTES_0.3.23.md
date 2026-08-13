# VRCMomo 0.3.23

Date / 日期 / 日付：2026-08-13<br>
Version / 版本 / バージョン：`0.3.23`<br>
Android `versionCode`：`35`<br>
Package / 包名 / パッケージ：`io.github.neil100o.vrcmomo`

## English

- Fixed LAN Bridge syncing: Ktor 3 rejects a `0` request timeout. The bridge now uses Ktor's proper infinite request-timeout value, while keeping a 120-second idle timeout for dead connections.
- Simplified Boop system notifications: the selected emoji remains in the title, and the duplicate “used Heart emoji” message line is removed.
- Enabled R8 code shrinking and Android resource shrinking for release builds, matching the current upstream release strategy. The signed universal APK is reduced from about 17.9 MB to about 3.7 MB.

## 简体中文

- 修复 LAN Bridge 同步：Ktor 3 不允许将请求超时设为 `0`。现在使用 Ktor 正确的无限请求超时，同时保留 120 秒空闲超时，用于识别真正断开的连接。
- 精简 Boop 系统通知：表情仍显示在标题里，去掉重复又神秘的“使用了 Heart 表情”一行。
- 发行构建启用 R8 代码压缩与 Android 资源压缩，和当前上游发行策略一致。固定签名通用 APK 从约 17.9 MB 降至约 3.7 MB。

## 日本語

- LAN Bridge 同期を修正しました。Ktor 3 ではリクエストタイムアウトに `0` を設定できないため、正しい無期限値を使用しつつ、切断検知用の 120 秒アイドルタイムアウトを維持します。
- Boop のシステム通知を整理しました。選択した絵文字はタイトルに残し、重複していた「Heart 絵文字を使用しました」の行を削除しました。
- リリースビルドで R8 コード縮小と Android リソース縮小を有効にしました。現在の上流リリース戦略と同じで、固定署名のユニバーサル APK は約 17.9 MB から約 3.7 MB になりました。

## Files / 文件 / ファイル

| File | Purpose / 用途 / 用途 |
| --- | --- |
| `VRCMomo-v0.3.23.apk` | Android standalone fixed-signature release / Android 独立包名固定签名正式版 / Android 独立パッケージ固定署名版 |
| `VRCMomo-LAN-Bridge.exe` | Windows LAN Bridge / Windows 局域网桥接器 / Windows LAN ブリッジ |
| `Allow-VRCMomoLanBridgeFirewall.bat` | LAN firewall helper / 局域网防火墙辅助 / LAN ファイアウォール補助 |
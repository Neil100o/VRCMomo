# VRCMomo downloads / 下载说明 / ダウンロード説明

## English

The files in this directory exist for the old automatic-update-track migration. New users should download the current fixed-signature Android APK and Windows LAN Bridge from [GitHub Releases](https://github.com/Neil100o/VRCMomo/releases/latest).

- `VRCMomo-v0.3.20-legacy-migration.apk`: install over the old 0.3.16 testing app, then complete one LAN Bridge sync to preserve phone activity history before moving to a permanent-signature release.
- `VRCMomo-LAN-Bridge.exe`: Windows LAN bridge. Run it on the PC with VRCX, then pair VRCMomo on the same LAN. Python and QR support are included.

The bridge reads VRCX activity without copying cookies, passwords, tokens, notes or moderation data. It does not write to VRCX native SQLite tables.

## 简体中文

此目录中的文件只用于旧自动更新轨迁移。新用户请从 [GitHub Releases](https://github.com/Neil100o/VRCMomo/releases/latest) 下载当前固定签名 Android APK 与 Windows 局域网桥接器。

- `VRCMomo-v0.3.20-legacy-migration.apk`：覆盖旧 0.3.16 测试版安装，再完成一次 LAN Bridge 同步，保存手机活动记录后再迁移到固定签名正式版。
- `VRCMomo-LAN-Bridge.exe`：Windows 局域网桥接器。在装有 VRCX 的电脑运行，再从同一局域网中的 VRCMomo 配对；已内置 Python 与二维码支持。

桥接器只读 VRCX 活动数据，不复制 Cookie、密码、令牌、备注或管理数据，也不会写入 VRCX 原始 SQLite 表。

## 日本語

このディレクトリのファイルは旧自動更新トラックの移行専用です。新規利用者は [GitHub Releases](https://github.com/Neil100o/VRCMomo/releases/latest) から現在の固定署名 Android APK と Windows LAN ブリッジをダウンロードしてください。

- `VRCMomo-v0.3.20-legacy-migration.apk`：旧 0.3.16 テスト版に上書きし、LAN Bridge で一度同期してスマートフォンの活動履歴を保管してから固定署名版へ移行します。
- `VRCMomo-LAN-Bridge.exe`：Windows LAN ブリッジ。VRCX のある PC で起動し、同一 LAN の VRCMomo からペアリングします。Python と QR サポートを含みます。

ブリッジは VRCX の活動データを読み取り専用で扱い、Cookie、パスワード、トークン、メモ、モデレーション情報をコピーせず、VRCX の元 SQLite テーブルも書き換えません。

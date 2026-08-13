# VRCMomo 0.3.22

Date / 日期 / 日付：2026-08-13<br>
Version / 版本 / バージョン：`0.3.22`<br>
Android `versionCode`：`34`<br>
Package / 包名 / パッケージ：`io.github.neil100o.vrcmomo`

## English

### Standalone Android identity

VRCMomo now uses its own Android application ID, `io.github.neil100o.vrcmomo`. It no longer conflicts with upstream VRCM, so both apps can be installed on one device. The release keeps the existing permanent VRCMomo signing key.

Because Android treats the new application ID as a separate app, data from VRCM or VRCMomo 0.3.21 and earlier is not read automatically. Use the Windows LAN Bridge once to archive old records, install this version, pair it with the same bridge and pull the archive.

### Included fix

This package includes the Boop notification metadata fix from 0.3.21: legacy `details.emojiId` is retained while legacy and V2 notifications are merged.

## 简体中文

### 独立 Android 身份

VRCMomo 现在使用自己的 Android 包名 `io.github.neil100o.vrcmomo`，不会再和上游 VRCM 冲突；两者可以同时安装。本发行继续使用既有的 VRCMomo 固定签名。

Android 会把新包名视为独立应用，因此 VRCM 或 0.3.21 及更早 VRCMomo 的本地记录不会自动读取。请先用 Windows LAN Bridge 同步一次旧记录，再安装本版本、连接同一桥接器并拉取归档。

### 包含的修复

本包包含 0.3.21 的 Boop 通知元数据修复：旧通知与 V2 通知合并时会保留 `details.emojiId`。

## 日本語

### 独立した Android ID

VRCMomo は独自の Android パッケージ `io.github.neil100o.vrcmomo` を使用するようになりました。上流 VRCM と競合せず、同じ端末に共存できます。このリリースは既存の VRCMomo 固定署名を引き続き使用します。

Android では新しいパッケージを別アプリとして扱うため、VRCM または 0.3.21 以前の VRCMomo のローカル記録は自動では読み込まれません。Windows LAN Bridge で旧記録を一度同期し、本版をインストールして同じブリッジからアーカイブを取得してください。

### 含まれる修正

この APK には 0.3.21 の Boop 通知メタデータ修正が含まれます。旧通知と V2 通知を結合する際に `details.emojiId` を保持します。

## Files / 文件 / ファイル

| File | Purpose / 用途 / 用途 |
| --- | --- |
| `VRCMomo-v0.3.22.apk` | Android standalone fixed-signature release / Android 独立包名固定签名正式版 / Android 独立パッケージ固定署名版 |
| `VRCMomo-LAN-Bridge.exe` | Windows bridge for history migration and PC-phone aggregation / Windows 迁移与手机电脑汇总桥接器 / Windows 記録移行・PC/スマホ統合ブリッジ |
| `Allow-VRCMomoLanBridgeFirewall.bat` | LAN firewall helper / 局域网防火墙辅助 / LAN ファイアウォール補助 |
| `VRCMomo-v0.3.22-feature-log.md` | Feature log / 功能日志 / 機能ログ |

[Install and migration guide / 安装与迁移指南 / インストールと移行ガイド](INSTALL_AND_MIGRATION.md)
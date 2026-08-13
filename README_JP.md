<div align="center">

# <img src="image/VRCMomoLogo.png" width="50" height="50" alt="VRCMomo logo" /> VRCMomo

[English](README.md) | [中文](README_ZH.md) | [日本語](README_JP.md)

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Neil100o/VRCMomo)](https://github.com/Neil100o/VRCMomo/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Neil100o/VRCMomo/total)](https://github.com/Neil100o/VRCMomo/releases/latest)

</div>

VRCMomo は [VRCM](https://github.com/vrcm-team/VRCM) を基に継続開発している Android 優先の VRChat Companion です。フレンド、グループ、ワールド、Gallery のモバイル機能に、ローカル活動履歴、通知、Boop、VRCX 活動履歴の移行、LAN アーカイブを追加します。

VRCMomo は VRChat Inc. とは無関係です。VRChat の Cookie、パスワード、トークン、メモ、モデレーション情報をアップロードしません。LAN ブリッジは自分の PC とスマートフォンの間だけで動作します。

## ダウンロードと更新

現在のリリース：**0.3.21**

| 必要なもの | ダウンロード先 | 用途 |
| --- | --- | --- |
| Android APK | [Releases](https://github.com/Neil100o/VRCMomo/releases/latest) の `VRCMomo-v0.3.21.apk` | 固定署名の正式版。以後の更新は GitHub Releases から確認します。 |
| Windows LAN ブリッジ | 同じ Release の `VRCMomo-LAN-Bridge.exe` | VRCX のある PC で起動し、活動履歴をスマートフォンと同期します。 |
| ファイアウォール補助 | 同じ Release の `Allow-VRCMomoLanBridgeFirewall.bat` | ブリッジは起動するがスマートフォンから見つからない/接続できない場合に使用します。 |
| 旧テスト版の移行 APK | [VRCMomo-v0.3.20-legacy-migration.apk](downloads/VRCMomo-v0.3.20-legacy-migration.apk) | 旧 0.3.16 自動更新トラック専用です。 |

リリース内容は [機能ログ](CHANGELOG.md)、署名と移行手順は [インストールと移行ガイド](docs/INSTALL_AND_MIGRATION.md) を確認してください。

## 旧 0.3.16 テスト版からの移行

旧テスト版と正式版は Android の署名が異なるため、直接の上書きはできません。

1. 旧アプリを `VRCMomo-v0.3.20-legacy-migration.apk` で上書き更新します。
2. PC で `VRCMomo-LAN-Bridge.exe` を起動し、旧アプリから一度 LAN 同期します。
3. ブリッジにアーカイブが作られたことを確認してから、0.3.21 正式版をインストールします。
4. 正式版を同じブリッジに接続してアーカイブを取得します。活動ログと関係統計を確認してから旧アプリを削除してください。

手順 2 の前に旧アプリを削除しないでください。イベントの重複除去と最大基線マージにより、同じアーカイブを複数回同期しても回数や時間は水増しされません。

## 上流 VRCM からの VRCMomo 差分

ここでは VRCMomo の追加・保守・改善だけを記載します。アカウント、フレンド/ワールド閲覧、お気に入り、基本グループ、関係グラフ、共通フレンド、VRChat+ Gallery は主に上流 VRCM の機能です。

### フレンド活動と履歴

- オンライン、位置、ソーシャルステータス、プロフィール変更、共同プレイ、最終活動、関係統計をアカウント単位でローカル保存。
- フィルタ可能な活動ログ、保持期間の整理、個別プロフィールでの履歴確認。BIO/ステータスは直近の差分を表示できます。
- 検索欄が空の「ユーザー」タブでは、直近 24 時間に一緒に遊んだプレイヤーを優先表示します。
- 安定したイベント識別子と最大基線マージにより、再起動や端末間同期での重複記録・統計水増しを防ぎます。

### Android 通知と Boop

- システム通知の総合スイッチ、お気に入りグループ/個別フレンドのオンライン通知、フレンド申請、フレンド増減、グループイベント、Boop 通知。
- Android 前景監視、通知権限、バッテリー設定への入口。Web 上のオンライン状態はゲーム内オンライン通知にしません。
- Boop 選択、アプリ内の受信カード、システム通知。0.3.21 では旧通知フィードの絵文字情報が失われる問題を修正しました。

### スマートフォンと PC の活動履歴統合

- VRCX SQLite の読み取り専用エクスポート：オンライン、位置、ステータス、BIO、アバター、フレンド関係、終了済み共同プレイ履歴。
- QR/LAN 検出、スマートフォンのアップロード/ダウンロード、アーカイブ再構築を行う Windows LAN ブリッジ。
- ブリッジは VRCX の元データベースを書き換えず、ログイン情報も扱いません。

### モバイル UI とプロフィール編集

- 独立した VRCMomo 名称、アイコン、バージョン、固定署名リリース。
- 明暗テーマ、複数配色、スマートフォンと広い画面に対応したワールド/アバター/フレンドカード。
- 自分のアバターの分類と、名前・説明・カバー編集。自分のプロフィールの BIO とソーシャルリンク編集。

## ドキュメント

| 文書 | 内容 |
| --- | --- |
| [CHANGELOG.md](CHANGELOG.md) | リリースごとの機能ログと修正履歴。 |
| [docs/INSTALL_AND_MIGRATION.md](docs/INSTALL_AND_MIGRATION.md) | インストール、署名、移行、LAN ブリッジ、トラブルシューティング。 |
| [docs/FEATURE_AUDIT_AND_PATHS.md](docs/FEATURE_AUDIT_AND_PATHS.md) | main の機能監査、分支確認、正確なソースパス。 |
| [docs/LAN_SYNC_DESIGN.md](docs/LAN_SYNC_DESIGN.md) | スマートフォン、ブリッジ、VRCX エクスポートのデータ境界とマージ規則。 |

## 出典と帰属

VRCMomo は [VRCM](https://github.com/vrcm-team/VRCM) の fork および独立継続プロジェクトです。Kotlin Multiplatform/Compose の基盤、認証、VRChat API/ネットワーク、フレンドとワールドの基本機能、お気に入り、グループ、基礎通知、Gallery、関係機能は主に VRCM Team と上流貢献者によるものです。

VRCMomo は独自のブランド、モバイル UI、活動履歴、通知/バックグラウンド処理、Boop、VRCX/LAN 移行、プロフィール編集、リリース署名、安定性修正を保守しています。上流から取り込んだ機能を VRCMomo がゼロから実装したものとして扱わないでください。

## ライセンス

VRCMomo は [MIT License](LICENSE) で公開されています。再現手順、端末情報、ログを添えた不具合報告は [Issues](https://github.com/Neil100o/VRCMomo/issues) へお願いします。

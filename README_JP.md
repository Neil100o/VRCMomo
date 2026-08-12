<div align="center">

# <img src="image/VRCMomoLogo.png" width="50" height="50"  alt="logo"/> VRCMomo

<!-- Language Selection -->
**Languages / 语言 / 言語:**
[English](README.md) • [中文](README_ZH.md) • [日本語](README_JP.md)


[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![GitHub release](https://img.shields.io/github/release/Neil100o/VRCMomo.svg)](https://github.com/Neil100o/VRCMomo/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Neil100o/VRCMomo/total?color=6451f1)](https://github.com/Neil100o/VRCMomo/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-blue)](https://www.jetbrains.com/lp/compose-multiplatform/)

## 日常的なつながりのための VRChat モバイル Companion

VRCM を基盤として継続開発している、Android 優先の VRChat モバイル Companion です。

> **旧署名の移行テスト版：0.3.20**。0.3.16 から上書き更新し、LAN ブリッジで一度同期して活動履歴を保管してから、今後の固定署名版へ移行してください。端末・Android バージョン・再現手順・ログを添えて [Issues](https://github.com/Neil100o/VRCMomo/issues) へ報告してください。

## テスト版のダウンロード

- **Android 移行 APK：**[VRCMomo-v0.3.20-legacy-migration.apk](downloads/VRCMomo-v0.3.20-legacy-migration.apk) — 旧 0.3.16 を上書き更新し、LAN ブリッジで同期・保管してから固定署名版へ移行してください。
- **LAN ブリッジ（Windows）：**[VRCMomo-LAN-Bridge.exe](downloads/VRCMomo-LAN-Bridge.exe) — VRCX を使っている PC で実行し、同一 LAN 上の VRCMomo からペアリングします。Python と QR サポートは同梱済みです。
- プライバシー境界とテスト時の注意は [downloads/README.md](downloads/README.md) を確認してください。これらは安定版ではありません。
- 固定署名版は GitHub Releases から以後の更新を確認します。テストチャンネルは旧 0.3.16 が移行 APK を受け取るためだけに残します。

</div>

<div align="center">

## 上流 VRCM からの VRCMomo 差分

</div>

### フレンド活動と関係履歴
- 実際に観測したオンライン/オフライン、最終活動、最終遭遇、遭遇回数、同一インスタンスでの滞在時間をアカウント別に永続保存します。
- 位置、ソーシャルステータス、プロフィール、共同プレイを分類・複数選択で確認できる活動タイムラインです。BIO とステータスは差分を表示します。
- 検索欄を空にした「ユーザー」タブでは、直近 24 時間に一緒に遊んだプレイヤーを最大 20 人表示します。

### Android 通知とバックグラウンド監視
- Boop とお気に入りフレンドのゲーム入退室に対する、オン/オフ可能なシステム通知。
- 前景監視とバッテリー最適化の案内により、バックグラウンドでの記録・通知の信頼性を改善。
- 関係統計を残したまま、アプリ内活動ログの保持期間設定・削除が可能です。

### Boop・アバター・VRCX 移行
- デフォルト絵文字付き Boop、受信時のアプリ内リアクションカード、通知から開いた後の未読 Boop 復元。
- 自分でアップロードしたアバターの独立分類と、名前・説明・カバー情報の編集。
- 読み取り専用の VRCX エクスポーターと Android のマージインポート。活動、位置、ステータス、BIO、アバター、関係、共同プレイ履歴を扱い、Cookie や認証情報は扱いません。

### ブランドとモバイル UI
- 独立した VRCMomo 名称、アイコン、0.x バージョン体系、APK 名。
- 明暗テーマ・複数配色、モバイル向けのナビゲーション、情報表示、安定性の継続改善。

> **帰属について：**この一覧は VRCMomo の追加・保守分のみです。アカウント、フレンド/ワールド閲覧、お気に入り、基本グループ・通知、VRChat+ Gallery、関係グラフと共通フレンドは主に上流 VRCM の機能です。

<div align="center">

## プラットフォーム対応

</div>

- **Android** - 現在の主要テスト・保守対象

<div align="center">

## 技術アーキテクチャ

</div>

### コア技術スタック
- **[Kotlin Multiplatform](https://kotlinlang.org/multiplatform/)** - クロスプラットフォーム開発フレームワーク
- **[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)** - モダンUIフレームワーク
- **[Ktor](https://ktor.io/)** - ネットワークリクエストとAPI通信
- **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** - JSONデータシリアライゼーション

### アーキテクチャコンポーネント
- **[Koin](https://github.com/InsertKoinIO/koin)** - 依存性注入フレームワーク
- **[Voyager](https://github.com/adrielcafe/voyager)** - ナビゲーションと状態管理
- **[Multiplatform-Settings](https://github.com/russhwolf/multiplatform-settings)** - クロスプラットフォーム設定ストレージ
- **[Coil](https://github.com/coil-kt/coil)** - 高性能画像読み込み

### 開発環境
- **Kotlin API**: 2.1
- **Android SDK Target**: 35
- **Java SDK**: 21
- **Compose**: 1.8.2

<div align="center">

## プロジェクトの出典と謝辞

</div>

VRCMomo は、VRCM Team とその貢献者が開発した [VRCM](https://github.com/vrcm-team/VRCM) を基にした fork および独立した継続プロジェクトです。

VRCX および VRCX-jirai を参考にした機能の出典、移植計画、プライバシー境界については、[VRCX-jirai 機能レビュー](docs/VRCX_JIRAI_FEATURE_REVIEW.md)を参照してください。安定版では、プライバシーに敏感な自動追跡・自動参加機能を扱いません。


Kotlin Multiplatform / Compose Multiplatform の基盤、Android / iOS の構成、認証とアカウント管理、VRChat API とネットワーク層、フレンドとワールドの管理、お気に入り、グループ、通知、既存 UI コンポーネント、VRChat+ Gallery、フレンド関係ネットワークと共通フレンドのページは、主に上流の VRCM に由来します。

VRCMomo では、新しいブランド、テーマとナビゲーションの変更、Boop と通知の改善、Gallery の修正と表示調整、iOS のブランド対応、リリースパッケージングなどを追加・保守しています。上流の機能をここで適応・保守する場合があっても、VRCMomo がゼロから実装したものとは解釈しないでください。

VRCM Team とすべての上流貢献者に感謝します。元の履歴と貢献の帰属については、[上流リポジトリ](https://github.com/vrcm-team/VRCM)を参照してください。

<div align="center">

## 免責事項

</div>

- VRCMomoはVRChat Incと関連がなく、VRChat Incの見解や意見を代表するものではありません
- VRCMomoはあなたのデバイス外でデータを保存・収集することはありません
- アプリケーション作者はこのアプリケーションが引き起こす損害について責任を負いません
- VRCMomoはゲームを改変・改ざんせず、[VRChat利用規約](https://hello.vrchat.com/legal)に違反しません
- このアプリケーションを合理的に使用し、関連法規とプラットフォーム規定を遵守してください

<div align="center">

## ライセンス

</div>

本プロジェクトは[MIT ライセンス](LICENSE)でオープンソース化されています。

<div align="center">

## 貢献

</div>

コードの貢献、問題の報告、機能提案を歓迎します！詳細については貢献ガイドをご確認ください。

---

<div align="center">

**このプロジェクトがお役に立てば、⭐をお願いします**

[最新版をダウンロード](https://github.com/Neil100o/VRCMomo/releases/latest) • [問題を報告](https://github.com/Neil100o/VRCMomo/issues) • [機能提案](https://github.com/Neil100o/VRCMomo/discussions)

</div>


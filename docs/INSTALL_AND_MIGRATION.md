# VRCMomo 安装、更新与旧记录迁移 / Installation, Updates and Legacy Migration / インストール・更新・旧記録移行

最后更新：2026-08-13
适用仓库：`F:\vrcmoskavis\VRCMomoLanSync`
当前正式发行：`0.3.21`，Android `versionCode = 33`

## 1. 选择正确的安装包

| 你的现状 | 应使用的文件 | 是否可直接覆盖安装 |
| --- | --- | --- |
| 从未安装过 VRCMomo | Release 的 `VRCMomo-v0.3.21.apk` | 可以。 |
| 已安装固定签名的 VRCMomo 0.3.20 | Release 的 `VRCMomo-v0.3.21.apk` | 可以。 |
| 已安装旧自动更新轨 0.3.16 | `downloads/VRCMomo-v0.3.20-legacy-migration.apk` | 可以，先做记录迁移。 |
| 已安装旧 0.3.16，且想装正式版 0.3.21 | 不能直接覆盖 | 必须先用桥接器导出旧应用记录；见第 2 节。 |

旧 0.3.16 使用 `io.github.vrcmteam.vrcm.debug` 和旧调试签名；正式发行版使用 `io.github.vrcmteam.vrcm` 与固定发行签名。Android 会把它们视为不同安装来源，正式版无法读取旧应用私有目录。

## 2. 从旧测试轨迁移活动记录

### 准备

1. 从 [Releases](https://github.com/Neil100o/VRCMomo/releases/latest) 下载 `VRCMomo-LAN-Bridge.exe` 到 Windows 电脑。
2. 如果桥接器能启动但手机找不到/连不上它，再运行同一 Release 的 `Allow-VRCMomoLanBridgeFirewall.bat`。它只添加本地网络的 TCP `38671` 和 UDP `38672` 放行规则。
3. 确认电脑和手机在同一可互通的局域网。关闭会改变本地路由的 VPN；访客 Wi-Fi、AP 隔离和不同 VLAN 往往不能使用发现或连接。

### 迁移步骤

1. 在旧应用上安装 `VRCMomo-v0.3.20-legacy-migration.apk`，覆盖旧 0.3.16。
2. 在电脑启动 `VRCMomo-LAN-Bridge.exe`。桥接器会显示二维码、地址和令牌。
3. 在旧应用打开：设置 → 局域网同步；使用“寻找附近电脑”或“扫描配对二维码”。
4. 连接后执行一次“发送手机记录到电脑”或完整同步。桥接器会在其工作目录的 `vrcmomo-lan-inbox/archive-rebuilt.json` 重建归档。
5. 确认旧应用的活动日志和桥接器归档均正常后，安装 Release 的 `VRCMomo-v0.3.21.apk`。
6. 在正式版重复配对并选择“从电脑同步”，导入同一归档。
7. 核对活动日志、最后活动、见面次数和共同游玩时长；确认无误后才删除旧应用。

不要在第 4 步之前卸载旧应用。桥接器的合并规则是：事件按指纹去重，累计统计取各设备的最大基线；因此可安全重复同步，不会把同一段共同游玩重复加总。

## 3. 日常使用 Windows LAN Bridge

### 它会做什么

- 只读 VRCX 的 SQLite 活动数据库；不会修改 VRCX 原始表。
- 提供本机临时 HTTP 服务、二维码和 UDP `38672` 局域网发现。
- 接收手机活动快照，重建本机归档；手机可以拉取归档并把自己的新事件回传。
- 可导入 VRCX 的在线、位置、社交状态、BIO、头像、好友关系和已结束共同游玩历史。

### 它不会做什么

- 不上传到云端。
- 不读取或导出 VRChat Cookie、账号密码、登录令牌、私人备注或管理/封禁数据。
- 不把手机记录直接写进 VRCX 的原始 SQLite 数据库。

### 网络排错顺序

1. 确认桥接器窗口仍在运行，且二维码/地址没有过期。
2. 确认手机和电脑 IP 在同一个可路由局域网；有线电脑和 Wi-Fi 手机可以正常共存，只要路由器没有隔离客户端。
3. 关闭手机和电脑的 VPN 后再试。
4. 运行 `Allow-VRCMomoLanBridgeFirewall.bat`，随后重启桥接器。
5. 自动发现失败时优先扫描桥接器二维码；二维码包含正确的地址和短期令牌。
6. 大型历史归档首次同步可能较久。客户端会在连接建立后按空闲时间判断中断，不以总传输时长直接取消。

## 4. 开发者路径

- 版本与 `versionCode`：`gradle/libs.versions.toml`
- 应用内版本常量：`composeApp/src/commonMain/kotlin/core/shared/AppConst.kt`
- 更新顺序和版本比较：`composeApp/src/commonMain/kotlin/service/VersionService.kt`
- 测试轨迁移元数据：`downloads/testing-channel.json`
- Windows 桥接器源码：`tools/vrcmomo_lan_bridge.py`
- 桥接合并规则：`tools/vrcmomo_activity_merge.py`
- Android 同步客户端：`composeApp/src/commonMain/kotlin/service/LanActivityBridgeClient.kt`
- Android 扫码/发现：`composeApp/src/androidMain/kotlin/service/LanBridgeQrScanner.android.kt`、`LanBridgeDiscovery.android.kt`
- 发行签名：`composeApp/build.gradle.kts`、`docs/RELEASE_SIGNING.md`

更完整的功能与源码入口索引见 [FEATURE_AUDIT_AND_PATHS.md](FEATURE_AUDIT_AND_PATHS.md)。

## English

### Which package should I install?

| Current state | File to use | Can install over the existing app? |
| --- | --- | --- |
| No VRCMomo is installed | `VRCMomo-v0.3.21.apk` from the current Release | Yes. |
| Permanent-signature VRCMomo 0.3.20 is installed | `VRCMomo-v0.3.21.apk` from the current Release | Yes. |
| The old 0.3.16 automatic-update app is installed | `downloads/VRCMomo-v0.3.20-legacy-migration.apk` | Yes; migrate history first. |
| The old 0.3.16 app is installed and you want 0.3.21 | Do not install the release over it first | Back up activity history through the bridge first. |

The old 0.3.16 app uses package `io.github.vrcmteam.vrcm.debug` and its old debug certificate. The permanent release uses `io.github.vrcmteam.vrcm` and a permanent release certificate, so Android does not permit a direct replacement and the new app cannot read the old private app storage.

### Migration from the old testing track

1. Download `VRCMomo-LAN-Bridge.exe` from [Releases](https://github.com/Neil100o/VRCMomo/releases/latest) on a Windows PC.
2. If the bridge starts but the phone cannot find or reach it, run `Allow-VRCMomoLanBridgeFirewall.bat` from the same Release. It opens LAN-only TCP `38671` and UDP `38672` rules.
3. Put the phone and PC on the same reachable LAN. Disable VPNs that change local routing; guest Wi-Fi, AP isolation and different VLANs often block discovery.
4. Install `VRCMomo-v0.3.20-legacy-migration.apk` over the old 0.3.16 app.
5. Start the bridge. In the old app, open Settings → LAN Sync, then use nearby-computer discovery or scan the bridge QR code.
6. Send phone records to the PC or run a complete sync. The bridge rebuilds `vrcmomo-lan-inbox/archive-rebuilt.json` in its working directory.
7. Confirm the archive exists, install `VRCMomo-v0.3.21.apk`, pair it with the same bridge and pull the archive.
8. Verify the activity log, last activity, meeting count and shared-play duration before deleting the old app.

Do not delete the old app before step 6. Events are deduplicated by stable fingerprints and cumulative totals are merged by maximum baseline, so repeated syncs are safe.

### Daily LAN Bridge use and troubleshooting

The bridge reads VRCX SQLite activity data only; it does not modify VRCX tables, copy credentials, or use a cloud service. It serves a temporary local endpoint, QR code and UDP discovery. The phone can download the rebuilt archive and upload its own new events.

If pairing fails: keep the bridge window open, verify both devices are on the same routable LAN, disable VPNs, run the firewall helper, restart the bridge, and then scan the QR code. Large first-time archives may take time; the client uses an idle timeout rather than cancelling an active transfer by total duration.

## 日本語

### どの APK をインストールしますか？

| 現在の状態 | 使用するファイル | 既存アプリへ上書き可能か |
| --- | --- | --- |
| VRCMomo を未インストール | 現在の Release の `VRCMomo-v0.3.21.apk` | 可能です。 |
| 固定署名の VRCMomo 0.3.20 を使用中 | 現在の Release の `VRCMomo-v0.3.21.apk` | 可能です。 |
| 旧 0.3.16 自動更新版を使用中 | `downloads/VRCMomo-v0.3.20-legacy-migration.apk` | 可能です。先に履歴を移行します。 |
| 旧 0.3.16 から 0.3.21 に移りたい | 先に正式版を上書きしない | ブリッジで活動履歴を退避します。 |

旧 0.3.16 は `io.github.vrcmteam.vrcm.debug` と旧デバッグ署名を使用し、正式版は `io.github.vrcmteam.vrcm` と固定署名を使用します。Android は直接の置換を許可せず、正式版は旧アプリの私有保存領域を読めません。

### 旧テストトラックからの移行

1. Windows PC で [Releases](https://github.com/Neil100o/VRCMomo/releases/latest) から `VRCMomo-LAN-Bridge.exe` をダウンロードします。
2. ブリッジが起動してもスマートフォンから見つからない/接続できない場合は、同じ Release の `Allow-VRCMomoLanBridgeFirewall.bat` を実行します。LAN 用 TCP `38671` と UDP `38672` の規則を追加します。
3. PC とスマートフォンを相互到達できる同じ LAN に接続します。ローカル経路を変える VPN は無効にしてください。ゲスト Wi-Fi、AP 分離、異なる VLAN では検出できない場合があります。
4. 旧 0.3.16 に `VRCMomo-v0.3.20-legacy-migration.apk` を上書きします。
5. ブリッジを起動し、旧アプリの設定 → LAN 同期から近くの PC を探すか、ブリッジの QR コードを読み取ります。
6. スマートフォン記録を PC へ送信するか、完全同期を実行します。ブリッジは作業フォルダに `vrcmomo-lan-inbox/archive-rebuilt.json` を作成します。
7. アーカイブを確認してから `VRCMomo-v0.3.21.apk` をインストールし、同じブリッジに接続してアーカイブを取得します。
8. 活動ログ、最終活動、遭遇回数、共同プレイ時間を確認してから旧アプリを削除します。

手順 6 より前に旧アプリを削除しないでください。イベントは安定した識別子で重複除去され、累計値は最大基線でマージされるため、繰り返し同期しても安全です。

### 日常の LAN Bridge 利用とトラブルシューティング

ブリッジは VRCX SQLite の活動データを読み取り専用で扱います。VRCX の表を書き換えず、認証情報をコピーせず、クラウドサービスも使用しません。一時的なローカル接続、QR コード、UDP 検出を提供し、スマートフォンは再構築済みアーカイブを取得して新しいイベントを送信できます。

ペアリングに失敗する場合は、ブリッジのウィンドウを開いたままにし、同じ到達可能な LAN を確認し、VPN を無効にし、ファイアウォール補助を実行してブリッジを再起動し、QR コードを読み取ってください。初回の大きなアーカイブには時間がかかる場合がありますが、クライアントは転送全体の時間ではなくアイドル時間で中断を判断します。

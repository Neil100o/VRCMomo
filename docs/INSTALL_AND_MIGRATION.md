# VRCMomo 安装、更新与旧记录迁移

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

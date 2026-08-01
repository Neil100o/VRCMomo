# VRCX 活动导出桥接工具

给普通用户分发的是 `VRCMomo-VRCX-Activity-Export.exe`，双击即可运行，**不需要安装 Python**。当前构建文件位于 `tools/dist/`。

`Export-VRCXActivity.bat` 和 `export_vrcx_activity.py` 是开发/维护用版本，需要本机安装 Python。

导出工具默认读取：

`%APPDATA%\VRCX\VRCX.sqlite3`

工具以只读方式打开数据库，导出 `vrcmomo-vrcx-activity-v1.json`。导出的内容是好友上下线、位置变化、关系历史、自己的实例记录和实例内 Join / Leave 事件；不会导出 Cookie、密码、Token、账号设置或原始数据库。

如 VRCX 数据库不在默认位置：

```text
Export-VRCXActivity.bat --db "D:\Backup\VRCX.sqlite3" --output "D:\Export\vrcmomo-vrcx-activity-v1.json"
```

若存在多个 VRCX 账号，工具会要求选择账号前缀。将 JSON 传到手机后，在 VRCMomo 的“设置 → VRCX 活动数据导入”中选择它；应用会先显示上下线事件、共同游玩和重复事件数量，确认后才合并。重复导入同一事件会自动跳过。

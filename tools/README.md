# VRCX 活动导出桥接工具

在 Windows 上双击 `Export-VRCXActivity.bat`。它默认读取：

`%APPDATA%\VRCX\VRCX.sqlite3`

工具以只读方式打开数据库，导出 `vrcmomo-vrcx-activity-v1.json`。导出的内容是好友上下线、位置变化、关系历史、自己的实例记录和实例内 Join / Leave 事件；不会导出 Cookie、密码、Token、账号设置或原始数据库。

如 VRCX 数据库不在默认位置：

```text
Export-VRCXActivity.bat --db "D:\Backup\VRCX.sqlite3" --output "D:\Export\vrcmomo-vrcx-activity-v1.json"
```

若存在多个 VRCX 账号，工具会要求选择账号前缀。下一步由 VRCMomo 手机端读取该 JSON、显示统计预览后再导入。

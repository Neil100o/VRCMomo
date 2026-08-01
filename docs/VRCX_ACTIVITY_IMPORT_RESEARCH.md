# VRCX 活动数据导入调研

> 状态：调研完成，尚未接入应用。目标是迁移 VRCX 的历史活动，而不是重复导入现有好友列表。

## 结论

VRCX 的有效历史数据保存在桌面端 SQLite 数据库中，而不是好友列表 JSON。应优先迁移：

1. 好友上线 / 下线历史；
2. 同实例相遇、一起游玩的时长与次数；
3. 最后活动、最后见面和离线时间；
4. 可选的昵称、状态、Bio、头像变化历史。

不迁移账号、Cookie、密码、Token，也不把 VRCX 的整个数据库复制到 VRCMomo。

## 已确认的 VRCX 数据来源

VRCX 当前源码中，账号相关表名由 VRChat 用户 ID 去掉 `usr_`、连字符和下划线后得到前缀，例如 `usr_xxx` 对应 `<prefix>_feed_online_offline`。

| VRCX 表 | 可迁移内容 | VRCMomo 对应 |
|---|---|---|
| `<prefix>_feed_online_offline` | 上线、下线、时间、用户 ID、位置、世界 | 好友活动区间、最后活动、离线时长 |
| `gamelog_join_leave` | 某实例内玩家 Join / Leave、时间、用户 ID | 共同游玩时长与见面次数 |
| `gamelog_location` | 自己进入实例的时间、位置、世界、停留时长 | 用于判断共同实例的起止范围 |
| `<prefix>_feed_gps` | 好友位置变化与世界 | 补充最后活动、最后见面 |
| `<prefix>_friend_log_history` | 加好友 / 删除好友、昵称和信任等级历史 | 可选关系时间线 |
| `<prefix>_activity_sessions_v2` | VRCX 已计算的活动会话缓存 | 仅作为校验，优先导入原始事件 |

## 推荐实现：桌面导出桥接文件

手机通常不能直接读取电脑上的 VRCX 数据库；直接在 Android 解析不同版本的 SQLite 数据库也难维护。因此采用两步流程：

```text
电脑上的 VRCX.sqlite3（只读）
        ↓
VRCMomo 导出桥接脚本
        ↓
vrcmomo-vrcx-activity-v1.json
        ↓
手机 VRCMomo 导入、预览、确认
```

桥接文件只包含经过筛选的活动事件和用户 ID；导入前显示账号、时间范围、事件数、涉及好友数，默认合并且去重。VRCMomo 会为导入记录标记 `source = vrcx-import`，避免与后续手机观察到的事件重复计算。

## 下一步

1. 写只读的 Windows 导出脚本，先用真实 VRCX 数据库验证表和字段；
2. 定义并固定 `vrcmomo-vrcx-activity-v1.json`；
3. 扩展 VRCMomo 活动存储，使其能够保存导入事件与来源；
4. 增加 Android 导入预览、冲突处理和回滚备份；
5. 用脱敏样本做端到端迁移测试。

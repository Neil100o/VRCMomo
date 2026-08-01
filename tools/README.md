# VRCX activity export bridge

`VRCMomo-VRCX-Activity-Export.exe` is the Windows tool distributed to testers.
Run it on a computer that has VRCX installed. It opens `%APPDATA%\VRCX\VRCX.sqlite3`
read-only and writes `vrcmomo-vrcx-activity-v1.json` beside the tool.

The default export contains no credentials, cookies, passwords, tokens, or VRCX account settings.
It includes the portable VRCMomo bridge history plus future-facing archive data:

- friend presence, locations, social-status, bio, avatar and friendship history;
- completed shared-session history and the user's own location history;
- VRCX activity-session records, mutual-relationship graph records, avatar history;
- cached avatar/world metadata and favorite world/avatar/friend groups;
- non-secret game log records such as portals and video/resource activity.

The export uses the `vrcmomo-vrcx-activity-v3` format. Current VRCMomo imports the supported
activity history and safely retains its own records through merge/deduplication. Extra archive
sections are intentionally forward-compatible for future mobile features.

VRCX local notes and memos are personal text, so they are excluded by default. Developers can
include them explicitly with:

```text
Export-VRCXActivity.bat --include-private-notes
```

For a non-default VRCX database location:

```text
Export-VRCXActivity.bat --db "D:\Backup\VRCX.sqlite3" --output "D:\Export\vrcmomo-vrcx-activity-v3.json"
```

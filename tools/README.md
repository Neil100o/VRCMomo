# VRCX activity export bridge

`VRCMomo-VRCX-Activity-Export.exe` is the Windows tool distributed to testers.
Run it on a computer that has VRCX installed. It opens `%APPDATA%\VRCX\VRCX.sqlite3`
read-only and writes `vrcmomo-vrcx-activity-v1.json` beside the tool.

The default export contains no credentials, cookies, passwords, tokens, or VRCX account settings.
It includes the portable VRCMomo bridge history plus future-facing archive data:

- friend presence, locations, social-status, bio, avatar and friendship history;
- completed shared-session history and the user's own location history;
- VRCX activity-session records, current friend-history records, and avatar history;
- cached avatar/world metadata that can support future mobile browsing;
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

## LAN sync bridge

`VRCMomo-LAN-Bridge.exe` is the Windows local-network bridge distributed to testers. Run it on a computer running VRCX; it contains Python, the QR helper and the read-only exporter, so no separate runtime or package install is needed. Its small desktop UI shows pairing state, archive counts, rebuild status and JSON export. It reads VRCX activity only through the existing exporter and never modifies `VRCX.sqlite3`.

Phone snapshots are stored beside the EXE in `vrcmomo-lan-inbox`. `archive-rebuilt.json` is an
inspectable canonical copy. Rebuild uses maximum snapshot baselines and paired meeting episodes;
repeating a sync does not add the same counters again.

```text
VRCMomo-LAN-Bridge.exe
```

`Start-VRCMomoLanBridge.bat` remains a source/developer launcher. Rebuild the distributed executable with `Build-VRCMomoLanBridge.ps1`.

The desktop UI shows a pairing QR code and also answers VRCMomo's LAN discovery request. Keep it open while syncing. VRCMomo can pair by discovery or QR, run a manual two-way sync, and optionally sync once when the app starts. Validated phone snapshots are stored in `vrcmomo-lan-inbox` and rebuilt into an idempotent aggregate archive; VRCX itself remains read-only.

For a non-default database:

```text
python vrcmomo_lan_bridge.py --db "D:\Backup\VRCX.sqlite3"
```

See `docs/LAN_SYNC_DESIGN.md` for protocol, privacy boundaries and the staged delivery plan.

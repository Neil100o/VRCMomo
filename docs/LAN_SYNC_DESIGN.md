# VRCMomo LAN activity bridge

## Goal

Synchronize social-activity history between a Windows computer running VRCX and an Android device running VRCMomo **only while they are on the same local network**. The bridge is an optional personal tool, not a cloud service.

## Boundaries

- Read VRCX's SQLite database in read-only mode.
- Never copy VRChat cookies, passwords, tokens, account settings, moderation data, or private notes by default.
- Do not write directly into VRCX's native SQLite tables.
- Preserve data already stored by either side. Imports are merge-only and use stable event keys for idempotency.

## Intended experience

1. Start `VRCMomo-LAN-Bridge` on the PC once.
2. Pair the phone by automatic discovery or by scanning the QR code shown in the desktop UI.
3. When both devices are reachable on the same LAN, VRCMomo can fetch VRCX changes automatically; the user can also press **Sync now**.
4. Phone-only events are uploaded to the bridge's separate inbox. A future VRCX fork adapter may display them, without mutating VRCX tables.

## Protocol v1

The PC bridge exposes a temporary local HTTP service. Every endpoint requires a randomly generated pairing token. The service is deliberately LAN-only and provides QR pairing plus UDP discovery. Traffic is not routed through a cloud service, so the bridge should only be used on a trusted local network.

| Endpoint | Direction | Purpose |
| --- | --- | --- |
| `GET /v1/health` | Phone <- PC | Pairing and capability check |
| `GET /v1/vrcx-activity` | Phone <- PC | Fresh read-only VRCX activity export in `vrcmomo-vrcx-activity-v3` format |
| `POST /v1/vrcmomo-activity` | Phone -> PC | Store a validated VRCMomo activity envelope for later desktop display/import |
| `GET /v1/vrcmomo-activity` | Phone <- PC | Return the canonical archive rebuilt from all uploaded phone snapshots |

The bridge accepts the token via `X-VRCMomo-Bridge-Token` or `?token=`. Payloads are limited to 32 MiB. Received mobile payloads are saved atomically in the bridge inbox and are not interpreted as VRCX database writes.

## Delivery stages

- **Completed:** local bridge server, temporary token, QR/UDP pairing, read-only VRCX export, bounded mobile inbox and canonical archive rebuild.
- **Completed:** Android discovery/QR pairing, manual download/upload and optional one-time sync when the app starts.
- **Next:** optional VRCX fork adapter to view imported mobile events in the desktop UI without writing them into VRCX's native tables.

## Conflict and deduplication rules

- Exact event identity uses type, timestamp, user ID, before/after values and Diff lines.
- Adjacent observations of the same transition within 120 seconds are treated as cross-device duplicates; an intervening event prevents folding.
- Social status and pseudo-location casing is normalized before comparison.
- Complete device counters use maximum-baseline merge, not addition.
- Completed `Met -> Left` episodes can rebuild a larger meeting count and together duration.
- Existing VRCMomo events are never overwritten; repeated sync is idempotent.
- The phone remains the owner of its local cache; the bridge is an exchange point, not the authoritative cloud database.

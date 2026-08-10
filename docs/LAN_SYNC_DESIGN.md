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
2. Pair the phone by scanning a QR code or entering the displayed local URL and short pairing code.
3. When both devices are reachable on the same LAN, VRCMomo can fetch VRCX changes automatically; the user can also press **Sync now**.
4. Phone-only events are uploaded to the bridge's separate inbox. A future VRCX fork adapter may display them, without mutating VRCX tables.

## Protocol v1

The PC bridge exposes a temporary local HTTP service. Every endpoint requires a randomly generated pairing token. The service is deliberately LAN-only; a future version can add QR pairing and encrypted transport.

| Endpoint | Direction | Purpose |
| --- | --- | --- |
| `GET /v1/health` | Phone <- PC | Pairing and capability check |
| `GET /v1/vrcx-activity` | Phone <- PC | Fresh read-only VRCX activity export in `vrcmomo-vrcx-activity-v3` format |
| `POST /v1/vrcmomo-activity` | Phone -> PC | Store a validated VRCMomo activity envelope for later desktop display/import |

The bridge accepts the token via `X-VRCMomo-Bridge-Token` or `?token=`. Payloads are limited to 16 MiB. Received mobile payloads are saved atomically in the bridge inbox and are not interpreted as VRCX database writes.

## Delivery stages

- **Stage 1 (this foundation):** local bridge server, pairing token, read-only VRCX export endpoint, bounded mobile inbox.
- **Stage 2:** Android pairing screen, one-tap fetch and merge using the existing VRCX importer, plus a manual credential-free phone-history upload to the bridge inbox.
- **Stage 3:** Android periodic LAN sync when the user enables it; no cloud requirement.
- **Stage 4:** optional VRCX fork adapter to view imported mobile events in the desktop UI.

## Conflict and deduplication rules

- Event identity is based on source, event type, timestamp, user ID and before/after values.
- Existing VRCMomo events are never overwritten.
- Repeated syncs only add unseen events.
- The phone remains the owner of its local cache; the bridge is an exchange point, not the authoritative cloud database.

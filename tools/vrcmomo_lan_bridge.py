#!/usr/bin/env python3
"""Local-only VRCX -> VRCMomo activity bridge.

The bridge reads VRCX activity through the existing read-only exporter and makes
it available to a paired VRCMomo device on the same LAN. It never writes VRCX's
SQLite database. Mobile uploads are bounded and stored separately for a future
desktop adapter; they are not injected into VRCX tables.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import secrets
import socket
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

MAX_PAYLOAD_BYTES = 16 * 1024 * 1024
DEFAULT_PORT = 38671
EXPORTER = Path(__file__).with_name("export_vrcx_activity.py")


def local_ip() -> str:
    """Best-effort LAN address for the pairing hint; no data leaves the device."""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        try:
            sock.connect(("192.0.2.1", 80))
            return sock.getsockname()[0]
        except OSError:
            return "127.0.0.1"


class BridgeState:
    def __init__(self, db: Path, account_prefix: str | None, inbox: Path, token: str) -> None:
        self.db = db
        self.account_prefix = account_prefix
        self.inbox = inbox
        self.token = token

    def export_vrcx_activity(self) -> bytes:
        with tempfile.TemporaryDirectory(prefix="vrcmomo-bridge-") as temporary:
            output = Path(temporary) / "vrcx-activity.json"
            command = [
                sys.executable,
                str(EXPORTER),
                "--db", str(self.db),
                "--output", str(output),
            ]
            if self.account_prefix:
                command.extend(("--account-prefix", self.account_prefix))
            result = subprocess.run(command, text=True, capture_output=True, timeout=90)
            if result.returncode != 0:
                raise RuntimeError(result.stderr.strip() or "VRCX export failed")
            payload = output.read_bytes()
        if len(payload) > MAX_PAYLOAD_BYTES:
            raise ValueError("VRCX export exceeds the 16 MiB LAN transfer limit")
        return payload

    def save_mobile_upload(self, payload: bytes) -> str:
        if not payload or len(payload) > MAX_PAYLOAD_BYTES:
            raise ValueError("Payload must be between 1 byte and 16 MiB")
        document = json.loads(payload)
        if not isinstance(document, dict) or document.get("format") != "vrcmomo-activity-sync-v1":
            raise ValueError("Unsupported VRCMomo LAN activity format")
        digest = hashlib.sha256(payload).hexdigest()
        self.inbox.mkdir(parents=True, exist_ok=True)
        target = self.inbox / f"mobile-{digest}.json"
        if not target.exists():
            temporary = target.with_suffix(".tmp")
            temporary.write_bytes(payload)
            temporary.replace(target)
        return digest


class BridgeHandler(BaseHTTPRequestHandler):
    server: "BridgeServer"

    def log_message(self, format: str, *args: object) -> None:
        # Do not log URLs, because a URL may carry the pairing token.
        print(f"[{self.log_date_time_string()}] {args[1]}")

    def do_GET(self) -> None:
        if not self._authorized():
            return self._json(HTTPStatus.UNAUTHORIZED, {"error": "pairing required"})
        path = urlparse(self.path).path
        if path == "/v1/health":
            return self._json(HTTPStatus.OK, {
                "service": "vrcmomo-lan-bridge",
                "protocol": 1,
                "vrcxExportFormat": "vrcmomo-vrcx-activity-v3",
                "mobileUploadFormat": "vrcmomo-activity-sync-v1",
                "serverTime": datetime.now(timezone.utc).isoformat(),
            })
        if path == "/v1/vrcx-activity":
            try:
                return self._bytes(HTTPStatus.OK, self.server.state.export_vrcx_activity())
            except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
                return self._json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": str(error)})
        return self._json(HTTPStatus.NOT_FOUND, {"error": "unknown endpoint"})

    def do_POST(self) -> None:
        if not self._authorized():
            return self._json(HTTPStatus.UNAUTHORIZED, {"error": "pairing required"})
        if urlparse(self.path).path != "/v1/vrcmomo-activity":
            return self._json(HTTPStatus.NOT_FOUND, {"error": "unknown endpoint"})
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_PAYLOAD_BYTES:
            return self._json(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, {"error": "payload must be 1 byte to 16 MiB"})
        try:
            digest = self.server.state.save_mobile_upload(self.rfile.read(length))
        except (ValueError, json.JSONDecodeError) as error:
            return self._json(HTTPStatus.BAD_REQUEST, {"error": str(error)})
        return self._json(HTTPStatus.OK, {"stored": True, "sha256": digest})

    def _authorized(self) -> bool:
        query_token = parse_qs(urlparse(self.path).query).get("token", [""])[0]
        supplied = self.headers.get("X-VRCMomo-Bridge-Token", query_token)
        return secrets.compare_digest(supplied, self.server.state.token)

    def _json(self, status: HTTPStatus, document: dict) -> None:
        self._bytes(status, json.dumps(document, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")

    def _bytes(self, status: HTTPStatus, payload: bytes, content_type: str = "application/json; charset=utf-8") -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)


class BridgeServer(ThreadingHTTPServer):
    def __init__(self, address: tuple[str, int], state: BridgeState) -> None:
        super().__init__(address, BridgeHandler)
        self.state = state


def main() -> int:
    parser = argparse.ArgumentParser(description="Serve a paired local VRCX activity bridge for VRCMomo.")
    parser.add_argument("--db", type=Path, default=Path.home() / "AppData/Roaming/VRCX/VRCX.sqlite3")
    parser.add_argument("--account-prefix", help="VRCX account table prefix when multiple accounts exist")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--inbox", type=Path, default=Path.cwd() / "vrcmomo-lan-inbox")
    args = parser.parse_args()
    if not args.db.is_file():
        parser.error(f"VRCX database not found: {args.db}")
    if not 1 <= args.port <= 65535:
        parser.error("port must be between 1 and 65535")

    token = secrets.token_urlsafe(24)
    state = BridgeState(args.db.resolve(), args.account_prefix, args.inbox.resolve(), token)
    server = BridgeServer(("0.0.0.0", args.port), state)
    address = local_ip()
    print("VRCMomo LAN Bridge is running. Keep this window open while syncing.")
    print(f"Pairing URL: http://{address}:{args.port}/v1/health?token={token}")
    print("VRCX is read-only. Mobile uploads are stored in the local inbox, never written into VRCX.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBridge stopped.")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

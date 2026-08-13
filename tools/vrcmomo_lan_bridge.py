#!/usr/bin/env python3
"""Local-only VRCX -> VRCMomo activity bridge.

The bridge reads VRCX activity through the existing read-only exporter and makes
it available to a paired VRCMomo device on the same LAN. It never writes VRCX's
SQLite database. Mobile uploads are bounded and stored separately for a future
desktop adapter; they are not injected into VRCX tables.
"""
from __future__ import annotations

import argparse
try:
    import qrcode
except ImportError:  # Optional at runtime; the launcher installs it for the normal path.
    qrcode = None
import hashlib
import io
import ipaddress
import json
import os
import secrets
import socket
import sqlite3
import subprocess
import sys
import tempfile
import threading
import time
import webbrowser
from contextlib import redirect_stderr, redirect_stdout
from datetime import datetime, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from vrcmomo_activity_merge import decode_documents, merge_archive

# Full VRCX archives can exceed 16 MiB while still being reasonable for an
# explicit local-network transfer. Keep a firm bound to avoid unbounded reads.
MAX_PAYLOAD_BYTES = 32 * 1024 * 1024
DEFAULT_PORT = 38671
DISCOVERY_PORT = 38672
DISCOVERY_REQUEST = b"VRCMOMO-LAN-DISCOVERY-V1"
EXPORTER = Path(__file__).with_name("export_vrcx_activity.py")


def default_inbox() -> Path:
    """Keep the archive beside the distributed bridge instead of the launch working directory."""
    base = Path(sys.executable).resolve().parent if getattr(sys, "frozen", False) else Path(__file__).resolve().parent
    return base / "vrcmomo-lan-inbox"


def print_pairing_qr(url: str) -> None:
    """Print a terminal QR code; it contains the short-lived pairing URL and token."""
    if qrcode is None:
        print("QR code helper is not installed; copy the pairing URL below instead. LAN discovery and sync still work normally.")
        return
    code = qrcode.QRCode(border=1)
    code.add_data(url)
    code.make(fit=True)
    print("Scan this QR code with the phone, or paste the pairing URL below:")
    try:
        code.print_ascii(invert=True)
    except UnicodeEncodeError:
        # Legacy Chinese Windows consoles commonly use GBK and cannot render
        # the block characters used by qrcode. Pairing URL and LAN discovery
        # remain fully functional, so never let an optional QR preview stop
        # the bridge server.
        print("This console cannot display the QR code; use the pairing URL or LAN discovery instead.")


def local_ip() -> str:
    """Prefer the home-LAN adapter over VPN and virtual adapters for pairing."""
    candidates: list[str] = []
    try:
        candidates.extend(socket.gethostbyname_ex(socket.gethostname())[2])
    except OSError:
        pass
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        try:
            sock.connect(("192.0.2.1", 80))
            candidates.append(sock.getsockname()[0])
        except OSError:
            pass
    private = []
    for candidate in dict.fromkeys(candidates):
        try:
            address = ipaddress.ip_address(candidate)
        except ValueError:
            continue
        if address.version == 4 and address.is_private and not address.is_loopback and not address.is_link_local:
            private.append(candidate)
    # ipaddress treats the VPN benchmarking block as non-global on some Python releases.
    private = [value for value in private if not value.startswith(("198.18.", "198.19."))]
    # A VPN commonly adds a 10.x adapter before the physical Wi-Fi adapter.
    # Most home routers use 192.168.x.x, so prefer it for a QR code shown to a
    # phone on the same Wi-Fi. Discovery still uses the reply source address.
    def preference(value: str) -> int:
        if value.startswith("192.168."):
            return 0
        if value.startswith("172."):
            return 1
        if value.startswith("10."):
            return 2
        return 3

    return min(private, key=preference) if private else "127.0.0.1"


class BridgeState:
    def __init__(
        self,
        db: Path,
        account_prefix: str | None,
        inbox: Path,
        token: str,
        on_status=None,
    ) -> None:
        self.db = db
        self.account_prefix = account_prefix
        self._resolved_account_prefix: str | None = None
        self.inbox = inbox
        self.token = token
        self.on_status = on_status
        self._lock = threading.RLock()
        self._export_lock = threading.Lock()
        self._last_report: dict[str, int] = {}

    def export_vrcx_activity(self) -> bytes:
        account_prefix = self._resolve_account_prefix()
        with tempfile.TemporaryDirectory(prefix="vrcmomo-bridge-") as temporary:
            output = Path(temporary) / "vrcx-activity.json"
            if getattr(sys, "frozen", False):
                self._export_vrcx_in_process(output)
            else:
                command = [
                    sys.executable,
                    str(EXPORTER),
                    "--db", str(self.db),
                    "--output", str(output),
                ]
                command.extend(("--account-prefix", account_prefix))
                result = subprocess.run(command, text=True, capture_output=True, timeout=90)
                if result.returncode != 0:
                    raise RuntimeError(result.stderr.strip() or "VRCX export failed")
            payload = output.read_bytes()
        if len(payload) > MAX_PAYLOAD_BYTES:
            raise ValueError("VRCX export exceeds the 32 MiB LAN transfer limit")
        return payload

    def _resolve_account_prefix(self) -> str:
        if self._resolved_account_prefix:
            return self._resolved_account_prefix
        import export_vrcx_activity

        with sqlite3.connect(self.db.resolve().as_uri() + "?mode=ro", uri=True) as connection:
            prefixes = export_vrcx_activity.account_prefixes(export_vrcx_activity.table_names(connection))
        if self.account_prefix:
            if self.account_prefix not in prefixes:
                raise ValueError(f"VRCX 账户不存在：{self.account_prefix}")
            selected = self.account_prefix
        elif len(prefixes) == 1:
            selected = prefixes[0]
        elif not prefixes:
            raise ValueError("没有找到 VRCX 活动记录")
        else:
            raise ValueError("检测到多个 VRCX 账户，请使用 --account-prefix 指定账户")
        self._resolved_account_prefix = selected
        return selected

    def _export_vrcx_in_process(self, output: Path) -> None:
        """PyInstaller executables cannot use sys.executable as a Python worker."""
        import export_vrcx_activity

        arguments = [
            "export_vrcx_activity", "--db", str(self.db), "--output", str(output),
            "--account-prefix", self._resolve_account_prefix(),
        ]
        with self._export_lock:
            original = sys.argv
            stream = io.StringIO()
            try:
                sys.argv = arguments
                with redirect_stdout(stream), redirect_stderr(stream):
                    result = export_vrcx_activity.main()
                if result:
                    raise RuntimeError(stream.getvalue().strip() or "VRCX export failed")
            except SystemExit as error:
                if error.code not in (None, 0):
                    raise RuntimeError(stream.getvalue().strip() or "VRCX export failed") from error
            finally:
                sys.argv = original

    def save_mobile_upload(self, payload: bytes) -> str:
        if not payload or len(payload) > MAX_PAYLOAD_BYTES:
            raise ValueError("Payload must be between 1 byte and 32 MiB")
        document = json.loads(payload)
        if not isinstance(document, dict) or document.get("format") not in {
            "vrcmomo-activity-sync-v1", "vrcmomo-activity-sync-v2"
        }:
            raise ValueError("Unsupported VRCMomo LAN activity format")
        digest = hashlib.sha256(payload).hexdigest()
        self.inbox.mkdir(parents=True, exist_ok=True)
        target = self.inbox / f"mobile-{digest}.json"
        if not target.exists():
            temporary = target.with_suffix(".tmp")
            temporary.write_bytes(payload)
            temporary.replace(target)
            self._status("收到手机记录，已加入合并归档")
        else:
            self._status("手机记录已存在，无需重复保存")
        self.rebuild_mobile_archive()
        return digest

    def export_mobile_archive(self) -> bytes:
        """Return one canonical rebuilt document per account."""
        archive, _ = self.rebuild_mobile_archive()
        return json.dumps(archive, ensure_ascii=False, separators=(",", ":")).encode("utf-8")

    def rebuild_mobile_archive(self) -> tuple[dict, dict[str, int]]:
        documents = []
        skipped = 0
        with self._lock:
            if self.inbox.exists():
                for source in self.inbox.glob("mobile-*.json"):
                    try:
                        value = json.loads(source.read_text(encoding="utf-8"))
                        documents.extend(decode_documents(value))
                    except (OSError, ValueError, json.JSONDecodeError):
                        skipped += 1
            archive, report = merge_archive(documents, int(time.time() * 1000))
            report_dict = report.to_dict()
            report_dict["skippedFiles"] = skipped
            self._last_report = report_dict
            self.inbox.mkdir(parents=True, exist_ok=True)
            target = self.inbox / "archive-rebuilt.json"
            temporary = target.with_suffix(".tmp")
            temporary.write_text(json.dumps(archive, ensure_ascii=False, indent=2), encoding="utf-8")
            temporary.replace(target)
        self._status(
            f"归档已重建：{report.merged_events} 条事件，{report.friends} 位好友，折叠 {report.exact_duplicates + report.near_duplicates} 条重复"
        )
        return archive, report_dict

    def archive_summary(self) -> dict[str, int]:
        with self._lock:
            return dict(self._last_report)

    def vrcx_summary(self) -> dict[str, int]:
        """Count readable VRCX records without exporting their contents to the UI."""
        import export_vrcx_activity

        with sqlite3.connect(self.db.resolve().as_uri() + "?mode=ro", uri=True) as connection:
            tables = export_vrcx_activity.table_names(connection)
            prefix = self._resolve_account_prefix()

            def count(table: str) -> int:
                if table not in tables:
                    return 0
                return int(connection.execute(
                    f"SELECT COUNT(*) FROM {export_vrcx_activity.quote_identifier(table)}"
                ).fetchone()[0])

            presence = count(f"{prefix}_feed_online_offline")
            locations = count(f"{prefix}_feed_gps")
            statuses = count(f"{prefix}_feed_status")
            profiles = count(f"{prefix}_feed_bio")
            meetings = count("gamelog_join_leave")
            return {
                "presence": presence,
                "locations": locations,
                "statuses": statuses,
                "profiles": profiles,
                "meetings": meetings,
                "total": presence + locations + statuses + profiles + meetings,
            }

    def save_rebuilt_archive_as(self, target: Path) -> None:
        archive, _ = self.rebuild_mobile_archive()
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_suffix(target.suffix + ".tmp")
        temporary.write_text(json.dumps(archive, ensure_ascii=False, indent=2), encoding="utf-8")
        temporary.replace(target)
        self._status(f"已导出归档：{target.name}")

    def _status(self, message: str) -> None:
        if self.on_status:
            self.on_status(message)
        else:
            print(message)
class DiscoveryResponder(threading.Thread):
    """Answers LAN discovery with the current short-lived pairing link."""

    def __init__(self, bridge_port: int, discovery_port: int, token: str) -> None:
        super().__init__(name="vrcmomo-lan-discovery", daemon=True)
        self.bridge_port = bridge_port
        self.discovery_port = discovery_port
        self.token = token
        self._socket: socket.socket | None = None

    def run(self) -> None:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp:
            self._socket = udp
            udp.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            udp.bind(("", self.discovery_port))
            while True:
                try:
                    request, address = udp.recvfrom(1024)
                except OSError:
                    return
                if request != DISCOVERY_REQUEST:
                    continue
                response = json.dumps({
                    "service": "vrcmomo-lan-bridge",
                    "protocol": 1,
                    "port": self.bridge_port,
                    "pairingUrl": f"http://{local_ip()}:{self.bridge_port}/v1/health?token={self.token}",
                }).encode("utf-8")
                udp.sendto(response, address)

    def close(self) -> None:
        if self._socket is not None:
            self._socket.close()


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
                "mobileUploadFormats": ["vrcmomo-activity-sync-v1", "vrcmomo-activity-sync-v2"],
                "mobileArchiveFormat": "vrcmomo-activity-archive-v1",
                "serverTime": datetime.now(timezone.utc).isoformat(),
            })
        if path == "/v1/vrcmomo-activity":
            return self._bytes(HTTPStatus.OK, self.server.state.export_mobile_archive())
        if path == "/v1/vrcx-activity":
            try:
                return self._bytes(HTTPStatus.OK, self.server.state.export_vrcx_activity())
            except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
                print(f"VRCX activity export failed: {error}", file=sys.stderr)
                return self._json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": str(error)})
        return self._json(HTTPStatus.NOT_FOUND, {"error": "unknown endpoint"})

    def do_POST(self) -> None:
        if not self._authorized():
            return self._json(HTTPStatus.UNAUTHORIZED, {"error": "pairing required"})
        if urlparse(self.path).path != "/v1/vrcmomo-activity":
            return self._json(HTTPStatus.NOT_FOUND, {"error": "unknown endpoint"})
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_PAYLOAD_BYTES:
            return self._json(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, {"error": "payload must be 1 byte to 32 MiB"})
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


def create_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Serve a paired local VRCX activity bridge for VRCMomo.")
    parser.add_argument("--db", type=Path, default=Path.home() / "AppData/Roaming/VRCX/VRCX.sqlite3")
    parser.add_argument("--account-prefix", help="VRCX account table prefix when multiple accounts exist")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--discovery-port", type=int, default=DISCOVERY_PORT)
    parser.add_argument("--no-discovery", action="store_true", help="Disable LAN address discovery replies")
    parser.add_argument("--inbox", type=Path, default=default_inbox())
    parser.add_argument("--console", action="store_true", help="Use the legacy terminal interface instead of the Windows UI")
    return parser


def validate_args(parser: argparse.ArgumentParser, args) -> None:
    if not args.db.is_file():
        parser.error(f"VRCX database not found: {args.db}")
    if not 1 <= args.port <= 65535 or not 1 <= args.discovery_port <= 65535:
        parser.error("ports must be between 1 and 65535")


def run_console(args) -> int:
    token = secrets.token_urlsafe(24)
    state = BridgeState(args.db.resolve(), args.account_prefix, args.inbox.resolve(), token)
    state.rebuild_mobile_archive()
    server = BridgeServer(("0.0.0.0", args.port), state)
    discovery = None if args.no_discovery else DiscoveryResponder(args.port, args.discovery_port, token)
    discovery and discovery.start()
    address = local_ip()
    print("VRCMomo LAN Bridge is running. Keep this window open while syncing.")
    pairing_url = f"http://{address}:{args.port}/v1/health?token={token}"
    print(f"Pairing URL: {pairing_url}")
    print_pairing_qr(pairing_url)
    print("VRCX is read-only. Mobile uploads are stored in the local inbox, never written into VRCX.")
    if discovery:
        print(f"LAN pairing discovery is active on UDP {args.discovery_port}.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBridge stopped.")
    finally:
        discovery and discovery.close()
        server.server_close()
    return 0


def run_gui(args) -> int:
    import tkinter as tk
    from tkinter import filedialog, messagebox, ttk

    root = tk.Tk()
    root.title("VRCMomo 局域网桥")
    root.geometry("760x590")
    root.minsize(680, 520)
    root.configure(bg="#f4f5f2")
    icon_path = Path(__file__).with_name("vrcmomo.ico")
    if icon_path.is_file():
        root.iconbitmap(default=str(icon_path))

    colors = {
        "paper": "#f4f5f2", "surface": "#ffffff", "ink": "#202321",
        "muted": "#69706c", "line": "#d9ddda", "signal": "#96ff46", "dark": "#242824",
    }
    style = ttk.Style(root)
    style.theme_use("clam")
    style.configure("Bridge.TFrame", background=colors["paper"])
    style.configure("Surface.TFrame", background=colors["surface"])
    style.configure("Title.TLabel", background=colors["paper"], foreground=colors["ink"], font=("Microsoft YaHei UI", 21, "bold"))
    style.configure("Micro.TLabel", background=colors["paper"], foreground=colors["muted"], font=("Microsoft YaHei UI", 9))
    style.configure("CardTitle.TLabel", background=colors["surface"], foreground=colors["muted"], font=("Microsoft YaHei UI", 9))
    style.configure("Metric.TLabel", background=colors["surface"], foreground=colors["ink"], font=("Microsoft YaHei UI", 18, "bold"))
    style.configure("Body.TLabel", background=colors["surface"], foreground=colors["ink"], font=("Microsoft YaHei UI", 10))
    style.configure("Primary.TButton", background=colors["dark"], foreground="#ffffff", padding=(18, 9), borderwidth=0, font=("Microsoft YaHei UI", 10, "bold"))
    style.map("Primary.TButton", background=[("active", "#353b35")])
    style.configure("Secondary.TButton", background=colors["surface"], foreground=colors["ink"], padding=(14, 8), bordercolor=colors["line"], font=("Microsoft YaHei UI", 10))

    shell = ttk.Frame(root, style="Bridge.TFrame", padding=(28, 22))
    shell.pack(fill="both", expand=True)
    header = ttk.Frame(shell, style="Bridge.TFrame")
    header.pack(fill="x")
    ttk.Label(header, text="VRCMomo 局域网桥", style="Title.TLabel").pack(side="left")
    status_var = tk.StringVar(value="正在启动")
    status = tk.Label(header, textvariable=status_var, bg=colors["signal"], fg="#15210e", padx=12, pady=5, font=("Microsoft YaHei UI", 9, "bold"))
    status.pack(side="right")
    ttk.Label(shell, text="LOCAL ACTIVITY BRIDGE  /  仅在当前局域网运行", style="Micro.TLabel").pack(anchor="w", pady=(3, 18))

    token = secrets.token_urlsafe(24)
    address = local_ip()
    pairing_url = f"http://{address}:{args.port}/v1/health?token={token}"
    log_messages: list[str] = []

    def report_status(message: str) -> None:
        def apply():
            log_messages.insert(0, message)
            del log_messages[4:]
            activity_var.set("\n".join(log_messages))
            refresh_metrics()
        root.after(0, apply)

    state = BridgeState(args.db.resolve(), args.account_prefix, args.inbox.resolve(), token, report_status)
    server = None
    discovery = None

    content = ttk.Frame(shell, style="Bridge.TFrame")
    content.pack(fill="both", expand=True)
    content.columnconfigure(0, weight=3)
    content.columnconfigure(1, weight=2)
    content.rowconfigure(0, weight=1)

    left = ttk.Frame(content, style="Surface.TFrame", padding=20)
    left.grid(row=0, column=0, sticky="nsew", padx=(0, 10))
    right = ttk.Frame(content, style="Surface.TFrame", padding=20)
    right.grid(row=0, column=1, sticky="nsew", padx=(10, 0))

    ttk.Label(left, text="配对", style="CardTitle.TLabel").pack(anchor="w")
    qr_holder = tk.Label(left, bg=colors["surface"])
    qr_holder.pack(anchor="w", pady=(12, 8))
    try:
        from PIL import ImageTk
        qr = qrcode.QRCode(border=1, box_size=5)
        qr.add_data(pairing_url)
        qr.make(fit=True)
        qr_photo = ImageTk.PhotoImage(qr.make_image(fill_color="#202321", back_color="white").get_image())
        qr_holder.configure(image=qr_photo)
        qr_holder.image = qr_photo
    except Exception:
        qr_holder.configure(text="二维码组件不可用\n可使用手机自动发现", fg=colors["muted"], font=("Microsoft YaHei UI", 10), justify="left")

    ttk.Label(left, text=f"{address}:{args.port}", style="Metric.TLabel").pack(anchor="w")
    ttk.Label(left, text="手机可直接“寻找附近电脑”或扫码配对", style="Body.TLabel").pack(anchor="w", pady=(4, 12))
    action_row = ttk.Frame(left, style="Surface.TFrame")
    action_row.pack(fill="x")

    def copy_pairing() -> None:
        root.clipboard_clear()
        root.clipboard_append(pairing_url)
        report_status("配对地址已复制")

    ttk.Button(action_row, text="复制地址", command=copy_pairing, style="Primary.TButton").pack(side="left")
    ttk.Button(action_row, text="打开归档目录", command=lambda: os.startfile(state.inbox), style="Secondary.TButton").pack(side="left", padx=(8, 0))

    ttk.Label(right, text="VRCX 数据", style="CardTitle.TLabel").pack(anchor="w")
    vrcx_summary_var = tk.StringVar(value="正在扫描本机 VRCX…")
    ttk.Label(right, textvariable=vrcx_summary_var, style="Body.TLabel", justify="left", wraplength=250).pack(anchor="w", pady=(8, 10))

    def refresh_vrcx_summary() -> None:
        try:
            summary = state.vrcx_summary()
            text = (
                f"已读取 {summary['total']} 条记录\n"
                f"上下线 {summary['presence']} · 位置 {summary['locations']} · "
                f"状态 {summary['statuses']} · 简介 {summary['profiles']} · 同游 {summary['meetings']}"
            )
            root.after(0, lambda: vrcx_summary_var.set(text))
            report_status("VRCX 扫描完成，可供手机同步")
        except (OSError, RuntimeError, ValueError, sqlite3.Error) as error:
            root.after(0, lambda: vrcx_summary_var.set(f"无法读取 VRCX：{error}"))
            report_status("VRCX 扫描失败")

    ttk.Button(
        right,
        text="重新扫描 VRCX",
        command=lambda: threading.Thread(target=refresh_vrcx_summary, daemon=True).start(),
        style="Secondary.TButton",
    ).pack(fill="x", pady=(0, 16))

    ttk.Separator(right).pack(fill="x", pady=(0, 16))
    ttk.Label(right, text="手机合并归档", style="CardTitle.TLabel").pack(anchor="w")
    metrics = ttk.Frame(right, style="Surface.TFrame")
    metrics.pack(fill="x", pady=(14, 18))
    metric_vars = {name: tk.StringVar(value="0") for name in ("设备", "好友", "事件", "折叠")}
    for index, (name, variable) in enumerate(metric_vars.items()):
        cell = ttk.Frame(metrics, style="Surface.TFrame")
        cell.grid(row=index // 2, column=index % 2, sticky="w", padx=(0, 26), pady=(0, 12))
        ttk.Label(cell, textvariable=variable, style="Metric.TLabel").pack(anchor="w")
        ttk.Label(cell, text=name, style="CardTitle.TLabel").pack(anchor="w")

    def refresh_metrics() -> None:
        summary = state.archive_summary()
        metric_vars["设备"].set(str(summary.get("source_devices", 0)))
        metric_vars["好友"].set(str(summary.get("friends", 0)))
        metric_vars["事件"].set(str(summary.get("merged_events", 0)))
        metric_vars["折叠"].set(str(summary.get("exact_duplicates", 0) + summary.get("near_duplicates", 0)))

    def rebuild() -> None:
        threading.Thread(target=state.rebuild_mobile_archive, daemon=True).start()

    def export_archive() -> None:
        target = filedialog.asksaveasfilename(
            title="导出合并归档", defaultextension=".json",
            initialfile="VRCMomo-activity-archive.json", filetypes=[("JSON", "*.json")],
        )
        if target:
            threading.Thread(target=state.save_rebuilt_archive_as, args=(Path(target),), daemon=True).start()

    ttk.Button(right, text="重新合并", command=rebuild, style="Primary.TButton").pack(fill="x")
    ttk.Button(right, text="导出 JSON", command=export_archive, style="Secondary.TButton").pack(fill="x", pady=(8, 0))
    ttk.Separator(right).pack(fill="x", pady=18)
    ttk.Label(right, text="最近活动", style="CardTitle.TLabel").pack(anchor="w")
    activity_var = tk.StringVar(value="等待手机连接")
    ttk.Label(right, textvariable=activity_var, style="Body.TLabel", justify="left", wraplength=250).pack(anchor="w", pady=(8, 0))

    footer = ttk.Frame(shell, style="Bridge.TFrame")
    footer.pack(fill="x", pady=(14, 0))
    ttk.Label(footer, text=f"VRCX 只读  ·  {args.db.name}", style="Micro.TLabel").pack(side="left")
    ttk.Label(footer, text=f"UDP {args.discovery_port}", style="Micro.TLabel").pack(side="right")

    try:
        state.rebuild_mobile_archive()
        refresh_vrcx_summary()
        server = BridgeServer(("0.0.0.0", args.port), state)
        discovery = None if args.no_discovery else DiscoveryResponder(args.port, args.discovery_port, token)
        discovery and discovery.start()
        threading.Thread(target=server.serve_forever, name="vrcmomo-http", daemon=True).start()
        status_var.set("运行中")
        report_status("桥接已启动，等待手机连接")
    except OSError as error:
        status_var.set("启动失败")
        messagebox.showerror("无法启动桥接", str(error))

    def close() -> None:
        if server:
            server.shutdown()
            server.server_close()
        if discovery:
            discovery.close()
        root.destroy()

    root.protocol("WM_DELETE_WINDOW", close)
    root.mainloop()
    return 0


def main() -> int:
    parser = create_parser()
    args = parser.parse_args()
    validate_args(parser, args)
    if args.console:
        return run_console(args)
    try:
        return run_gui(args)
    except ImportError as error:
        print(f"GUI unavailable ({error}); falling back to console.", file=sys.stderr)
        return run_console(args)


if __name__ == "__main__":
    raise SystemExit(main())

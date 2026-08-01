#!/usr/bin/env python3
"""Export selected, non-secret VRCX activity tables into VRCMomo's bridge JSON.

This tool opens the VRCX database read-only. It never copies account settings,
cookies, passwords, tokens, or the original SQLite file.
"""

from __future__ import annotations

import argparse
import json
import os
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

FORMAT = "vrcmomo-vrcx-activity-v2"
PREFIX_SUFFIX = "_feed_online_offline"
DEFAULT_DB = Path(os.environ.get("APPDATA", "")) / "VRCX" / "VRCX.sqlite3"


def quote_identifier(value: str) -> str:
    if not value.replace("_", "").isalnum():
        raise ValueError("Unsafe SQLite identifier")
    return f'"{value}"'


def table_names(connection: sqlite3.Connection) -> set[str]:
    return {
        row[0]
        for row in connection.execute("SELECT name FROM sqlite_master WHERE type = 'table'")
    }


def account_prefixes(tables: Iterable[str]) -> list[str]:
    return sorted(
        table[: -len(PREFIX_SUFFIX)]
        for table in tables
        if table.endswith(PREFIX_SUFFIX) and table[: -len(PREFIX_SUFFIX)]
    )


def read_table(connection: sqlite3.Connection, tables: set[str], name: str, columns: list[str]) -> list[dict]:
    if name not in tables:
        return []
    available = {row[1] for row in connection.execute(f"PRAGMA table_info({quote_identifier(name)})")}
    selected = [column for column in columns if column in available]
    if not selected:
        return []
    query = f"SELECT {', '.join(quote_identifier(column) for column in selected)} FROM {quote_identifier(name)} ORDER BY created_at"
    return [dict(zip(selected, row)) for row in connection.execute(query)]


def choose_prefix(prefixes: list[str], requested: str | None) -> str:
    if requested:
        if requested not in prefixes:
            raise ValueError(f"Unknown account prefix: {requested}")
        return requested
    if len(prefixes) == 1:
        return prefixes[0]
    if not prefixes:
        raise ValueError("No VRCX activity tables were found in this database.")
    print("Multiple VRCX accounts were found:")
    for index, prefix in enumerate(prefixes, start=1):
        print(f"  {index}. {prefix}")
    while True:
        raw = input("Choose an account number: ").strip()
        if raw.isdigit() and 1 <= int(raw) <= len(prefixes):
            return prefixes[int(raw) - 1]
        print("Please enter a valid number.")


def main() -> int:
    parser = argparse.ArgumentParser(description="Export VRCX activity history for VRCMomo.")
    parser.add_argument("--db", type=Path, default=DEFAULT_DB, help="Path to VRCX.sqlite3")
    parser.add_argument("--output", type=Path, default=Path.cwd() / "vrcmomo-vrcx-activity-v1.json")
    parser.add_argument("--account-prefix", help="VRCX account-table prefix when more than one account exists")
    args = parser.parse_args()

    if not args.db.is_file():
        parser.error(f"VRCX database not found: {args.db}")

    db_uri = args.db.resolve().as_uri() + "?mode=ro"
    with sqlite3.connect(db_uri, uri=True) as connection:
        tables = table_names(connection)
        prefix = choose_prefix(account_prefixes(tables), args.account_prefix)
        payload = {
            "format": FORMAT,
            "exportedAt": datetime.now(timezone.utc).isoformat(),
            "source": {"application": "VRCX", "mode": "read-only-sqlite"},
            "account": {"vrcxTablePrefix": prefix},
            "events": {
                "presence": read_table(
                    connection, tables, f"{prefix}_feed_online_offline",
                    ["created_at", "user_id", "display_name", "type", "location", "world_name", "time", "group_name"],
                ),
                "locationChanges": read_table(
                    connection, tables, f"{prefix}_feed_gps",
                    ["created_at", "user_id", "display_name", "location", "world_name", "previous_location", "time", "group_name"],
                ),
                "statusChanges": read_table(
                    connection, tables, f"{prefix}_feed_status",
                    ["created_at", "user_id", "display_name", "status", "status_description", "previous_status", "previous_status_description"],
                ),
                "profileChanges": read_table(
                    connection, tables, f"{prefix}_feed_bio",
                    ["created_at", "user_id", "display_name", "bio", "previous_bio"],
                ),
                "avatarChanges": read_table(
                    connection, tables, f"{prefix}_feed_avatar",
                    ["created_at", "user_id", "display_name", "owner_id", "avatar_name", "current_avatar_image_url", "current_avatar_thumbnail_image_url", "previous_current_avatar_image_url", "previous_current_avatar_thumbnail_image_url"],
                ),
                "friendHistory": read_table(
                    connection, tables, f"{prefix}_friend_log_history",
                    ["created_at", "type", "user_id", "display_name", "previous_display_name", "trust_level", "previous_trust_level", "friend_number"],
                ),
                "selfLocations": read_table(
                    connection, tables, "gamelog_location",
                    ["created_at", "location", "world_id", "world_name", "time", "group_name"],
                ),
                "instanceJoinLeave": read_table(
                    connection, tables, "gamelog_join_leave",
                    ["created_at", "type", "display_name", "location", "user_id", "time"],
                ),
            },
        }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    counts = {name: len(rows) for name, rows in payload["events"].items()}
    print(f"Wrote {args.output}")
    print("Event counts: " + ", ".join(f"{name}={count}" for name, count in counts.items()))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, sqlite3.Error) as error:
        print(f"Export failed: {error}", file=sys.stderr)
        raise SystemExit(1)

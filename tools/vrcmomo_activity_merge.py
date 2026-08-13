"""Deterministic VRCMomo activity archive merge and baseline rebuild.

The module has no UI or network dependencies so the same contract can later be
ported to Kotlin. Input documents are complete device snapshots, never deltas.
"""
from __future__ import annotations

from copy import deepcopy
from collections import defaultdict
from dataclasses import dataclass, asdict
from typing import Any, Iterable

SYNC_FORMATS = {"vrcmomo-activity-sync-v1", "vrcmomo-activity-sync-v2"}
ARCHIVE_FORMAT = "vrcmomo-activity-archive-v1"
MERGED_SOURCE_PREFIX = "bridge-aggregate:"
NEAR_DUPLICATE_WINDOW_MILLIS = 120_000


@dataclass(frozen=True)
class MergeReport:
    source_documents: int = 0
    source_devices: int = 0
    owners: int = 0
    source_events: int = 0
    merged_events: int = 0
    exact_duplicates: int = 0
    near_duplicates: int = 0
    friends: int = 0
    rebuilt_meetings: int = 0

    def to_dict(self) -> dict[str, int]:
        return asdict(self)


def decode_documents(value: Any) -> list[dict[str, Any]]:
    """Accept one sync document or an archive and return validated documents."""
    if not isinstance(value, dict):
        raise ValueError("Activity archive must be a JSON object")
    if value.get("format") == ARCHIVE_FORMAT or ("documents" in value and "format" not in value):
        documents = value.get("documents", [])
    else:
        documents = [value]
    if not isinstance(documents, list):
        raise ValueError("Archive documents must be a list")
    result = []
    for document in documents:
        if not isinstance(document, dict) or document.get("format") not in SYNC_FORMATS:
            raise ValueError("Unsupported VRCMomo activity document")
        if not isinstance(document.get("ownerUserId"), str) or not document["ownerUserId"]:
            raise ValueError("Activity document is missing ownerUserId")
        result.append(migrate_document(document))
    return result


def migrate_document(document: dict[str, Any]) -> dict[str, Any]:
    """Upgrade a mobile snapshot at the bridge boundary without dropping data.

    Older VRCMomo caches predate the optional ``pronouns`` field inside the
    retained friend snapshot.  The bridge can keep such uploads for months, so
    migrate them whenever they are read instead of requiring every phone to
    upload a fresh snapshot before it can import the shared archive.
    """
    migrated = deepcopy(document)
    stats = migrated.get("statsByFriendId")
    if not isinstance(stats, dict):
        return migrated
    for stat in stats.values():
        if not isinstance(stat, dict):
            continue
        friend = stat.get("lastKnownFriend")
        if isinstance(friend, dict):
            friend.setdefault("pronouns", None)
    return migrated


def merge_archive(documents: Iterable[dict[str, Any]], exported_at_millis: int) -> tuple[dict[str, Any], MergeReport]:
    """Merge snapshots per owner and rebuild an idempotent canonical baseline."""
    source_documents = list(documents)
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for document in source_documents:
        grouped[document["ownerUserId"]].append(document)

    merged_documents = []
    source_events = merged_events = exact = near = friends = meetings = 0
    devices = set()
    for owner, owner_documents in sorted(grouped.items()):
        for document in owner_documents:
            devices.add((owner, _source_id(document)))
        merged, owner_report = _merge_owner(owner, owner_documents, exported_at_millis)
        merged_documents.append(merged)
        source_events += owner_report["sourceEvents"]
        merged_events += owner_report["mergedEvents"]
        exact += owner_report["exactDuplicates"]
        near += owner_report["nearDuplicates"]
        friends += owner_report["friends"]
        meetings += owner_report["rebuiltMeetings"]

    report = MergeReport(
        source_documents=len(source_documents),
        source_devices=len(devices),
        owners=len(grouped),
        source_events=source_events,
        merged_events=merged_events,
        exact_duplicates=exact,
        near_duplicates=near,
        friends=friends,
        rebuilt_meetings=meetings,
    )
    return {
        "format": ARCHIVE_FORMAT,
        "documents": merged_documents,
        "mergeReport": report.to_dict(),
    }, report


def _merge_owner(owner: str, documents: list[dict[str, Any]], exported_at_millis: int) -> tuple[dict[str, Any], dict[str, int]]:
    raw_events: list[dict[str, Any]] = []
    stats_sources: list[tuple[int, dict[str, Any]]] = []
    for document in documents:
        exported_at = _integer(document.get("exportedAtMillis"))
        raw_events.extend(event for event in document.get("activityEvents", []) if isinstance(event, dict))
        stats = document.get("statsByFriendId", {})
        if isinstance(stats, dict):
            stats_sources.extend((exported_at, stat) for stat in stats.values() if isinstance(stat, dict))

    events, exact, near = _merge_events(raw_events)
    stats, rebuilt_meetings = _rebuild_stats(stats_sources, events)
    return {
        "format": "vrcmomo-activity-sync-v2",
        "ownerUserId": owner,
        "exportedAtMillis": exported_at_millis,
        "sourceDeviceId": f"{MERGED_SOURCE_PREFIX}{owner}",
        "statsByFriendId": stats,
        "activityEvents": sorted(events, key=lambda item: _integer(item.get("occurredAtMillis")), reverse=True),
    }, {
        "sourceEvents": len(raw_events),
        "mergedEvents": len(events),
        "exactDuplicates": exact,
        "nearDuplicates": near,
        "friends": len(stats),
        "rebuiltMeetings": rebuilt_meetings,
    }


def _merge_events(events: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], int, int]:
    exact_seen: set[tuple[Any, ...]] = set()
    exact_unique = []
    exact_duplicates = 0
    for event in sorted(events, key=lambda item: _integer(item.get("occurredAtMillis"))):
        normalized = _normalize_event(event)
        identity = _event_identity(normalized)
        if identity in exact_seen:
            exact_duplicates += 1
            continue
        exact_seen.add(identity)
        exact_unique.append(normalized)

    # Different devices can observe one transition seconds apart. Collapse only events whose
    # semantic before/after payload is identical and which fall in a short fixed window.
    last_by_user: dict[str, dict[str, Any]] = {}
    merged = []
    near_duplicates = 0
    for event in exact_unique:
        semantic = _event_semantic_identity(event)
        user_id = str(event.get("userId", ""))
        previous = last_by_user.get(user_id)
        timestamp = _integer(event.get("occurredAtMillis"))
        if (
            previous is not None
            and _event_semantic_identity(previous) == semantic
            and timestamp - _integer(previous.get("occurredAtMillis")) <= NEAR_DUPLICATE_WINDOW_MILLIS
        ):
            near_duplicates += 1
            continue
        last_by_user[user_id] = event
        merged.append(event)
    return merged, exact_duplicates, near_duplicates


def _rebuild_stats(
    stats_sources: list[tuple[int, dict[str, Any]]],
    events: list[dict[str, Any]],
) -> tuple[dict[str, dict[str, Any]], int]:
    by_friend: dict[str, list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    for exported_at, stat in stats_sources:
        user_id = stat.get("userId")
        if isinstance(user_id, str) and user_id:
            by_friend[user_id].append((exported_at, stat))
    events_by_friend: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for event in events:
        user_id = event.get("userId")
        if isinstance(user_id, str) and user_id:
            events_by_friend[user_id].append(event)

    all_friends = set(by_friend) | set(events_by_friend)
    result = {}
    total_rebuilt_meetings = 0
    for user_id in sorted(all_friends):
        snapshots = sorted(by_friend.get(user_id, []), key=lambda item: item[0])
        newest = snapshots[-1][1] if snapshots else {"userId": user_id}
        merged = dict(newest)
        merged["userId"] = user_id
        merged.pop("activeTogetherSinceMillis", None)
        merged.pop("activeTogetherInstanceId", None)
        for field in ("meetingCount", "togetherDurationMillis"):
            merged[field] = max((_integer(stat.get(field)) for _, stat in snapshots), default=0)
        for field in (
            "lastSeenTogetherAtMillis", "lastOnlineAtMillis", "lastOfflineAtMillis", "lastActivityAtMillis"
        ):
            value = max((_integer_or_none(stat.get(field)) for _, stat in snapshots), default=None, key=lambda item: item or -1)
            if value is not None:
                merged[field] = value

        friend_events = sorted(events_by_friend.get(user_id, []), key=lambda item: _integer(item.get("occurredAtMillis")))
        meeting_count, meeting_duration, last_seen = _meeting_timeline(friend_events)
        total_rebuilt_meetings += meeting_count
        merged["meetingCount"] = max(_integer(merged.get("meetingCount")), meeting_count)
        merged["togetherDurationMillis"] = max(_integer(merged.get("togetherDurationMillis")), meeting_duration)
        if last_seen is not None:
            merged["lastSeenTogetherAtMillis"] = max(_integer(merged.get("lastSeenTogetherAtMillis")), last_seen)
        _adopt_event_timestamp(merged, friend_events, "Online", "lastOnlineAtMillis")
        _adopt_event_timestamp(merged, friend_events, "Offline", "lastOfflineAtMillis")
        if friend_events:
            merged["lastActivityAtMillis"] = max(
                _integer(merged.get("lastActivityAtMillis")),
                max(_integer(event.get("occurredAtMillis")) for event in friend_events),
            )
        result[user_id] = merged
    return result, total_rebuilt_meetings


def _meeting_timeline(events: list[dict[str, Any]]) -> tuple[int, int, int | None]:
    count = duration = 0
    opened_at: int | None = None
    last_seen: int | None = None
    for event in events:
        event_type = str(event.get("type", ""))
        timestamp = _integer(event.get("occurredAtMillis"))
        if event_type == "Met" and opened_at is None:
            opened_at = timestamp
            count += 1
            last_seen = timestamp
        elif event_type == "Left" and opened_at is not None:
            duration += max(0, timestamp - opened_at)
            opened_at = None
            last_seen = timestamp
    return count, duration, last_seen


def _adopt_event_timestamp(stats: dict[str, Any], events: list[dict[str, Any]], event_type: str, field: str) -> None:
    timestamps = [_integer(event.get("occurredAtMillis")) for event in events if event.get("type") == event_type]
    if timestamps:
        stats[field] = max(_integer(stats.get(field)), max(timestamps))


def _normalize_event(event: dict[str, Any]) -> dict[str, Any]:
    normalized = dict(event)
    event_type = str(normalized.get("type", ""))
    if event_type == "StatusChanged":
        normalized["previousValue"] = _normalize_status(normalized.get("previousValue"))
        normalized["currentValue"] = _normalize_status(normalized.get("currentValue"))
    elif event_type in {"Online", "Offline", "LocationChanged"}:
        normalized["previousValue"] = _normalize_location(normalized.get("previousValue"))
        normalized["currentValue"] = _normalize_location(normalized.get("currentValue"))
    return normalized


def _normalize_status(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    parts = value.strip().split(" · ", 1)
    status = " ".join(parts[0].replace("_", " ").lower().split())
    status = {"askme": "ask me", "joinme": "join me"}.get(status, status)
    return " · ".join([status, parts[1].strip()] if len(parts) > 1 and parts[1].strip() else [status])


def _normalize_location(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    stripped = value.strip()
    return stripped.lower() if stripped.lower() in {"offline", "web", "private", "traveling"} else stripped


def _event_identity(event: dict[str, Any]) -> tuple[Any, ...]:
    return (_event_semantic_identity(event), _integer(event.get("occurredAtMillis")))


def _event_semantic_identity(event: dict[str, Any]) -> tuple[Any, ...]:
    diff_lines = event.get("diffLines", [])
    diff = tuple(
        (bool(line.get("added")), str(line.get("text", "")))
        for line in diff_lines if isinstance(line, dict)
    ) if isinstance(diff_lines, list) else ()
    return (
        event.get("userId"), event.get("type"), event.get("previousValue"), event.get("currentValue"), diff,
    )


def _source_id(document: dict[str, Any]) -> str:
    return str(document.get("sourceDeviceId") or f"legacy:{document.get('ownerUserId', '')}")


def _integer(value: Any) -> int:
    return value if isinstance(value, int) and not isinstance(value, bool) else 0


def _integer_or_none(value: Any) -> int | None:
    return value if isinstance(value, int) and not isinstance(value, bool) else None

import unittest

from tools.vrcmomo_activity_merge import decode_documents, merge_archive


def document(source, stats=None, events=None, owner="usr_owner", exported=1_000):
    return {
        "format": "vrcmomo-activity-sync-v2",
        "ownerUserId": owner,
        "exportedAtMillis": exported,
        "sourceDeviceId": source,
        "statsByFriendId": stats or {},
        "activityEvents": events or [],
    }


def event(kind, timestamp, previous=None, current=None):
    return {
        "userId": "usr_friend", "displayName": "Friend", "type": kind,
        "occurredAtMillis": timestamp, "diffLines": [],
        "previousValue": previous, "currentValue": current,
    }


class ActivityMergeTest(unittest.TestCase):
    def test_repeated_snapshots_keep_maximum_baseline(self):
        first = document("phone-a", {"usr_friend": {
            "userId": "usr_friend", "meetingCount": 8, "togetherDurationMillis": 12_000,
        }})
        second = document("phone-b", {"usr_friend": {
            "userId": "usr_friend", "meetingCount": 5, "togetherDurationMillis": 18_000,
        }})
        archive, _ = merge_archive([first, second, second], 9_000)
        stats = archive["documents"][0]["statsByFriendId"]["usr_friend"]
        self.assertEqual(8, stats["meetingCount"])
        self.assertEqual(18_000, stats["togetherDurationMillis"])

    def test_cross_device_near_duplicates_collapse_and_status_is_normalized(self):
        first = document("phone-a", events=[event("StatusChanged", 10_000, "Active", "AskMe")])
        second = document("phone-b", events=[event("StatusChanged", 20_000, "active", "ask me")])
        archive, report = merge_archive([first, second], 30_000)
        events = archive["documents"][0]["activityEvents"]
        self.assertEqual(1, len(events))
        self.assertEqual("active", events[0]["previousValue"])
        self.assertEqual("ask me", events[0]["currentValue"])
        self.assertEqual(1, report.near_duplicates)

    def test_meeting_episodes_rebuild_count_duration_and_last_seen(self):
        source = document("phone-a", events=[
            event("Met", 1_000), event("Left", 6_000),
            event("Met", 10_000), event("Left", 18_000),
        ])
        archive, _ = merge_archive([source], 20_000)
        stats = archive["documents"][0]["statsByFriendId"]["usr_friend"]
        self.assertEqual(2, stats["meetingCount"])
        self.assertEqual(13_000, stats["togetherDurationMillis"])
        self.assertEqual(18_000, stats["lastSeenTogetherAtMillis"])

    def test_archive_and_single_document_decode(self):
        source = document("phone-a")
        self.assertEqual([source], decode_documents(source))
        self.assertEqual([source], decode_documents({"format": "vrcmomo-activity-archive-v1", "documents": [source]}))


if __name__ == "__main__":
    unittest.main()

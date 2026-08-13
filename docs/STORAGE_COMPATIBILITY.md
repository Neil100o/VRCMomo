# Persistent data compatibility

VRCMomo keeps durable, account-scoped data separate from the app version. A normal app update must not erase local history.

## Current activity-record contract

- Location: `<app-private-data>/VRCMomo/friend-activity/<ownerUserId>.json`
- Each record has `schemaVersion`; current schema is `9`.
- Pre-schema records are treated as schema `1` and migrated in place without dropping activity fields.
- Before a file migration, the original is retained as `<ownerUserId>.json.v<oldSchema>.backup`.
- Writes use a temporary file followed by an atomic move.
- A build will not overwrite a file written by a newer schema.
- Legacy Settings storage remains a one-time migration source until a durable-file write succeeds.

## Rule for future updates

Do not rename/delete persisted fields or bump `schemaVersion` without an explicit migration step and a test using a prior serialized record. Add migration steps in `FriendActivityCacheDao.migrate`, retain unknown future schemas, and never make account cleanup part of an ordinary upgrade path.

Other local caches remain account-scoped. New durable records should follow this same schema, backup, atomic-write, and downgrade-protection contract.

## Legacy 0.3.16 testing track

The old automatic-testing build uses package `io.github.vrcmteam.vrcm.debug`. Android keeps its
private JSON separate from the permanent package `io.github.neil100o.vrcmomo`, so installing the
permanent package cannot read that data directly.

Use the staged migration documented in `LEGACY_TESTING_TRACK_MIGRATION.md`. A VRCMomo sync import
merges timeline events by stable event key and merges complete cumulative snapshots by maximum
value rather than addition. Retrying the same migration therefore does not multiply meeting count
or together time.

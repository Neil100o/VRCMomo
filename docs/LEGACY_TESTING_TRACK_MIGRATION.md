# 0.3.16 testing-track migration

Most early testers received VRCMomo 0.3.16 from the old automatic update track. That APK uses:

- package: `io.github.vrcmteam.vrcm.debug`
- version code: `28`
- signing SHA-256: `126750fd1f22da40825faf829c2ab691ee2176d82b98f26d73ad6129720b60d3`

The permanent release package is `io.github.neil100o.vrcmomo` and cannot read the old package's
private JSON. Migration must therefore happen before the old app is removed.

## Safe rollout

1. Publish `VRCMomo-v0.3.20-legacy-migration.apk` as the old testing channel's migration update.
2. The migration APK keeps the old package and signer, so Android updates 0.3.16 in place.
3. In that updated app, pair with the LAN bridge and send the complete phone activity snapshot.
4. Install the permanent release package alongside it and pull the archive from the bridge.
5. Check the activity log and relationship statistics before removing the old app.

The bridge retains the full V1/V2 snapshot. Import merges immutable events by key and cumulative
phone snapshots by maximum meeting count/duration and latest timestamp. Importing the same archive
again is idempotent.

## Building the intermediate APK

Run from the repository root:

```powershell
.\gradlew.bat :composeApp:assembleDebug -PlegacyDebugSigning=true
```

This option deliberately uses the original Android debug keystore instead of the permanent
VRCMomo release key. Never use it for the final `io.github.neil100o.vrcmomo` release package.

Before publishing, verify all four values: package name, version code, version name, and signer.
`downloads/testing-channel.json` now points to this migration APK. Before moving an account to the
permanent signer, complete one bridge sync and verify the rebuilt archive contains the expected activity history.

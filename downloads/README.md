# VRCMomo test downloads

These files are the legacy Android migration build and its Windows LAN bridge.

- `VRCMomo-v0.3.20-legacy-migration.apk`: final old-signature testing-track migration build. Install it as an update over 0.3.16, then use the LAN bridge once to preserve phone activity history before moving to the permanent-signature build.
- `VRCMomo-LAN-Bridge.exe`: Windows LAN bridge. Run it on the computer with VRCX, then pair from VRCMomo to sync activity archives over the same LAN. Python and the QR helper are already included.

The bridge reads VRCX activity without copying cookies, passwords, tokens, notes or moderation data. The 0.3.20 APK is only for migration; later fixed-signature builds are published through GitHub Releases.


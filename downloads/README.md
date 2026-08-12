# VRCMomo test downloads

These files are the current Android test build and the optional, Windows-only VRCX activity exporter.

- `VRCMomo-v0.3.20.apk`: final old-signature testing-track migration build. Install it as an update over 0.3.16, then use the LAN bridge once to preserve phone activity history before moving to the permanent-signature build.
- `VRCMomo-VRCX-Activity-Export.exe`: optional read-only VRCX activity exporter. Run it on the Windows computer where VRCX is installed, then import the generated JSON from VRCMomo settings on Android.
- `VRCMomo-LAN-Bridge.exe`: Windows LAN bridge. Run it on the computer with VRCX, then pair from VRCMomo to sync activity archives over the same LAN. Python and the QR helper are already included.

The exporter does not copy VRCX cookies, passwords, tokens, notes or moderation data. Test builds are not stable releases; back up local data before testing and report reproducible issues through GitHub Issues.

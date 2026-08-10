# Android release signing

VRCMomo release APKs must always use one permanent signing key. Android rejects updates that use a different certificate, even when package name and version are correct.

The release workflow reads only these GitHub Actions secrets:

- `VRCMOMO_RELEASE_KEYSTORE_BASE64`
- `VRCMOMO_RELEASE_STORE_PASSWORD`
- `VRCMOMO_RELEASE_KEY_ALIAS`
- `VRCMOMO_RELEASE_KEY_PASSWORD`

The keystore and its password record must be backed up outside the repository. Never commit them, upload them as workflow artifacts, or regenerate a key for a normal release. A new key means existing installs cannot update in place.

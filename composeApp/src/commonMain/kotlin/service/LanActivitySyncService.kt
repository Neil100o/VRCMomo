package io.github.vrcmteam.vrcm.service

/**
 * One complete local-network exchange with a paired desktop bridge.
 *
 * The phone first contributes its snapshot, then imports the bridge's rebuilt
 * archive and VRCX's read-only history, before publishing its final merged
 * snapshot back to the bridge. This makes repeated syncs idempotent and leaves
 * both sides with the same conservative baseline.
 */
internal class LanActivitySyncService(
    private val activityService: FriendActivityService,
    private val bridgeClient: LanActivityBridgeClient,
) {
    suspend fun prepare(pairing: LanBridgePairing): LanActivitySyncPreview {
        bridgeClient.uploadVrcmomoActivity(pairing, activityService.exportLanActivitySync())
        val archive = activityService.previewVrcmomoActivityImport(
            bridgeClient.fetchVrcmomoActivityArchive(pairing),
        )
        val vrcx = activityService.previewVrcxActivityImport(
            bridgeClient.fetchVrcxActivity(pairing),
        )
        return LanActivitySyncPreview(archive = archive, vrcx = vrcx)
    }

    suspend fun apply(pairing: LanBridgePairing, preview: LanActivitySyncPreview) {
        activityService.applyVrcmomoActivityImport(preview.archive)
        activityService.applyVrcxActivityImport(preview.vrcx)
        bridgeClient.uploadVrcmomoActivity(pairing, activityService.exportLanActivitySync())
    }

    suspend fun sync(pairing: LanBridgePairing): LanActivitySyncPreview {
        val preview = prepare(pairing)
        apply(pairing, preview)
        return preview
    }
}

internal data class LanActivitySyncPreview(
    val archive: VrcmomoActivityImportPreview,
    val vrcx: VrcxActivityImportPreview,
)

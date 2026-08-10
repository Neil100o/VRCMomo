package io.github.vrcmteam.vrcm.storage.data

/** Last visible result of the optional personal LAN bridge; no payload or token is retained here. */
data class LanSyncStatus(
    val lastSuccessAtMillis: Long? = null,
    val lastDirection: String? = null,
    val lastError: String? = null,
)

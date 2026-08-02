package io.github.vrcmteam.vrcm.network.api.users

import io.github.vrcmteam.vrcm.network.supports.VRCApiException

/**
 * The Boop endpoint uses HTTP 429 for its per-recipient cooldown.  The API's
 * response text has varied, so do not rely on one English message here.
 *
 * This check is intentionally only used by the Boop action: a 429 from other
 * endpoints must still be surfaced as a normal rate-limit error.
 */
internal fun Throwable.isBoopAlreadySentError(): Boolean =
    this is VRCApiException && code == 429

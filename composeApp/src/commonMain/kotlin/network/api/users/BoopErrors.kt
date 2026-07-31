package io.github.vrcmteam.vrcm.network.api.users

import io.github.vrcmteam.vrcm.network.supports.VRCApiException

internal fun Throwable.isBoopAlreadySentError(): Boolean =
    this is VRCApiException &&
        code == 429 &&
        bodyText.contains("User already booped.", ignoreCase = true)

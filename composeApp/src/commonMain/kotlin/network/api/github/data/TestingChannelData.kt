package io.github.vrcmteam.vrcm.network.api.github.data

import kotlinx.serialization.Serializable

/** Public, non-authenticated metadata for the VRCMomo Android testing channel. */
@Serializable
data class TestingChannelData(
    val version: String,
    val notes: String = "",
    val apkUrl: String,
    val pageUrl: String,
)

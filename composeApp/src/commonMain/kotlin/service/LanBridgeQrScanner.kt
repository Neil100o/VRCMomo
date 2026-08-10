package io.github.vrcmteam.vrcm.service

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Starts a platform QR scanner and returns the raw pairing URL. */
@Composable
internal expect fun LanBridgeQrScanButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onScanned: (String) -> Unit,
)

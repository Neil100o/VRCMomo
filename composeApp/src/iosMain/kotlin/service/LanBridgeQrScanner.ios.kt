package io.github.vrcmteam.vrcm.service

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun LanBridgeQrScanButton(
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    onScanned: (String) -> Unit,
) {
    OutlinedButton(modifier = modifier, enabled = false, onClick = {}) { Text(label) }
}

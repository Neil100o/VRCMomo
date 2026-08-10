package io.github.vrcmteam.vrcm.service

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
internal actual fun LanBridgeQrScanButton(
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    onScanned: (String) -> Unit,
) {
    val options = remember {
        ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Scan VRCMomo LAN pairing QR code")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
    }
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.trim()?.takeIf(String::isNotBlank)?.let(onScanned)
    }
    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        onClick = { launcher.launch(options) },
    ) { Text(label) }
}

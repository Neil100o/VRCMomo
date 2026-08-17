package io.github.vrcmteam.vrcm.presentation.compoments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.MomoCallActionResult
import io.github.vrcmteam.vrcm.MomoCallState
import io.github.vrcmteam.vrcm.getAppPlatform
import io.github.vrcmteam.vrcm.storage.SettingsDao
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Temporary LAN-only setup for the MomoCall desktop/Android interoperability test. */
@Composable
fun MomoCallSetupDialog(
    targetUserId: String,
    onDismissRequest: () -> Unit,
) {
    val platform = getAppPlatform()
    val settingsDao = koinInject<SettingsDao>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var relayUrl by remember { mutableStateOf(settingsDao.momoCallSignalingUrl.orEmpty()) }
    var relaySecret by remember { mutableStateOf(settingsDao.momoCallSharedSecret.orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("MomoCall") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("实验性局域网语音。仅填写自己的开发信令器地址与密钥；不会传递 VRChat Cookie。")
                OutlinedTextField(
                    value = relayUrl,
                    onValueChange = { relayUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("信令地址（ws://电脑IP:38700）") },
                )
                OutlinedTextField(
                    value = relaySecret,
                    onValueChange = { relaySecret = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("开发信令密钥") },
                )
                status?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    settingsDao.momoCallSignalingUrl = relayUrl
                    settingsDao.momoCallSharedSecret = relaySecret
                    scope.launch {
                        status = when (platform.connectMomoCall()) {
                            MomoCallActionResult.Started -> when (platform.callMomoUser(targetUserId)) {
                                MomoCallActionResult.Started -> "正在呼叫"
                                MomoCallActionResult.PermissionRequired -> "请允许麦克风权限后再点一次"
                                else -> "无法发起呼叫"
                            }
                            MomoCallActionResult.PermissionRequired -> "请允许麦克风权限后再点一次"
                            else -> "无法连接信令器"
                        }
                    }
                },
            ) { Text("呼叫") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } },
    )
}

/** App-level incoming-call prompt, independent of the current profile page. */
@Composable
fun MomoCallIncomingOverlay() {
    val platform = getAppPlatform()
    val state by platform.momoCallState.collectAsState()
    val incoming = state as? MomoCallState.Incoming ?: return
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { scope.launch { platform.rejectMomoCall() } },
        title = { Text("MomoCall 来电") },
        text = { Text("${incoming.fromUserId} 正在呼叫你") },
        confirmButton = { TextButton(onClick = { scope.launch { platform.acceptMomoCall() } }) { Text("接听") } },
        dismissButton = { TextButton(onClick = { scope.launch { platform.rejectMomoCall() } }) { Text("拒绝") } },
    )
}

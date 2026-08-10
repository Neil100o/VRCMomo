package io.github.vrcmteam.vrcm.presentation.compoments

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.presentation.supports.AppIcons
import kotlinx.coroutines.launch

/** Shows the public VRChat address for a profile and copies it without opening a share sheet. */
@Composable
fun OfficialUrlRow(
    url: String,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val locale = strings

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = url,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = {
                    val copied = runCatching { clipboard.setText(AnnotatedString(url)) }.isSuccess
                    scope.launch {
                        SharedFlowCentre.toastText.emit(
                            if (copied) ToastText.Success(locale.officialUrlCopied)
                            else ToastText.Error(locale.officialUrlCopyFailed),
                        )
                    }
                },
            ) {
                Icon(AppIcons.Link, contentDescription = locale.copyOfficialUrl)
            }
        }
    }
}

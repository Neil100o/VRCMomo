package io.github.vrcmteam.vrcm.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.network.api.users.data.BoopData

/**
 * VRChat default Boop emoji constants.
 *
 * emojiId accepts string constants for default emojis or FileIDs for custom
 * inventory emojis.
 */
private val defaultBoops = listOf(
    "🎁" to "Gift",
    "🎁" to "Gifts",
    "💍" to "Life Ring",
    "🌿" to "Mistletoe",
    "💰" to "Money",
    "🕶️" to "Neon Shades",
    "🧴" to "Sun Lotion",
    "👻" to "Boo",
    "💔" to "Broken Heart",
    "❗" to "Exclamation",
    "▶️" to "Go",
    "❤️" to "Heart",
    "🎵" to "Music Note",
    "❓" to "Question",
    "🛑" to "Stop",
    "💤" to "Zzz",
)

@Composable
fun BoopSelector(
    onSelect: (BoopData) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Boop") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                defaultBoops.forEach { (display, id) ->
                    TextButton(
                        modifier = Modifier.padding(4.dp),
                        onClick = {
                            onSelect(BoopData(emojiId = id))
                        },
                    ) {
                        Text(display)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

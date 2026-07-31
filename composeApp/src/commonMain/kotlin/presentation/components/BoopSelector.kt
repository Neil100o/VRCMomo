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
 * VRChat default Boop emoji mapping.
 *
 * Keep display names and API ids separate so custom emoji support can be added
 * later without changing the selector UI.
 */
data class BoopEmoji(
    val name: String,
    val emojiId: String,
    val display: String,
)

private val defaultBoops = listOf(
    BoopEmoji("Gift", "Gift", "🎁"),
    BoopEmoji("Gifts", "Gifts", "🎁"),
    BoopEmoji("Life Ring", "Life Ring", "💍"),
    BoopEmoji("Mistletoe", "Mistletoe", "🌿"),
    BoopEmoji("Money", "Money", "💰"),
    BoopEmoji("Neon Shades", "Neon Shades", "🕶️"),
    BoopEmoji("Sun Lotion", "Sun Lotion", "🧴"),
    BoopEmoji("Boo", "Boo", "👻"),
    BoopEmoji("Broken Heart", "Broken Heart", "💔"),
    BoopEmoji("Exclamation", "Exclamation", "❗"),
    BoopEmoji("Go", "Go", "▶️"),
    BoopEmoji("Heart", "Heart", "❤️"),
    BoopEmoji("Music Note", "Music Note", "🎵"),
    BoopEmoji("Question", "Question", "❓"),
    BoopEmoji("Stop", "Stop", "🛑"),
    BoopEmoji("Zzz", "Zzz", "💤"),
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
                defaultBoops.forEach { boop ->
                    TextButton(
                        modifier = Modifier.padding(4.dp),
                        onClick = {
                            onSelect(BoopData(emojiId = boop.emojiId))
                        },
                    ) {
                        Text(boop.display)
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

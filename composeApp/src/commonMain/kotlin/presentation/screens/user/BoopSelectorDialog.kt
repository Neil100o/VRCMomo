package io.github.vrcmteam.vrcm.presentation.screens.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.network.api.users.data.BoopData
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings

internal data class BoopEmojiOption(
    val name: String,
    val request: BoopData,
)

private val defaultBoopEmojiOptions = listOf(
    BoopEmojiOption("Default", BoopData()),
    BoopEmojiOption("Gift", BoopData(emojiId = "Gift")),
    BoopEmojiOption("Gifts", BoopData(emojiId = "Gifts")),
    BoopEmojiOption("Life Ring", BoopData(emojiId = "Life Ring")),
    BoopEmojiOption("Mistletoe", BoopData(emojiId = "Mistletoe")),
    BoopEmojiOption("Money", BoopData(emojiId = "Money")),
    BoopEmojiOption("Neon Shades", BoopData(emojiId = "Neon Shades")),
    BoopEmojiOption("Sun Lotion", BoopData(emojiId = "Sun Lotion")),
    BoopEmojiOption("Boo", BoopData(emojiId = "Boo")),
    BoopEmojiOption("Broken Heart", BoopData(emojiId = "Broken Heart")),
    BoopEmojiOption("Exclamation", BoopData(emojiId = "Exclamation")),
    BoopEmojiOption("Go", BoopData(emojiId = "Go")),
    BoopEmojiOption("Heart", BoopData(emojiId = "Heart")),
    BoopEmojiOption("Music Note", BoopData(emojiId = "Music Note")),
    BoopEmojiOption("Question", BoopData(emojiId = "Question")),
    BoopEmojiOption("Stop", BoopData(emojiId = "Stop")),
    BoopEmojiOption("Zzz", BoopData(emojiId = "Zzz")),
)

@Composable
internal fun BoopSelectorDialog(
    onDismissRequest: () -> Unit,
    onSelect: (BoopData) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(strings.profileBoopChooseEmoji) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(defaultBoopEmojiOptions, key = { it.name }) { option ->
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(option.request) },
                    ) {
                        Text(
                            if (option.name == "Default") strings.profileBoopDefaultEmoji else option.name,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(strings.cancel)
            }
        },
    )
}

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
    BoopEmojiOption("Angry", BoopData(emojiId = "default_angry")),
    BoopEmojiOption("Arrow Point", BoopData(emojiId = "default_arrowpoint")),
    BoopEmojiOption("Bats", BoopData(emojiId = "default_bats")),
    BoopEmojiOption("Beachball", BoopData(emojiId = "default_beachball")),
    BoopEmojiOption("Beer", BoopData(emojiId = "default_beer")),
    BoopEmojiOption("Blushing", BoopData(emojiId = "default_blushing")),
    BoopEmojiOption("Boo", BoopData(emojiId = "default_boo")),
    BoopEmojiOption("Broken Heart", BoopData(emojiId = "default_broken_heart")),
    BoopEmojiOption("Candy", BoopData(emojiId = "default_candy")),
    BoopEmojiOption("Candy Cane", BoopData(emojiId = "default_candy_cane")),
    BoopEmojiOption("Candy Corn", BoopData(emojiId = "default_candy_corn")),
    BoopEmojiOption("CantSee", BoopData(emojiId = "default_cantsee")),
    BoopEmojiOption("Champagne", BoopData(emojiId = "default_champagne")),
    BoopEmojiOption("Cloud", BoopData(emojiId = "default_cloud")),
    BoopEmojiOption("Coal", BoopData(emojiId = "default_coal")),
    BoopEmojiOption("Confetti", BoopData(emojiId = "default_confetti")),
    BoopEmojiOption("Crying", BoopData(emojiId = "default_crying")),
    BoopEmojiOption("Drink", BoopData(emojiId = "default_drink")),
    BoopEmojiOption("Exclamation", BoopData(emojiId = "default_exclamation")),
    BoopEmojiOption("Fire", BoopData(emojiId = "default_fire")),
    BoopEmojiOption("Frown", BoopData(emojiId = "default_frown")),
    BoopEmojiOption("Gift", BoopData(emojiId = "default_gift")),
    BoopEmojiOption("Gifts", BoopData(emojiId = "default_gifts")),
    BoopEmojiOption("Gingerbread", BoopData(emojiId = "default_gingerbread")),
    BoopEmojiOption("Go", BoopData(emojiId = "default_go")),
    BoopEmojiOption("Hand Wave", BoopData(emojiId = "default_hand_wave")),
    BoopEmojiOption("Hang Ten", BoopData(emojiId = "default_hang_ten")),
    BoopEmojiOption("Heart", BoopData(emojiId = "default_heart")),
    BoopEmojiOption("Hourglass", BoopData(emojiId = "default_hourglass")),
    BoopEmojiOption("Ice Cream", BoopData(emojiId = "default_ice_cream")),
    BoopEmojiOption("In Love", BoopData(emojiId = "default_in_love")),
    BoopEmojiOption("Jack O Lantern", BoopData(emojiId = "default_jack_o_lantern")),
    BoopEmojiOption("Keyboard", BoopData(emojiId = "default_keyboard")),
    BoopEmojiOption("Kiss", BoopData(emojiId = "default_kiss")),
    BoopEmojiOption("Laugh", BoopData(emojiId = "default_laugh")),
    BoopEmojiOption("Life Ring", BoopData(emojiId = "default_life_ring")),
    BoopEmojiOption("Mistletoe", BoopData(emojiId = "default_mistletoe")),
    BoopEmojiOption("Money", BoopData(emojiId = "default_money")),
    BoopEmojiOption("Music Note", BoopData(emojiId = "default_music_note")),
    BoopEmojiOption("Neon Shades", BoopData(emojiId = "default_neon_shades")),
    BoopEmojiOption("NoHeadphones", BoopData(emojiId = "default_noheadphones")),
    BoopEmojiOption("NoMic", BoopData(emojiId = "default_nomic")),
    BoopEmojiOption("Pineapple", BoopData(emojiId = "default_pineapple")),
    BoopEmojiOption("Pizza", BoopData(emojiId = "default_pizza")),
    BoopEmojiOption("Portal", BoopData(emojiId = "default_portal")),
    BoopEmojiOption("Question", BoopData(emojiId = "default_question")),
    BoopEmojiOption("Shush", BoopData(emojiId = "default_shush")),
    BoopEmojiOption("Skull", BoopData(emojiId = "default_skull")),
    BoopEmojiOption("Smile", BoopData(emojiId = "default_smile")),
    BoopEmojiOption("Snow Fall", BoopData(emojiId = "default_snow_fall")),
    BoopEmojiOption("Snowball", BoopData(emojiId = "default_snowball")),
    BoopEmojiOption("Splash", BoopData(emojiId = "default_splash")),
    BoopEmojiOption("Spooky Ghost", BoopData(emojiId = "default_spooky_ghost")),
    BoopEmojiOption("Stoic", BoopData(emojiId = "default_stoic")),
    BoopEmojiOption("Stop", BoopData(emojiId = "default_stop")),
    BoopEmojiOption("Sun Lotion", BoopData(emojiId = "default_sun_lotion")),
    BoopEmojiOption("Sunglasses", BoopData(emojiId = "default_sunglasses")),
    BoopEmojiOption("Thinking", BoopData(emojiId = "default_thinking")),
    BoopEmojiOption("Thumbs Down", BoopData(emojiId = "default_thumbs_down")),
    BoopEmojiOption("Thumbs Up", BoopData(emojiId = "default_thumbs_up")),
    BoopEmojiOption("Tomato", BoopData(emojiId = "default_tomato")),
    BoopEmojiOption("Tongue Out", BoopData(emojiId = "default_tongue_out")),
    BoopEmojiOption("Web", BoopData(emojiId = "default_web")),
    BoopEmojiOption("Wow", BoopData(emojiId = "default_wow")),
    BoopEmojiOption("Zzz", BoopData(emojiId = "default_zzz")),
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

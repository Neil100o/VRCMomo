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
    BoopEmojiOption("Banana", BoopData(emojiId = "boop_banana")),
    BoopEmojiOption("Bread", BoopData(emojiId = "boop_bread")),
    BoopEmojiOption("Broken Heart", BoopData(emojiId = "boop_broken_heart")),
    BoopEmojiOption("Bunny", BoopData(emojiId = "boop_bunny")),
    BoopEmojiOption("Carrot", BoopData(emojiId = "boop_carrot")),
    BoopEmojiOption("Cat", BoopData(emojiId = "boop_cat")),
    BoopEmojiOption("Coffee", BoopData(emojiId = "boop_coffee")),
    BoopEmojiOption("Confetti", BoopData(emojiId = "boop_confetti")),
    BoopEmojiOption("Dice", BoopData(emojiId = "boop_dice")),
    BoopEmojiOption("Donut", BoopData(emojiId = "boop_donut")),
    BoopEmojiOption("Egg", BoopData(emojiId = "boop_egg")),
    BoopEmojiOption("Eight Ball", BoopData(emojiId = "boop_eight_ball")),
    BoopEmojiOption("Fire", BoopData(emojiId = "boop_fire")),
    BoopEmojiOption("Gift", BoopData(emojiId = "boop_gift")),
    BoopEmojiOption("Grapes", BoopData(emojiId = "boop_grapes")),
    BoopEmojiOption("Hand Heart", BoopData(emojiId = "boop_hand_heart")),
    BoopEmojiOption("Hand Rock", BoopData(emojiId = "boop_hand_rock")),
    BoopEmojiOption("Hand Scissors", BoopData(emojiId = "boop_hand_scissors")),
    BoopEmojiOption("Hand Spock", BoopData(emojiId = "boop_hand_spock")),
    BoopEmojiOption("Thumbs Down", BoopData(emojiId = "boop_hand_thumbs_down")),
    BoopEmojiOption("Thumbs Up", BoopData(emojiId = "boop_hand_thumbs_up")),
    BoopEmojiOption("Victory", BoopData(emojiId = "boop_hand_victory")),
    BoopEmojiOption("Heart", BoopData(emojiId = "boop_heart")),
    BoopEmojiOption("Heart Exclamation", BoopData(emojiId = "boop_heavy_heart_exclamation")),
    BoopEmojiOption("Honey", BoopData(emojiId = "boop_honey")),
    BoopEmojiOption("Hug", BoopData(emojiId = "boop_hug")),
    BoopEmojiOption("Lizard", BoopData(emojiId = "boop_lizard")),
    BoopEmojiOption("Money", BoopData(emojiId = "boop_money")),
    BoopEmojiOption("Mushroom", BoopData(emojiId = "boop_mushroom")),
    BoopEmojiOption("Nerd", BoopData(emojiId = "boop_nerd")),
    BoopEmojiOption("Pancakes", BoopData(emojiId = "boop_pancakes")),
    BoopEmojiOption("Party", BoopData(emojiId = "boop_party")),
    BoopEmojiOption("Peach", BoopData(emojiId = "boop_peach")),
    BoopEmojiOption("Penguin", BoopData(emojiId = "boop_penguin")),
    BoopEmojiOption("Pickaxe", BoopData(emojiId = "boop_pickaxe")),
    BoopEmojiOption("Pig", BoopData(emojiId = "boop_pig")),
    BoopEmojiOption("Pizza", BoopData(emojiId = "boop_pizza")),
    BoopEmojiOption("Poop", BoopData(emojiId = "boop_poop")),
    BoopEmojiOption("Pumpkin", BoopData(emojiId = "boop_pumpkin")),
    BoopEmojiOption("Punch", BoopData(emojiId = "boop_punch")),
    BoopEmojiOption("Question", BoopData(emojiId = "boop_question")),
    BoopEmojiOption("Rainbow", BoopData(emojiId = "boop_rainbow")),
    BoopEmojiOption("Santa", BoopData(emojiId = "boop_santa")),
    BoopEmojiOption("Sheep", BoopData(emojiId = "boop_sheep")),
    BoopEmojiOption("Shoe", BoopData(emojiId = "boop_shoe")),
    BoopEmojiOption("Skull", BoopData(emojiId = "boop_skull")),
    BoopEmojiOption("Strawberry", BoopData(emojiId = "boop_strawberry")),
    BoopEmojiOption("Sun", BoopData(emojiId = "boop_sun")),
    BoopEmojiOption("Surprised", BoopData(emojiId = "boop_surprised")),
    BoopEmojiOption("Sushi", BoopData(emojiId = "boop_sushi")),
    BoopEmojiOption("Tangerine", BoopData(emojiId = "boop_tangerine")),
    BoopEmojiOption("Toilet Paper", BoopData(emojiId = "boop_toilet_paper")),
    BoopEmojiOption("Tomato", BoopData(emojiId = "boop_tomato")),
    BoopEmojiOption("Uwu", BoopData(emojiId = "boop_uwu")),
    BoopEmojiOption("Watermelon", BoopData(emojiId = "boop_watermelon")),
    BoopEmojiOption("Waving", BoopData(emojiId = "boop_waving")),
    BoopEmojiOption("Winking", BoopData(emojiId = "boop_winking")),
    BoopEmojiOption("Wizard", BoopData(emojiId = "boop_wizard")),
    BoopEmojiOption("Yummy", BoopData(emojiId = "boop_yummy")),
    BoopEmojiOption("Zombie", BoopData(emojiId = "boop_zombie")),
    BoopEmojiOption("Zzz", BoopData(emojiId = "boop_zzz")),
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

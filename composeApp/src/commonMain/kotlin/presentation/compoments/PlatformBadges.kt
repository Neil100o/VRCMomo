package io.github.vrcmteam.vrcm.presentation.compoments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.network.api.avatars.data.AvatarUnityPackage
import io.github.vrcmteam.vrcm.network.api.files.data.PlatformType
import io.github.vrcmteam.vrcm.network.api.worlds.data.UnityPackage
import io.github.vrcmteam.vrcm.presentation.supports.AppIcons

fun List<UnityPackage>.availableWorldPlatforms(): List<PlatformType> =
    map { it.platform }.distinct().sortedBy(PlatformType::ordinal)

fun List<AvatarUnityPackage>.availableAvatarPlatforms(): List<PlatformType> =
    mapNotNull { pkg ->
        when (pkg.platform?.lowercase()) {
            "android" -> PlatformType.Android
            "ios" -> PlatformType.Ios
            "standalonewindows", "windows" -> PlatformType.Windows
            else -> null
        }
    }.distinct().sortedBy(PlatformType::ordinal)

@Composable
fun PlatformBadgeRow(
    platforms: List<PlatformType>,
    modifier: Modifier = Modifier,
) {
    if (platforms.isEmpty()) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            platforms.forEach { platform ->
                val (icon, description) = platform.iconAndDescription()
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private fun PlatformType.iconAndDescription(): Pair<ImageVector, String> = when (this) {
    PlatformType.Android -> AppIcons.Android to "Android"
    PlatformType.Ios -> AppIcons.Apple to "iOS"
    PlatformType.Windows -> AppIcons.Windows to "Windows"
}

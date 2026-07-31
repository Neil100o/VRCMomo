package io.github.vrcmteam.vrcm.presentation.theme.lime

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.vrcmteam.vrcm.presentation.settings.theme.ThemeColor

/** A high-clarity lime and paper-gray option for focused, readable utility screens. */
val SignalLimeThemeColor = ThemeColor(
    name = "Signal Lime",
    lightColorScheme = lightColorScheme(
        primary = Color(0xFF556000), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE4F677), onPrimaryContainer = Color(0xFF171D00),
        secondary = Color(0xFF55554B), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE1E2D2), onSecondaryContainer = Color(0xFF1B1C14),
        tertiary = Color(0xFF386A4B), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBAF2C9), onTertiaryContainer = Color(0xFF002111),
        background = Color(0xFFF7F7F2), onBackground = Color(0xFF1B1C18),
        surface = Color(0xFFF7F7F2), onSurface = Color(0xFF1B1C18),
        surfaceVariant = Color(0xFFE3E4D8), onSurfaceVariant = Color(0xFF46473E),
        outline = Color(0xFF77786D), outlineVariant = Color(0xFFC7C8BC),
        inverseSurface = Color(0xFF30302B), inverseOnSurface = Color(0xFFF2F1EB),
        inversePrimary = Color(0xFFCBDF60),
        surfaceDim = Color(0xFFDBDBD5), surfaceBright = Color(0xFFF7F7F2),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF1F1EB),
        surfaceContainer = Color(0xFFEDEDE7), surfaceContainerHigh = Color(0xFFE7E7E1),
        surfaceContainerHighest = Color(0xFFE1E1DB),
    ),
    darkColorScheme = darkColorScheme(
        primary = Color(0xFFCBDF60), onPrimary = Color(0xFF293100),
        primaryContainer = Color(0xFF3E4900), onPrimaryContainer = Color(0xFFE4F677),
        secondary = Color(0xFFC5C6B8), onSecondary = Color(0xFF2F3028),
        secondaryContainer = Color(0xFF45463D), onSecondaryContainer = Color(0xFFE1E2D2),
        tertiary = Color(0xFF9ED5AD), onTertiary = Color(0xFF07381E),
        tertiaryContainer = Color(0xFF205234), onTertiaryContainer = Color(0xFFBAF2C9),
        background = Color(0xFF12130F), onBackground = Color(0xFFE4E3DD),
        surface = Color(0xFF12130F), onSurface = Color(0xFFE4E3DD),
        surfaceVariant = Color(0xFF46473E), onSurfaceVariant = Color(0xFFC7C8BC),
        outline = Color(0xFF919286), outlineVariant = Color(0xFF46473E),
        inverseSurface = Color(0xFFE4E3DD), inverseOnSurface = Color(0xFF30302B),
        inversePrimary = Color(0xFF556000),
        surfaceDim = Color(0xFF12130F), surfaceBright = Color(0xFF3B3C36),
        surfaceContainerLowest = Color(0xFF0D0E0A), surfaceContainerLow = Color(0xFF1B1C17),
        surfaceContainer = Color(0xFF1F201B), surfaceContainerHigh = Color(0xFF2A2A25),
        surfaceContainerHighest = Color(0xFF35352F),
    ),
)

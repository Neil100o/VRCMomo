package io.github.vrcmteam.vrcm.presentation.theme.momo

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.vrcmteam.vrcm.presentation.settings.theme.ThemeColor

/** VRCMomo's calm, warm default palette. */
val MomoThemeColor = ThemeColor(
    name = "Momo",
    lightColorScheme = lightColorScheme(
        primary = Color(0xFF9C4F16), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBC5), onPrimaryContainer = Color(0xFF351000),
        secondary = Color(0xFF765846), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDBC8), onSecondaryContainer = Color(0xFF2C1609),
        tertiary = Color(0xFF6D5E22), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF8E9A6), onTertiaryContainer = Color(0xFF211B00),
        background = Color(0xFFFFF8F3), onBackground = Color(0xFF221A16),
        surface = Color(0xFFFFF8F3), onSurface = Color(0xFF221A16),
        surfaceVariant = Color(0xFFF4DED2), onSurfaceVariant = Color(0xFF54433A),
        outline = Color(0xFF897268), outlineVariant = Color(0xFFDDC2B5),
        inverseSurface = Color(0xFF382F2A), inverseOnSurface = Color(0xFFFFEDE5),
        inversePrimary = Color(0xFFFFB783),
        surfaceDim = Color(0xFFE7D8D0), surfaceBright = Color(0xFFFFF8F3),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFF1EA),
        surfaceContainer = Color(0xFFFBEAE1), surfaceContainerHigh = Color(0xFFF5E4DB),
        surfaceContainerHighest = Color(0xFFEFDCD3),
    ),
    darkColorScheme = darkColorScheme(
        primary = Color(0xFFFFB783), onPrimary = Color(0xFF542000),
        primaryContainer = Color(0xFF793800), onPrimaryContainer = Color(0xFFFFDBC5),
        secondary = Color(0xFFE5BEA8), onSecondary = Color(0xFF432A1B),
        secondaryContainer = Color(0xFF5C4030), onSecondaryContainer = Color(0xFFFFDBC8),
        tertiary = Color(0xFFDBC985), onTertiary = Color(0xFF393000),
        tertiaryContainer = Color(0xFF534900), onTertiaryContainer = Color(0xFFF8E9A6),
        background = Color(0xFF1A120E), onBackground = Color(0xFFF1DFD6),
        surface = Color(0xFF1A120E), onSurface = Color(0xFFF1DFD6),
        surfaceVariant = Color(0xFF54433A), onSurfaceVariant = Color(0xFFD9C2B6),
        outline = Color(0xFFA98C7D), outlineVariant = Color(0xFF54433A),
        inverseSurface = Color(0xFFF1DFD6), inverseOnSurface = Color(0xFF382F2A),
        inversePrimary = Color(0xFF9C4F16),
        surfaceDim = Color(0xFF1A120E), surfaceBright = Color(0xFF433832),
        surfaceContainerLowest = Color(0xFF140D0A), surfaceContainerLow = Color(0xFF221914),
        surfaceContainer = Color(0xFF261D18), surfaceContainerHigh = Color(0xFF312721),
        surfaceContainerHighest = Color(0xFF3C312B),
    ),
)

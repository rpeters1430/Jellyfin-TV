package com.example.jellyfintv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.*

enum class AppThemePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val deepBackground: Color,
    val cardSurface: Color,
    val cardSurfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val focusRing: Color,
    val gradientStart: Color,
    val gradientEnd: Color
) {
    JELLYFIN(
        id = "JELLYFIN",
        displayName = "Jellyfin Classic",
        description = "Iconic Jellyfin Blue & Purple gradient on deep space navy",
        deepBackground = Color(0xFF0C0F17),
        cardSurface = Color(0xFF161B29),
        cardSurfaceVariant = Color(0xFF22293C),
        primary = Color(0xFF00A4DC),
        secondary = Color(0xFFAA5CC3),
        focusRing = Color(0xFF00A4DC),
        gradientStart = Color(0xFF00A4DC),
        gradientEnd = Color(0xFFAA5CC3)
    ),
    JELLYFIN_PURPLE(
        id = "JELLYFIN_PURPLE",
        displayName = "Jellyfin Amethyst",
        description = "Deep violet Jellyfin atmosphere with neon amethyst accents",
        deepBackground = Color(0xFF0E0917),
        cardSurface = Color(0xFF191026),
        cardSurfaceVariant = Color(0xFF28183E),
        primary = Color(0xFFAA5CC3),
        secondary = Color(0xFF00A4DC),
        focusRing = Color(0xFFC084FC),
        gradientStart = Color(0xFFAA5CC3),
        gradientEnd = Color(0xFFC084FC)
    ),
    JELLYFIN_OLED(
        id = "JELLYFIN_OLED",
        displayName = "Jellyfin OLED Black",
        description = "True pitch black background for OLED screens with bright Jellyfin cyan",
        deepBackground = Color(0xFF000000),
        cardSurface = Color(0xFF0F1116),
        cardSurfaceVariant = Color(0xFF1C2028),
        primary = Color(0xFF00A4DC),
        secondary = Color(0xFFAA5CC3),
        focusRing = Color(0xFF38BDF8),
        gradientStart = Color(0xFF00A4DC),
        gradientEnd = Color(0xFF818CF8)
    ),
    EMERALD(
        id = "EMERALD",
        displayName = "Emerald Stream",
        description = "Vibrant emerald green with cyan highlights",
        deepBackground = Color(0xFF05120E),
        cardSurface = Color(0xFF0C221A),
        cardSurfaceVariant = Color(0xFF14362A),
        primary = Color(0xFF10B981),
        secondary = Color(0xFF06B6D4),
        focusRing = Color(0xFF34D399),
        gradientStart = Color(0xFF10B981),
        gradientEnd = Color(0xFF06B6D4)
    ),
    CRIMSON(
        id = "CRIMSON",
        displayName = "Crimson Cinema",
        description = "Rich theater red with warm amber glow",
        deepBackground = Color(0xFF130609),
        cardSurface = Color(0xFF220C11),
        cardSurfaceVariant = Color(0xFF38131B),
        primary = Color(0xFFE11D48),
        secondary = Color(0xFFF59E0B),
        focusRing = Color(0xFFFB7185),
        gradientStart = Color(0xFFE11D48),
        gradientEnd = Color(0xFFF43F5E)
    ),
    CYBERPUNK(
        id = "CYBERPUNK",
        displayName = "Cyberpunk Neon",
        description = "Electric neon cyan and hot magenta glow",
        deepBackground = Color(0xFF090614),
        cardSurface = Color(0xFF140D2B),
        cardSurfaceVariant = Color(0xFF221644),
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFFFF007F),
        focusRing = Color(0xFF00E5FF),
        gradientStart = Color(0xFF00E5FF),
        gradientEnd = Color(0xFFFF007F)
    );

    val accentBrush: Brush
        get() = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))

    companion object {
        fun fromId(id: String?): AppThemePreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: JELLYFIN
        }
    }
}

val DeepBackground: Color
    @Composable get() = LocalAppTheme.current.deepBackground

val CardSurface: Color
    @Composable get() = LocalAppTheme.current.cardSurface

val CardSurfaceVariant: Color
    @Composable get() = LocalAppTheme.current.cardSurfaceVariant

val JellyfinBlue: Color
    @Composable get() = LocalAppTheme.current.primary

val JellyfinPurple: Color
    @Composable get() = LocalAppTheme.current.secondary

val AccentGradientStart: Color
    @Composable get() = LocalAppTheme.current.gradientStart

val AccentGradientEnd: Color
    @Composable get() = LocalAppTheme.current.gradientEnd

val FocusRingColor: Color
    @Composable get() = LocalAppTheme.current.focusRing

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val RatingStarColor = Color(0xFFFFB800)

val LocalAppTheme = staticCompositionLocalOf { AppThemePreset.JELLYFIN }

@Composable
fun JellyfinTVTheme(
    preset: AppThemePreset = AppThemePreset.JELLYFIN,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        background = preset.deepBackground,
        surface = preset.cardSurface,
        surfaceVariant = preset.cardSurfaceVariant,
        primary = preset.primary,
        onPrimary = Color.White,
        secondary = preset.secondary,
        onSecondary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        border = preset.focusRing
    )

    CompositionLocalProvider(LocalAppTheme provides preset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}

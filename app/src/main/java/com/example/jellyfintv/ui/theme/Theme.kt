package com.example.jellyfintv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.*

val DeepBackground = Color(0xFF0C0F17)
val CardSurface = Color(0xFF161B29)
val CardSurfaceVariant = Color(0xFF22293C)
val JellyfinBlue = Color(0xFF00A4DC)
val JellyfinPurple = Color(0xFFAA5CC3)
val AccentGradientStart = Color(0xFF00A4DC)
val AccentGradientEnd = Color(0xFFAA5CC3)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val FocusRingColor = Color(0xFF38BDF8)
val RatingStarColor = Color(0xFFFFB800)

private val DarkTVColorScheme = darkColorScheme(
    background = DeepBackground,
    surface = CardSurface,
    surfaceVariant = CardSurfaceVariant,
    primary = JellyfinBlue,
    onPrimary = Color.White,
    secondary = JellyfinPurple,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    border = FocusRingColor
)

@Composable
fun JellyfinTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkTVColorScheme,
        typography = Typography(),
        content = content
    )
}

package com.ijad.breeze.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ijad.breeze.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = BreezeBlueSoft,
    onPrimary = Color.White,
    secondary = BreezeBlue,
    onSecondary = Color.White,
    tertiary = BreezeGlow,
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    error = Danger
)

private val LightColors = lightColorScheme(
    primary = BreezeBlue,
    onPrimary = Color.White,
    secondary = BreezeBlueDeep,
    onSecondary = Color.White,
    tertiary = BreezeBlueSoft,
    background = Mist,
    onBackground = Ink,
    surface = MistCard,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Slate,
    outline = Color(0xFFCBD5E1),
    primaryContainer = AccentContainer,
    onPrimaryContainer = BreezeBlueDeep,
    error = Danger
)

@Composable
fun BreezeTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

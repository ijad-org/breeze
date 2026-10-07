package com.ijad.breeze.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.ijad.breeze.R
import com.ijad.breeze.data.AcMode

/** True when the user turned animations off (Android's prefers-reduced-motion equivalent). */
val LocalReduceMotion = staticCompositionLocalOf { false }

// CSS timing functions used by the prototype (docs/source/src/index.css).
val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
val CssEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
/** Toggle thumb: cubic-bezier(0.34, 1.56, 0.64, 1), a slight overshoot. */
val ToggleSpringEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** True when the resolved Breeze theme (Light / Dark / System) is dark. */
val LocalBreezeDark = staticCompositionLocalOf { false }

/** Prototype `tc(dark, a)`: the base text colour at [alpha]. */
@Composable
@ReadOnlyComposable
fun ink(alpha: Float = 1f): Color =
    (if (LocalBreezeDark.current) InkDark else InkLight).copy(alpha = alpha)

/** Neutral fill used by secondary buttons, inactive chips and steppers. */
@Composable
@ReadOnlyComposable
fun neutralFill(): Color =
    if (LocalBreezeDark.current) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)

/** Hairline used between rows inside glass cards. */
@Composable
@ReadOnlyComposable
fun hairline(): Color =
    if (LocalBreezeDark.current) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)

@Composable
@ReadOnlyComposable
fun surfaceColor(): Color = if (LocalBreezeDark.current) SurfaceDark else SurfaceLight

val AcMode.tint: Color
    get() = when (this) {
        AcMode.Cool -> CoolTint
        AcMode.Heat -> HeatTint
        AcMode.Dry -> DryTint
        AcMode.Fan -> FanTint
        AcMode.Auto -> AutoTint
    }

@Composable
fun AcMode.icon(): ImageVector = when (this) {
    AcMode.Cool -> Icons.Rounded.AcUnit
    AcMode.Heat -> Icons.Rounded.LocalFireDepartment
    AcMode.Dry -> Icons.Rounded.WaterDrop
    AcMode.Fan -> fanIcon()
    AcMode.Auto -> Icons.Rounded.AutoMode
}

/** Material Symbols `mode_fan`; not part of material-icons-extended. */
@Composable
fun fanIcon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_mode_fan)

private val DarkColors = darkColorScheme(
    primary = CoolTint,
    onPrimary = Color.White,
    secondary = CoolTint,
    background = SurfaceDark,
    onBackground = InkDark,
    surface = Color(0xFF1A1E25),
    onSurface = InkDark,
    surfaceContainerHigh = Color(0xFF1A1E25),
    surfaceContainer = Color(0xFF1A1E25),
    onSurfaceVariant = InkDark.copy(alpha = 0.55f),
    outline = Color.White.copy(alpha = 0.12f),
    error = Danger
)

private val LightColors = lightColorScheme(
    primary = CoolTint,
    onPrimary = Color.White,
    secondary = CoolTint,
    background = SurfaceLight,
    onBackground = InkLight,
    surface = Color.White,
    onSurface = InkLight,
    surfaceContainerHigh = Color.White,
    surfaceContainer = Color.White,
    onSurfaceVariant = InkLight.copy(alpha = 0.55f),
    outline = Color.Black.copy(alpha = 0.10f),
    error = Danger
)

@Composable
fun BreezeTheme(
    dark: Boolean,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalBreezeDark provides dark, LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}

package com.ijad.breeze.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.ui.theme.CoolTint
import com.ijad.breeze.ui.theme.Danger
import com.ijad.breeze.ui.theme.LocalBreezeDark
import com.ijad.breeze.ui.theme.SurfaceDark
import com.ijad.breeze.ui.theme.SurfaceLight
import com.ijad.breeze.ui.theme.ToggleSpringEasing
import com.ijad.breeze.ui.theme.ink
import com.ijad.breeze.ui.theme.neutralFill

// ─── Background ───────────────────────────────────────────────────────────────

/**
 * Prototype `getBg(rgb, dark)`: surface colour plus an elliptical radial glow of [tint]
 * at the top (and a faint bottom-right glow in dark). [intensity] 0 → plain surface
 * (used for the powered-off remote). Tint and intensity crossfade over 500 ms.
 */
@Composable
fun Modifier.breezeBackground(tint: Color, intensity: Float = 1f): Modifier {
    val dark = LocalBreezeDark.current
    val animTint by animateColorAsState(tint, tween(500), label = "bgTint")
    val animIntensity by animateFloatAsState(intensity, tween(500), label = "bgIntensity")
    // Theme switches re-tint every surface rather than snapping.
    val surface by animateColorAsState(if (dark) SurfaceDark else SurfaceLight, tween(300), label = "surface")
    return this.drawBehind {
        drawRect(surface)
        if (animIntensity <= 0f) return@drawBehind
        if (dark) {
            ellipseGlow(animTint.copy(alpha = 0.52f * animIntensity), Offset(size.width / 2f, 0f), 1.60f, 0.52f, 0.62f)
            ellipseGlow(animTint.copy(alpha = 0.14f * animIntensity), Offset(size.width * 0.88f, size.height * 0.98f), 0.90f, 0.40f, 0.55f)
        } else {
            ellipseGlow(animTint.copy(alpha = 0.17f * animIntensity), Offset(size.width / 2f, 0f), 1.30f, 0.46f, 0.62f)
        }
    }
}

/** CSS `radial-gradient(ellipse rx% ry% at center, color 0%, transparent stop%)`. */
private fun DrawScope.ellipseGlow(color: Color, center: Offset, rxFrac: Float, ryFrac: Float, stop: Float) {
    val rx = size.width * rxFrac
    val ry = size.height * ryFrac
    scale(scaleX = rx / ry, scaleY = 1f, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                stop to color.copy(alpha = 0f),
                center = center,
                radius = ry
            ),
            radius = ry,
            center = center
        )
    }
}

// ─── Glass card ───────────────────────────────────────────────────────────────

/** Prototype `gss(dark, accent)`: translucent fill + hairline border, radius 24. */
@Composable
fun Modifier.glass(accent: Color? = null, shape: Shape = RoundedCornerShape(24.dp)): Modifier {
    val dark = LocalBreezeDark.current
    val fill = when {
        accent != null -> accent.copy(alpha = if (dark) 0.10f else 0.08f)
        dark -> Color.White.copy(alpha = 0.07f)
        else -> Color.White.copy(alpha = 0.72f)
    }
    val border = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)
    return this
        .clip(shape)
        .background(fill, shape)
        .border(1.dp, border, shape)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    radius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.glass(accent, RoundedCornerShape(radius)), content = content)
}

/** 1px row separator inside a glass card. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    val dark = LocalBreezeDark.current
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f))
    )
}

// ─── Press feedback ───────────────────────────────────────────────────────────

/** Click with the prototype's scale-down press feedback and no ripple. */
@Composable
fun Modifier.pressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) pressedScale else 1f,
        tween(120),
        label = "press"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

// ─── Buttons ──────────────────────────────────────────────────────────────────

enum class ButtonVariant { Primary, Secondary, Ghost }

/** Prototype `RoundBtn`: 52dp pill, tinted glow on primary, 0.38 opacity when disabled. */
@Composable
fun RoundButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    tint: Color = CoolTint,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    fontSize: Int = 15,
    leading: ImageVector? = null
) {
    val dark = LocalBreezeDark.current
    val shape = CircleShape
    val bg = when (variant) {
        ButtonVariant.Primary -> tint
        ButtonVariant.Secondary -> if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
        ButtonVariant.Ghost -> Color.Transparent
    }
    val fg = when (variant) {
        ButtonVariant.Primary -> Color.White
        ButtonVariant.Secondary -> ink()
        ButtonVariant.Ghost -> tint
    }
    Row(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.38f)
            .then(
                if (variant == ButtonVariant.Primary && enabled) {
                    Modifier.shadow(14.dp, shape, ambientColor = tint, spotColor = tint)
                } else Modifier
            )
            .clip(shape)
            .background(bg)
            .then(
                if (variant == ButtonVariant.Secondary) {
                    Modifier.border(
                        1.dp,
                        if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f),
                        shape
                    )
                } else Modifier
            )
            .pressScale(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            color = fg,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Round 44dp icon button (app bars, steppers, power, arrows). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    background: Color = Color.Transparent,
    iconTint: Color = ink(0.75f),
    border: Color? = null,
    enabled: Boolean = true,
    pressedScale: Float = 0.88f
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
            .pressScale(enabled = enabled, pressedScale = pressedScale, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

// ─── App bar ──────────────────────────────────────────────────────────────────

/** Back arrow + title row used by Brand, Find code, Timer, Settings and Legal. */
@Composable
fun BreezeAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    centerTitle: Boolean = false,
    titleSize: Int = 20,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        Text(
            title,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = ink(),
            modifier = Modifier.weight(1f),
            textAlign = if (centerTitle) androidx.compose.ui.text.style.TextAlign.Center else null
        )
        trailing()
    }
}

// ─── Labels ───────────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.1.sp,
        color = ink(0.38f)
    )
}

// ─── Toggle ───────────────────────────────────────────────────────────────────

/** Prototype `Toggle`: 48×28 track, 22dp thumb with overshoot spring, tinted glow when on. */
@Composable
fun BreezeToggle(
    on: Boolean,
    onChange: (Boolean) -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val track by animateColorAsState(
        if (on) tint else Color(0xFF94A3B8).copy(alpha = 0.4f),
        tween(250),
        label = "track"
    )
    // .toggle-thumb: left 0.25s cubic-bezier(0.34,1.56,0.64,1) — the swing knob's spring.
    val thumbX by animateDpAsState(if (on) 23.dp else 3.dp, tween(250, easing = ToggleSpringEasing), label = "thumb")
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(48.dp, 28.dp)
            .then(if (on) Modifier.shadow(8.dp, shape, ambientColor = tint, spotColor = tint) else Modifier)
            .clip(shape)
            .background(track)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch
            ) { onChange(!on) }
    ) {
        Box(
            Modifier
                .offset(x = thumbX, y = 3.dp)
                .size(22.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

// ─── Fan steps ────────────────────────────────────────────────────────────────

/** Prototype `FanSteps`: four 8dp bars; bars above [value] fade progressively. */
@Composable
fun FanSteps(
    value: Int,
    onChange: (Int) -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dark = LocalBreezeDark.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..4).forEach { s ->
            val active = s <= value
            val color by animateColorAsState(
                if (active) tint else if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f),
                tween(200),
                label = "fanStep"
            )
            val stepAlpha = if (active) 1f else (0.35f + (value - s + 1) * 0.1f).coerceIn(0.05f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled
                    ) { onChange(s) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .alpha(stepAlpha)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

// ─── Chips ────────────────────────────────────────────────────────────────────

/** Mode / preset pill: tinted with glow when [active], neutral otherwise. */
@Composable
fun PillChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
) {
    val bg by animateColorAsState(if (active) tint else neutralFill(), tween(200), label = "chipBg")
    val fg by animateColorAsState(
        if (active) Color.White else ink(if (enabled) 0.75f else 0.35f),
        tween(200),
        label = "chipFg"
    )
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .then(if (active) Modifier.shadow(8.dp, CircleShape, ambientColor = tint, spotColor = tint) else Modifier)
            .clip(CircleShape)
            .background(bg)
            .pressScale(enabled = enabled, onClick = onClick)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

// ─── Banners ──────────────────────────────────────────────────────────────────

@Composable
fun NoIrBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass(accent = Danger, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("No IR blaster detected", style = MaterialTheme.typography.labelMedium, color = Danger)
            Text(
                "You can still browse and pair. Sending needs a phone with an IR emitter.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = ink(0.55f)
            )
        }
    }
}

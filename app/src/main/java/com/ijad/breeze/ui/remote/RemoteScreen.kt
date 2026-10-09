package com.ijad.breeze.ui.remote

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.AcTimer
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.data.RemoteState
import com.ijad.breeze.data.TimerAction
import com.ijad.breeze.ir.AcCommands
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.components.CircleIconButton
import com.ijad.breeze.ui.components.FanSteps
import com.ijad.breeze.ui.components.NoIrBanner
import com.ijad.breeze.ui.components.PillChip
import com.ijad.breeze.ui.components.RoundButton
import com.ijad.breeze.ui.components.BreezeToggle
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.components.glass
import com.ijad.breeze.ui.components.pressScale
import com.ijad.breeze.ui.theme.AutoTint
import com.ijad.breeze.ui.theme.CoolTint
import com.ijad.breeze.ui.theme.CssEase
import com.ijad.breeze.ui.theme.CssEaseOut
import com.ijad.breeze.ui.theme.LocalReduceMotion
import com.ijad.breeze.ui.theme.Danger
import com.ijad.breeze.ui.theme.LocalBreezeDark
import com.ijad.breeze.ui.theme.SurfaceDark
import com.ijad.breeze.ui.theme.fanIcon
import com.ijad.breeze.ui.theme.icon
import com.ijad.breeze.ui.theme.ink
import com.ijad.breeze.ui.theme.tint
import kotlinx.coroutines.delay
import java.util.Date

private enum class Preset(val label: String) { Sleep("Sleep"), Eco("Eco"), Turbo("Turbo") }

private data class Hint(val text: String, val warning: Boolean)

/** Remote — docs/design/04, 06–11 (modes, power off, dark). */
@Composable
fun RemoteScreen(
    devices: List<AcDevice>,
    activeDevice: AcDevice?,
    timer: AcTimer?,
    tempAlerts: Boolean,
    ecoTips: Boolean,
    irTransmitter: IrTransmitter,
    onSelectDevice: (String) -> Unit,
    onStateChange: (deviceId: String, RemoteState) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTimer: () -> Unit,
    onCancelTimer: (deviceId: String) -> Unit,
    onAddAc: () -> Unit
) {
    if (activeDevice == null) {
        EmptyRemote(onOpenSettings = onOpenSettings, onAddAc = onAddAc)
        return
    }

    val context = LocalContext.current
    var state by remember(activeDevice.id) { mutableStateOf(activeDevice.state) }
    // Saveable so the highlight survives a trip to Timer/Settings and back.
    var preset by rememberSaveable(activeDevice.id) { mutableStateOf<Preset?>(null) }
    var hint by remember { mutableStateOf<Hint?>(null) }
    var warnedUnsupported by remember(activeDevice.id) { mutableStateOf(false) }

    // A timer may have changed power while we were in the background…
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state = activeDevice.state }
    // …or while this screen is showing. Our own toggles save the same value, so this is a no-op for them.
    LaunchedEffect(activeDevice.state.poweredOn) {
        if (state.poweredOn != activeDevice.state.poweredOn) {
            state = state.copy(poweredOn = activeDevice.state.poweredOn)
        }
    }

    LaunchedEffect(hint) {
        if (hint != null) {
            delay(4500)
            hint = null
        }
    }

    fun report(result: AcCommands.Result) {
        when (result) {
            AcCommands.Result.Sent, AcCommands.Result.NoEmitter -> Unit // banner already explains
            is AcCommands.Result.Unsupported -> if (!warnedUnsupported) {
                warnedUnsupported = true
                Toast.makeText(context, AcCommands.messageFor(result), Toast.LENGTH_SHORT).show()
            }
            is AcCommands.Result.Failed -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun apply(new: RemoteState, clearPreset: Boolean = true) {
        if (clearPreset) preset = null
        state = new
        onStateChange(activeDevice.id, new)
        report(AcCommands.sendState(irTransmitter, activeDevice, new))
    }

    fun checkTemp(new: RemoteState) {
        val t = new.temperatureC
        hint = when {
            tempAlerts && t <= 17 -> Hint("$t° is very cold. Make sure that's what you want.", warning = true)
            tempAlerts && t >= 29 -> Hint("$t° is very warm. Make sure that's what you want.", warning = true)
            ecoTips && new.mode == AcMode.Cool && t < 22 ->
                Hint("Eco tip: 24–26° keeps you cool. Each degree warmer uses about 6% less energy.", warning = false)
            else -> hint
        }
    }

    fun applyPreset(p: Preset) {
        val new = when (p) {
            Preset.Sleep -> state.copy(temperatureC = (state.temperatureC + 1).coerceAtMost(30), fan = FanSpeed.Low)
            Preset.Eco -> state.copy(mode = AcMode.Cool, temperatureC = 26, fan = FanSpeed.Auto)
            Preset.Turbo -> state.copy(mode = AcMode.Cool, temperatureC = 18, fan = FanSpeed.High)
        }
        apply(new, clearPreset = false)
        preset = p
        checkTemp(new)
    }

    val powered = state.poweredOn
    val tint by animateColorAsState(if (powered) state.mode.tint else AutoTint, tween(400), label = "tint")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .breezeBackground(if (powered) state.mode.tint else AutoTint, intensity = if (powered) 1f else 0f)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            TopBar(
                device = activeDevice,
                devices = devices,
                powered = powered,
                tint = tint,
                onSelectDevice = onSelectDevice,
                onAddAc = onAddAc,
                onOpenSettings = onOpenSettings,
                onPower = { apply(state.copy(poweredOn = !powered), clearPreset = true) }
            )

            if (!irTransmitter.hasIrEmitter) {
                NoIrBanner(Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }

            Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                TempDial(
                    temp = state.temperatureC,
                    tint = tint,
                    powered = powered,
                    onDec = {
                        val new = state.copy(temperatureC = (state.temperatureC - 1).coerceAtLeast(16))
                        if (new != state) { apply(new); checkTemp(new) }
                    },
                    onInc = {
                        val new = state.copy(temperatureC = (state.temperatureC + 1).coerceAtMost(30))
                        if (new != state) { apply(new); checkTemp(new) }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AcMode.entries.forEach { m ->
                    PillChip(
                        label = m.label,
                        icon = m.icon(),
                        active = powered && state.mode == m,
                        tint = m.tint,
                        enabled = powered,
                        onClick = {
                            if (state.mode != m) {
                                val new = state.copy(mode = m)
                                apply(new)
                                checkTemp(new)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FanCard(
                    fan = state.fan,
                    tint = tint,
                    powered = powered,
                    onFan = { f -> if (f != state.fan) apply(state.copy(fan = f)) },
                    modifier = Modifier.weight(1f)
                )
                SwingCard(
                    swingOn = state.swingOn,
                    tint = tint,
                    powered = powered,
                    onToggle = { on ->
                        val new = state.copy(swingOn = on)
                        state = new
                        onStateChange(activeDevice.id, new)
                        report(AcCommands.sendSwing(irTransmitter, activeDevice, new))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = hint != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                val h = hint
                if (h != null) HintPill(h, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
            }

            Spacer(Modifier.height(16.dp))
        }

        PresetsSheet(
            powered = powered,
            tint = tint,
            activePreset = preset,
            timer = timer,
            onPreset = { applyPreset(it) },
            onTimer = onOpenTimer,
            onCancelTimer = { onCancelTimer(activeDevice.id) }
        )
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    device: AcDevice,
    devices: List<AcDevice>,
    powered: Boolean,
    tint: Color,
    onSelectDevice: (String) -> Unit,
    onAddAc: () -> Unit,
    onOpenSettings: () -> Unit,
    onPower: () -> Unit
) {
    val dark = LocalBreezeDark.current
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { menu = true }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Switch AC", tint = ink(0.55f), modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = if (dark) Color(0xFF1A1E25) else Color.White
            ) {
                devices.forEach { d ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(d.name, style = MaterialTheme.typography.titleSmall, color = if (d.id == device.id) CoolTint else ink(0.9f))
                                Text("${d.brandName} · Code ${d.configIndex + 1}", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, color = ink(0.45f))
                            }
                        },
                        onClick = { menu = false; onSelectDevice(d.id) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Add AC", style = MaterialTheme.typography.titleSmall, color = CoolTint) },
                    leadingIcon = { Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = CoolTint) },
                    onClick = { menu = false; onAddAc() }
                )
            }
        }
        CircleIconButton(Icons.Rounded.Settings, contentDescription = "Open settings", onClick = onOpenSettings, iconTint = ink(0.55f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(if (powered) Modifier.shadow(12.dp, CircleShape, ambientColor = tint, spotColor = tint) else Modifier)
                .background(
                    if (powered) tint.copy(alpha = 0.16f)
                    else if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f),
                    CircleShape
                )
                .pressScale(onClick = onPower),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = if (powered) "Turn off" else "Turn on",
                tint = if (powered) tint else ink(0.45f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─── Dial ─────────────────────────────────────────────────────────────────────

private const val ARC_START = 150f
private const val ARC_SWEEP = 240f

/** 224dp dial: r=96 track (stroke 7) sweeping 240° from 150°, glowing tinted progress. */
@Composable
private fun TempDial(temp: Int, tint: Color, powered: Boolean, onDec: () -> Unit, onInc: () -> Unit) {
    val dark = LocalBreezeDark.current
    // stroke-dasharray 0.35s ease
    val progress by animateFloatAsState(((temp - 16) / 14f).coerceIn(0f, 1f), tween(350, easing = CssEase), label = "dial")
    val arcAlpha by animateFloatAsState(if (powered) 1f else 0f, tween(300), label = "arcAlpha")
    val trackColor = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val numberColor by animateColorAsState(ink(if (powered) 1f else 0.25f), tween(300), label = "num")

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(Modifier.size(224.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(224.dp)) {
                val r = 96.dp.toPx()
                val stroke = 7.dp.toPx()
                drawDialArc(trackColor, r, stroke, ARC_SWEEP)
                if (arcAlpha > 0f) {
                    val sweep = (ARC_SWEEP * progress).coerceAtLeast(1f)
                    // Soft glow (CSS drop-shadow 8px) layered under the main arc.
                    drawDialArc(tint.copy(alpha = 0.10f * arcAlpha), r, stroke + 12.dp.toPx(), sweep)
                    drawDialArc(tint.copy(alpha = 0.18f * arcAlpha), r, stroke + 6.dp.toPx(), sweep)
                    drawDialArc(tint.copy(alpha = arcAlpha), r, stroke, sweep)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$temp°", style = MaterialTheme.typography.displayMedium, color = numberColor)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Celsius",
                    style = MaterialTheme.typography.bodySmall,
                    letterSpacing = 0.8.sp,
                    color = ink(if (powered) 0.42f else 0.18f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val bg = if (dark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f)
            val border = if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.09f)
            listOf(Icons.Rounded.Remove to onDec, Icons.Rounded.Add to onInc).forEach { (icon, action) ->
                CircleIconButton(
                    icon,
                    contentDescription = if (icon == Icons.Rounded.Remove) "Cooler" else "Warmer",
                    onClick = action,
                    size = 48.dp,
                    background = bg,
                    border = border,
                    iconTint = ink(if (powered) 0.85f else 0.28f),
                    enabled = powered,
                    modifier = Modifier.alpha(if (powered) 1f else 0.38f)
                )
            }
        }
    }
}

private fun DrawScope.drawDialArc(color: Color, radius: Float, stroke: Float, sweep: Float) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawArc(
        color = color,
        startAngle = ARC_START,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(c.x - radius, c.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}

// ─── Cards ────────────────────────────────────────────────────────────────────

@Composable
private fun CardHeader(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 12.sp, letterSpacing = 0.7.sp, color = ink(0.5f))
    }
}

@Composable
private fun FanCard(fan: FanSpeed, tint: Color, powered: Boolean, onFan: (FanSpeed) -> Unit, modifier: Modifier) {
    val cardAlpha by animateFloatAsState(if (powered) 1f else 0.38f, tween(300), label = "fanCard")
    Column(
        modifier
            .alpha(cardAlpha)
            .glass(accent = tint)
            .padding(16.dp)
    ) {
        CardHeader(fanIcon(), "Fan", tint)
        Spacer(Modifier.height(14.dp))
        FanSteps(
            value = fan.ordinal + 1,
            onChange = { onFan(FanSpeed.entries[it - 1]) },
            tint = tint,
            enabled = powered
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FanSpeed.entries.forEach { f ->
                Text(
                    f.label,
                    fontSize = 10.sp,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (f == fan && powered) tint else ink(0.38f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SwingCard(swingOn: Boolean, tint: Color, powered: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier) {
    val cardAlpha by animateFloatAsState(if (powered) 1f else 0.38f, tween(300), label = "swingCard")
    Column(
        modifier
            .alpha(cardAlpha)
            .glass(accent = tint)
            .padding(16.dp)
    ) {
        CardHeader(Icons.Rounded.Air, "Swing", tint)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            BreezeToggle(on = swingOn, onChange = onToggle, tint = tint, enabled = powered)
            Icon(
                if (swingOn) Icons.Rounded.SwapVert else Icons.Rounded.Stop,
                contentDescription = null,
                tint = if (swingOn) tint else ink(0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HintPill(hint: Hint, modifier: Modifier = Modifier) {
    val color = if (hint.warning) Danger else Color(0xFF2FB384)
    Row(
        modifier
            .fillMaxWidth()
            .glass(accent = color, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(if (hint.warning) Icons.Rounded.Thermostat else Icons.Rounded.Eco, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(hint.text, style = MaterialTheme.typography.bodySmall, color = ink(0.8f))
    }
}

// ─── Presets sheet ────────────────────────────────────────────────────────────

@Composable
private fun PresetsSheet(
    powered: Boolean,
    tint: Color,
    activePreset: Preset?,
    timer: AcTimer?,
    onPreset: (Preset) -> Unit,
    onTimer: () -> Unit,
    onCancelTimer: () -> Unit
) {
    val dark = LocalBreezeDark.current
    val reduceMotion = LocalReduceMotion.current
    val topBorder = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    // `slide-up` 0.35s ease-out: translateY(12px) → 0, opacity 0 → 1, once on entry.
    val entry = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { entry.animateTo(1f, tween(350, easing = CssEaseOut)) }
    Column(
        modifier = Modifier
            .graphicsLayer {
                translationY = (1f - entry.value) * 12.dp.toPx()
                alpha = entry.value
            }
            .fillMaxWidth()
            .background(if (dark) SurfaceDark.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f), shape)
            // CSS border-top only: stroke the rounded outline, clipped to the top edge.
            .drawBehind {
                val r = 24.dp.toPx()
                clipRect(bottom = r) {
                    drawRoundRect(topBorder, cornerRadius = CornerRadius(r, r), style = Stroke(1.dp.toPx()))
                }
            }
            .navigationBarsPadding()
            .padding(top = 14.dp, bottom = 28.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(36.dp, 4.dp)
                .background(ink(0.18f), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(14.dp))

        if (timer != null) {
            TimerRow(timer, onOpen = onTimer, onCancel = onCancelTimer, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp))
        }

        Text(
            "PRESETS",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.1.sp,
            color = ink(0.4f),
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pad = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
            listOf(
                Preset.Sleep to Icons.Rounded.Bedtime,
                Preset.Eco to Icons.Rounded.Eco,
                Preset.Turbo to Icons.Rounded.Bolt
            ).forEach { (p, icon) ->
                PillChip(
                    label = p.label,
                    icon = icon,
                    active = powered && activePreset == p,
                    tint = tint,
                    enabled = powered,
                    onClick = { onPreset(p) },
                    padding = pad
                )
            }
            PillChip(
                label = "Timer",
                icon = Icons.Rounded.Timer,
                active = timer != null,
                tint = CoolTint,
                onClick = onTimer,
                padding = pad
            )
        }
    }
}

@Composable
private fun TimerRow(timer: AcTimer, onOpen: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (true) {
            now = System.currentTimeMillis()
            delay(20_000)
        }
    }
    val remainingMin = ((timer.triggerAtMillis - now).coerceAtLeast(0) + 59_999) / 60_000
    val clock = DateFormat.getTimeFormat(context).format(Date(timer.triggerAtMillis))
    val verb = if (timer.action == TimerAction.TurnOff) "Turns off" else "Turns on"
    val remaining = if (remainingMin >= 60) "${remainingMin / 60}h ${remainingMin % 60}m" else "${remainingMin}m"
    Row(
        modifier
            .fillMaxWidth()
            .glass(accent = CoolTint, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Timer, contentDescription = null, tint = CoolTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "$verb at $clock · in $remaining",
            style = MaterialTheme.typography.labelMedium,
            color = ink(0.8f),
            modifier = Modifier.weight(1f)
        )
        CircleIconButton(Icons.Rounded.Close, contentDescription = "Cancel timer", onClick = onCancel, size = 36.dp, iconSize = 18.dp, iconTint = ink(0.5f))
    }
}

// ─── Empty ────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyRemote(onOpenSettings: () -> Unit, onAddAc: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .breezeBackground(CoolTint)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Breeze", style = MaterialTheme.typography.titleMedium, color = ink(), modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Rounded.Settings, contentDescription = "Open settings", onClick = onOpenSettings, iconTint = ink(0.55f))
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .background(CoolTint.copy(alpha = 0.16f), CircleShape)
                    .border(1.5.dp, CoolTint.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(AcMode.Cool.icon(), contentDescription = null, tint = CoolTint, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("No AC paired yet", style = MaterialTheme.typography.headlineMedium, color = ink())
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick your brand and find a code that works to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = ink(0.55f),
                textAlign = TextAlign.Center
            )
        }
        RoundButton(
            "Add AC",
            onClick = onAddAc,
            leading = Icons.Rounded.Add,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        )
    }
}

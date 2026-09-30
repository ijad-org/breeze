package com.ijad.breeze.ui.remote

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.ir.BrandIr
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.components.BreezeWordmark
import com.ijad.breeze.ui.components.ElevatedCard
import com.ijad.breeze.ui.components.NoIrBanner
import com.ijad.breeze.ui.theme.BreezeBlue
import com.ijad.breeze.ui.theme.BreezeGlow

@Composable
fun RemoteScreen(
    devices: List<AcDevice>,
    activeDevice: AcDevice?,
    irTransmitter: IrTransmitter,
    onSelectDevice: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAddAc: () -> Unit
) {
    val context = LocalContext.current
    var poweredOn by remember { mutableStateOf(true) }
    var temperature by remember { mutableIntStateOf(24) }
    var mode by remember { mutableStateOf(AcMode.Cool) }
    var fan by remember { mutableStateOf(FanSpeed.Medium) }
    var swingOn by remember { mutableStateOf(true) }
    var deviceMenu by remember { mutableStateOf(false) }

    fun transmitControl() {
        val device = activeDevice ?: return
        if (!irTransmitter.hasIrEmitter) {
            Toast.makeText(context, "No IR blaster on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val pattern = BrandIr.controlPattern(device.brandId, poweredOn, mode, temperature, fan)
        if (pattern == null) {
            Toast.makeText(context, "Codes coming for ${device.brandName}", Toast.LENGTH_SHORT).show()
            return
        }
        val result = irTransmitter.transmit(pattern.frequencyHz, pattern.micros)
        if (!result.success) Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
    }

    fun transmitSwing() {
        val device = activeDevice ?: return
        if (!irTransmitter.hasIrEmitter) {
            Toast.makeText(context, "No IR blaster on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val pattern = BrandIr.swingPattern(device.brandId)
        if (pattern == null) {
            Toast.makeText(context, "Codes coming for ${device.brandName}", Toast.LENGTH_SHORT).show()
            return
        }
        val result = irTransmitter.transmit(pattern.frequencyHz, pattern.micros)
        if (!result.success) Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreezeWordmark(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
            }
        }

        if (!irTransmitter.hasIrEmitter) {
            NoIrBanner(Modifier.padding(vertical = 8.dp))
        }

        if (activeDevice == null) {
            EmptyRemote(onAddAc = onAddAc)
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { deviceMenu = true }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeDevice.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = deviceMenu, onDismissRequest = { deviceMenu = false }) {
                    devices.forEach { d ->
                        DropdownMenuItem(
                            text = { Text("${d.name} (${d.brandName})") },
                            onClick = {
                                deviceMenu = false
                                onSelectDevice(d.id)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Add AC…") },
                        onClick = {
                            deviceMenu = false
                            onAddAc()
                        }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // Glowing power
            Box(contentAlignment = Alignment.Center) {
                if (poweredOn) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(BreezeGlow.copy(alpha = 0.35f))
                    )
                }
                IconButton(
                    onClick = {
                        poweredOn = !poweredOn
                        transmitControl()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (poweredOn) BreezeBlue.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        Icons.Rounded.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = if (poweredOn) BreezeBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "${activeDevice.brandName} · Code ${activeDevice.configIndex + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Temperature dial
        TempDial(
            temperature = temperature,
            poweredOn = poweredOn,
            onMinus = {
                temperature = (temperature - 1).coerceAtLeast(16)
                if (poweredOn) transmitControl()
            },
            onPlus = {
                temperature = (temperature + 1).coerceAtMost(30)
                if (poweredOn) transmitControl()
            }
        )

        Spacer(Modifier.height(16.dp))

        // Mode chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AcMode.Cool to Icons.Rounded.AcUnit,
                AcMode.Heat to Icons.Rounded.WbSunny,
                AcMode.Dry to Icons.Rounded.WaterDrop,
                AcMode.Fan to Icons.Rounded.Air
            ).forEach { (m, icon) ->
                ModeChip(
                    label = m.label,
                    icon = icon,
                    selected = mode == m && poweredOn,
                    onClick = {
                        mode = m
                        if (poweredOn) transmitControl()
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FanCard(
                fan = fan,
                modifier = Modifier.weight(1f),
                onCycle = {
                    fan = when (fan) {
                        FanSpeed.Low -> FanSpeed.Medium
                        FanSpeed.Medium -> FanSpeed.High
                        FanSpeed.High -> FanSpeed.Auto
                        FanSpeed.Auto -> FanSpeed.Low
                    }
                    if (poweredOn) transmitControl()
                }
            )
            SwingCard(
                swingOn = swingOn,
                modifier = Modifier.weight(1f),
                onToggle = {
                    swingOn = !swingOn
                    transmitSwing()
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Presets drawer-like strip
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
                Text(
                    text = "PRESETS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PresetBtn(Icons.Rounded.DarkMode, "Sleep") {
                        Toast.makeText(context, "Sleep preset — IR stub", Toast.LENGTH_SHORT).show()
                    }
                    PresetBtn(Icons.Rounded.Eco, "Eco") {
                        Toast.makeText(context, "Eco preset — IR stub", Toast.LENGTH_SHORT).show()
                    }
                    PresetBtn(Icons.Rounded.Bolt, "Turbo") {
                        Toast.makeText(context, "Turbo preset — IR stub", Toast.LENGTH_SHORT).show()
                    }
                    PresetBtn(Icons.Rounded.Timer, "Timer") {
                        Toast.makeText(context, "Timer — coming soon", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EmptyRemote(onAddAc: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No AC paired yet", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add a brand and find a working code to start.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Add AC",
            color = BreezeBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAddAc)
                .padding(12.dp)
        )
    }
}

@Composable
private fun TempDial(
    temperature: Int,
    poweredOn: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val progress = ((temperature - 16) / 14f).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val stroke = 14.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = if (poweredOn) BreezeBlue else Color(0xFF94A3B8),
                    startAngle = 150f,
                    sweepAngle = 240f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$temperature",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (poweredOn) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Celsius",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            CircleAction("−", onMinus)
            CircleAction("+", onPlus)
        }
    }
}

@Composable
private fun CircleAction(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .shadow(if (selected) 6.dp else 2.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) BreezeBlue else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun FanCard(fan: FanSpeed, modifier: Modifier, onCycle: () -> Unit) {
    ElevatedCard(modifier = modifier, onClick = onCycle) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Air, contentDescription = null, tint = BreezeBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("FAN", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(12.dp))
            val levels = listOf(FanSpeed.Low, FanSpeed.Medium, FanSpeed.High)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                levels.forEachIndexed { i, level ->
                    val active = when (fan) {
                        FanSpeed.Auto -> true
                        else -> levels.indexOf(fan) >= i
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (active) BreezeBlue else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = fan.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwingCard(swingOn: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = BreezeBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SWING", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(8.dp))
            Switch(
                checked = swingOn,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BreezeBlue,
                    checkedThumbColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun PresetBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(icon, contentDescription = label, tint = BreezeBlue)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

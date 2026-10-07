package com.ijad.breeze.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.BuildConfig
import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.ThemeMode
import com.ijad.breeze.ui.components.BreezeAppBar
import com.ijad.breeze.ui.components.BreezeToggle
import com.ijad.breeze.ui.components.GlassCard
import com.ijad.breeze.ui.components.RowDivider
import com.ijad.breeze.ui.components.SectionLabel
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.components.glass
import com.ijad.breeze.ui.theme.AutoTint
import com.ijad.breeze.ui.theme.CoolTint
import com.ijad.breeze.ui.theme.CssEase
import com.ijad.breeze.ui.theme.Danger
import com.ijad.breeze.ui.theme.LocalBreezeDark
import com.ijad.breeze.ui.theme.ink
import com.ijad.breeze.ui.theme.surfaceColor

/** Settings — docs/design/05-settings-system.png, 05b/c/d */
@Composable
fun SettingsScreen(
    devices: List<AcDevice>,
    themeMode: ThemeMode,
    tempAlerts: Boolean,
    timerReminders: Boolean,
    ecoTips: Boolean,
    onBack: () -> Unit,
    onAddAc: () -> Unit,
    onRename: (deviceId: String, name: String) -> Unit,
    onDelete: (deviceId: String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onTempAlerts: (Boolean) -> Unit,
    onTimerReminders: (Boolean) -> Unit,
    onEcoTips: (Boolean) -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit
) {
    var renaming by remember { mutableStateOf<AcDevice?>(null) }
    var deleting by remember { mutableStateOf<AcDevice?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .breezeBackground(AutoTint)
    ) {
        BreezeAppBar(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 44.dp)
        ) {
            Section("My ACs") {
                devices.forEachIndexed { i, d ->
                    AcRow(d, onEdit = { renaming = d }, onDelete = { deleting = d })
                    if (i < devices.lastIndex) RowDivider()
                }
                if (devices.isNotEmpty()) RowDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddAc)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = CoolTint, modifier = Modifier.size(20.dp))
                    Text("Add AC", style = MaterialTheme.typography.labelLarge, fontSize = 14.sp, color = CoolTint)
                }
            }

            Section("Notifications") {
                SettingRow(Icons.Rounded.Thermostat, "Temperature alerts") {
                    BreezeToggle(tempAlerts, onTempAlerts, tint = CoolTint)
                }
                RowDivider()
                SettingRow(Icons.Rounded.Timer, "Timer reminders") {
                    BreezeToggle(timerReminders, onTimerReminders, tint = CoolTint)
                }
                RowDivider()
                SettingRow(Icons.Rounded.Eco, "Eco tips") {
                    BreezeToggle(ecoTips, onEcoTips, tint = CoolTint)
                }
            }

            Section("Appearance") {
                AppearanceSegment(themeMode, onThemeChange, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }

            Section("About") {
                SettingRow(Icons.Rounded.Info, "Version ${BuildConfig.VERSION_NAME}") {
                    Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = ink(0.4f))
                }
                RowDivider()
                SettingRow(Icons.Rounded.PrivacyTip, "Privacy Policy", onClick = onPrivacy) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ink(0.3f), modifier = Modifier.size(18.dp))
                }
                RowDivider()
                SettingRow(Icons.Rounded.Gavel, "Terms of Service", onClick = onTerms) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ink(0.3f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    renaming?.let { d ->
        RenameDialog(
            device = d,
            onDismiss = { renaming = null },
            onSave = { name -> onRename(d.id, name); renaming = null }
        )
    }
    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = surfaceColor(),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Remove ${d.name}?", style = MaterialTheme.typography.titleLarge, color = ink()) },
            text = {
                Text(
                    "You'll need to pair it again to use it. Any timer for this AC is cancelled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink(0.6f)
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(d.id); deleting = null }) {
                    Text("Remove", color = Danger, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("Cancel", color = ink(0.7f), style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 20.dp)) {
        SectionLabel(title)
        GlassCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ink(0.45f), modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ink(0.85f), modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun AcRow(device: AcDevice, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dark = LocalBreezeDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = CoolTint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.titleSmall, fontSize = 14.sp, color = ink(0.9f))
            Text(
                "${device.brandName} · Code ${device.configIndex + 1}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = ink(0.45f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        SmallIcon(Icons.Rounded.Edit, "Rename ${device.name}", ink(0.4f), onEdit)
        SmallIcon(Icons.Rounded.Delete, "Remove ${device.name}", Danger, onDelete)
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = false, radius = 18.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Light / Dark / System with a sliding selected pill. */
@Composable
private fun AppearanceSegment(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalBreezeDark.current
    val options = listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val segment = maxWidth / options.size
        // `transition: all 0.18s` (CSS ease) on the segment buttons.
        val x by animateDpAsState(segment * options.indexOf(selected), tween(180, easing = CssEase), label = "segment")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .then(if (!dark) Modifier.shadow(3.dp, RoundedCornerShape(10.dp)) else Modifier)
                .background(if (dark) Color.White.copy(alpha = 0.14f) else Color.White, RoundedCornerShape(10.dp))
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { opt ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(opt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opt.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = ink(if (opt == selected) 0.9f else 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(device: AcDevice, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable(device.id) { mutableStateOf(device.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor(),
        shape = RoundedCornerShape(24.dp),
        title = { Text("Rename AC", style = MaterialTheme.typography.titleLarge, color = ink()) },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .glass(shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink()),
                    cursorBrush = SolidColor(CoolTint),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Save", color = CoolTint, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ink(0.7f), style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

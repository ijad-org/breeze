package com.ijad.breeze.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.data.ThemeMode
import com.ijad.breeze.ui.components.BreezeFanMark
import com.ijad.breeze.ui.components.BreezeWordmark
import com.ijad.breeze.ui.components.ElevatedCard
import com.ijad.breeze.ui.components.SectionLabel
import com.ijad.breeze.ui.theme.BreezeBlue
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    devices: List<AcDevice>,
    repository: AppRepository,
    themeMode: ThemeMode,
    tempAlerts: Boolean,
    timerReminders: Boolean,
    ecoTips: Boolean,
    onBack: () -> Unit,
    onAddAc: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onTempAlerts: (Boolean) -> Unit,
    onTimerReminders: (Boolean) -> Unit,
    onEcoTips: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editDevice by remember { mutableStateOf<AcDevice?>(null) }
    var editName by remember { mutableStateOf("") }

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
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        SectionLabel("My ACs")
        devices.forEach { device ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        BreezeFanMark(size = 22.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            "${device.brandName} · Code ${device.configIndex + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        editDevice = device
                        editName = device.name
                    }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = BreezeBlue)
                    }
                    IconButton(onClick = {
                        scope.launch { repository.removeDevice(device.id) }
                        Toast.makeText(context, "Removed ${device.name}", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAddAc)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = BreezeBlue)
            Spacer(Modifier.width(6.dp))
            Text("Add AC", color = BreezeBlue, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Notifications")
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                NotifRow(
                    icon = Icons.Rounded.Thermostat,
                    title = "Temperature alerts",
                    checked = tempAlerts,
                    onChecked = onTempAlerts
                )
                NotifRow(
                    icon = Icons.Rounded.Timer,
                    title = "Timer reminders",
                    checked = timerReminders,
                    onChecked = onTimerReminders
                )
                NotifRow(
                    icon = Icons.Rounded.Eco,
                    title = "Eco tips",
                    checked = ecoTips,
                    onChecked = onEcoTips,
                    last = true
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Appearance")
        ThemeSegmented(
            selected = themeMode,
            onSelect = onThemeChange
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel("About")
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = BreezeBlue)
                Spacer(Modifier.width(12.dp))
                Text("Version 1.0.0", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    "v1.0.0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    editDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { editDevice = null },
            title = { Text("Rename AC") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.updateDeviceName(device.id, editName.ifBlank { device.name })
                    }
                    editDevice = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editDevice = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NotifRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    last: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BreezeBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = BreezeBlue,
                checkedThumbColor = Color.White
            )
        )
    }
    if (!last) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 48.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun ThemeSegmented(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    mode.label,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

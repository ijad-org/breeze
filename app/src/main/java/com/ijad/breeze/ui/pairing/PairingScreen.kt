package com.ijad.breeze.ui.pairing

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ijad.breeze.data.AcBrand
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.ir.BrandIr
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.components.NoIrBanner
import com.ijad.breeze.ui.components.PrimaryPillButton
import com.ijad.breeze.ui.components.SecondaryPillButton
import com.ijad.breeze.ui.theme.BreezeBlue
import com.ijad.breeze.ui.theme.BreezeGlow
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(
    brand: AcBrand,
    repository: AppRepository,
    irTransmitter: IrTransmitter,
    onBack: () -> Unit,
    onPaired: (deviceId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val totalCodes = BrandIr.probeCount(brand.id, brand.codeCount)
    var index by remember { mutableIntStateOf(0) }

    fun sendProbe() {
        if (!irTransmitter.hasIrEmitter) {
            Toast.makeText(context, "No IR blaster on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val pattern = BrandIr.powerProbe(brand.id, index)
        if (pattern == null) {
            Toast.makeText(context, "Codes coming for ${brand.name}", Toast.LENGTH_SHORT).show()
            return
        }
        val result = irTransmitter.transmit(pattern.frequencyHz, pattern.micros)
        if (!result.success) {
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun advance() {
        if (index < totalCodes - 1) index++
        else Toast.makeText(context, "End of codes — try another brand or retry", Toast.LENGTH_SHORT).show()
    }

    fun saveAndOpen(name: String = "Living Room") {
        scope.launch {
            val device = repository.addDevice(name = name, brand = brand, configIndex = index)
            onPaired(device.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Find your code",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { saveAndOpen() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Save", tint = BreezeBlue)
            }
        }

        Text(
            text = "Point your phone at the AC and tap the power button. Advance until it responds.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        if (!irTransmitter.hasIrEmitter) {
            NoIrBanner(Modifier.padding(bottom = 16.dp))
        }

        Spacer(Modifier.weight(0.4f))

        // Large glowing power + arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (index > 0) {
                CircleNav(
                    onClick = { index-- },
                    content = {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                )
            } else {
                Spacer(Modifier.size(48.dp))
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(BreezeGlow.copy(alpha = 0.2f))
                )
                Box(
                    Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(BreezeBlue.copy(alpha = 0.15f))
                )
                IconButton(
                    onClick = { sendProbe() },
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(BreezeBlue)
                ) {
                    Icon(
                        Icons.Rounded.PowerSettingsNew,
                        contentDescription = "Power probe",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            CircleNav(
                onClick = { advance() },
                content = {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next")
                }
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Code ${index + 1} of $totalCodes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = brand.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(0.6f))

        Text(
            text = "Did your AC respond?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        PrimaryPillButton(
            text = "Yes, it worked",
            onClick = { saveAndOpen() }
        )
        Spacer(Modifier.height(10.dp))
        SecondaryPillButton(
            text = "Not yet",
            onClick = { advance() },
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun CircleNav(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

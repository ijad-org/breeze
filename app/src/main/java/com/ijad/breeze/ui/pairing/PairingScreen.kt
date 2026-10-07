package com.ijad.breeze.ui.pairing

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Countertops
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Desk
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.data.AcBrand
import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.ir.BrandIr
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.components.BreezeAppBar
import com.ijad.breeze.ui.components.ButtonVariant
import com.ijad.breeze.ui.components.CircleIconButton
import com.ijad.breeze.ui.components.NoIrBanner
import com.ijad.breeze.ui.components.PillChip
import com.ijad.breeze.ui.components.RoundButton
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.components.glass
import com.ijad.breeze.ui.components.pressScale
import com.ijad.breeze.ui.theme.CoolTint
import com.ijad.breeze.ui.theme.CssEaseOut
import com.ijad.breeze.ui.theme.LocalReduceMotion
import com.ijad.breeze.ui.theme.LocalBreezeDark
import com.ijad.breeze.ui.theme.ink
import com.ijad.breeze.ui.theme.surfaceColor
import kotlinx.coroutines.launch

private val RoomSuggestions = listOf("Living Room", "Bedroom", "Office", "Kitchen")

/** Find your code — docs/design/03-find-code.png */
@Composable
fun PairingScreen(
    brand: AcBrand,
    existingDevices: List<AcDevice>,
    repository: AppRepository,
    irTransmitter: IrTransmitter,
    onBack: () -> Unit,
    onPaired: (deviceId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dark = LocalBreezeDark.current
    val tint = CoolTint
    val totalCodes = BrandIr.probeCount(brand.id, brand.codeCount)
    var index by rememberSaveable { mutableStateOf(0) }
    var naming by rememberSaveable { mutableStateOf(false) }

    fun sendProbe() {
        if (!irTransmitter.hasIrEmitter) {
            Toast.makeText(context, "No IR blaster on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val pattern = BrandIr.powerProbe(brand.id, index) ?: return
        val result = irTransmitter.transmit(pattern.frequencyHz, pattern.micros)
        if (!result.success) Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
    }

    fun advance() {
        if (index < totalCodes - 1) index++
        else Toast.makeText(context, "That was the last code. Try another brand or start over.", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .breezeBackground(tint)
            .navigationBarsPadding()
    ) {
        BreezeAppBar(
            title = "Find your code",
            onBack = onBack,
            centerTitle = true,
            titleSize = 18,
            trailing = {
                CircleIconButton(
                    Icons.Rounded.Check,
                    contentDescription = "Save this code",
                    onClick = { naming = true },
                    background = tint.copy(alpha = 0.14f),
                    iconTint = tint
                )
            }
        )

        Text(
            "Point your phone at the AC and tap the power button. Advance until it responds.",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = ink(0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 16.dp)
        )

        if (!irTransmitter.hasIrEmitter) {
            NoIrBanner(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                val arrowBg = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.6f)
                val arrowBorder = if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
                val leftAlpha by animateFloatAsState(if (index > 0) 1f else 0f, tween(250), label = "left")
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Previous code",
                    onClick = { if (index > 0) index-- },
                    enabled = index > 0,
                    background = arrowBg,
                    border = arrowBorder,
                    iconTint = ink(0.8f),
                    iconSize = 18.dp,
                    modifier = Modifier.alpha(leftAlpha)
                )
                PulsingPowerButton(tint = tint, onClick = { sendProbe() })
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "Next code",
                    onClick = { advance() },
                    background = arrowBg,
                    border = arrowBorder,
                    iconTint = ink(0.8f),
                    iconSize = 18.dp
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Code ${index + 1} of $totalCodes",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                letterSpacing = 0.7.sp,
                color = ink(0.45f)
            )
        }

        Text(
            "Did your AC respond?",
            style = MaterialTheme.typography.titleSmall,
            color = ink(0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundButton("Yes, it worked", onClick = { naming = true }, tint = tint, fontSize = 14, modifier = Modifier.weight(1f))
            RoundButton("Not yet", onClick = { advance() }, variant = ButtonVariant.Secondary, fontSize = 14, modifier = Modifier.weight(1f))
        }
    }

    if (naming) {
        NameAcSheet(
            initialName = RoomSuggestions.firstOrNull { s -> existingDevices.none { it.name == s } } ?: "${brand.name} AC",
            onDismiss = { naming = false },
            onSave = { name ->
                scope.launch {
                    val device = repository.addDevice(name = name.trim(), brand = brand, configIndex = index)
                    naming = false
                    onPaired(device.id)
                }
            }
        )
    }
}

/**
 * 88dp power button with the `btn-pulse` ring (2.2s ease-out infinite): box-shadow keyframes
 * 0%/100% = 0 spread at rgba(tint,.4), 60% = 20dp spread transparent, eased per segment.
 */
@Composable
private fun PulsingPowerButton(tint: Color, onClick: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val elapsed by produceState(0L, reduceMotion) {
        if (reduceMotion) return@produceState
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = it - start }
    }
    // Layout box is the 88dp button; ring and pulse draw outside it like a CSS box-shadow.
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val ringR = 54.dp.toPx()
            if (!reduceMotion) {
                val p = (elapsed % 2200L) / 2200f
                // k: 0 at the 0%/100% keyframes, 1 at the 60% keyframe.
                val k = if (p < 0.6f) CssEaseOut.transform(p / 0.6f)
                else 1f - CssEaseOut.transform((p - 0.6f) / 0.4f)
                val spread = 20.dp.toPx() * k
                val alpha = 0.4f * (1f - k)
                if (spread > 0.5f && alpha > 0f) {
                    drawCircle(tint.copy(alpha = alpha), radius = ringR + spread / 2f, style = Stroke(spread))
                }
            }
            drawCircle(tint.copy(alpha = 0.35f), radius = ringR, style = Stroke(2.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(88.dp)
                .shadow(20.dp, CircleShape, ambientColor = tint, spotColor = tint)
                .background(tint, CircleShape)
                .pressScale(pressedScale = 0.92f, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Send power code", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NameAcSheet(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(initialName) }
    val tint = CoolTint
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding()
        ) {
            Text("Name this AC", style = MaterialTheme.typography.titleLarge, color = ink())
            Spacer(Modifier.height(6.dp))
            Text("Pick a room so you can tell your ACs apart.", style = MaterialTheme.typography.bodySmall, color = ink(0.5f))
            Spacer(Modifier.height(16.dp))
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
                    cursorBrush = SolidColor(tint),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSave(name) }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomSuggestions.forEach { room ->
                    PillChip(
                        label = room,
                        icon = when (room) {
                            "Bedroom" -> Icons.Rounded.Bed
                            "Office" -> Icons.Rounded.Desk
                            "Kitchen" -> Icons.Rounded.Countertops
                            else -> Icons.Rounded.Chair
                        },
                        active = name == room,
                        tint = tint,
                        onClick = { name = room }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            RoundButton(
                "Save & open remote",
                onClick = { onSave(name) },
                enabled = name.isNotBlank(),
                tint = tint,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

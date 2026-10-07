package com.ijad.breeze.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.AcTimer
import com.ijad.breeze.data.TimerAction
import com.ijad.breeze.timer.TimerScheduler
import com.ijad.breeze.ui.components.BreezeAppBar
import com.ijad.breeze.ui.components.ButtonVariant
import com.ijad.breeze.ui.components.CircleIconButton
import com.ijad.breeze.ui.components.RoundButton
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.components.glass
import com.ijad.breeze.ui.components.pressScale
import com.ijad.breeze.ui.theme.CoolTint
import com.ijad.breeze.ui.theme.ink
import com.ijad.breeze.ui.theme.neutralFill
import java.util.Date

/** Timer — docs/design/12-timer-turn-off-in.png, 12b-timer-turn-on-at.png */
@Composable
fun TimerScreen(
    device: AcDevice?,
    existing: AcTimer?,
    timerReminders: Boolean,
    scheduler: TimerScheduler,
    onBack: () -> Unit,
    onSet: (AcTimer) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val tint = CoolTint
    var tab by rememberSaveable { mutableStateOf(existing?.action ?: TimerAction.TurnOff) }
    var offHours by rememberSaveable { mutableIntStateOf(2) }
    var offMinutes by rememberSaveable { mutableIntStateOf(30) }
    var onHours by rememberSaveable { mutableIntStateOf(7) }
    var onMinutes by rememberSaveable { mutableIntStateOf(0) }

    val hours = if (tab == TimerAction.TurnOff) offHours else onHours
    val minutes = if (tab == TimerAction.TurnOff) offMinutes else onMinutes
    fun setHours(v: Int) = if (tab == TimerAction.TurnOff) offHours = v else onHours = v
    fun setMinutes(v: Int) = if (tab == TimerAction.TurnOff) offMinutes = v else onMinutes = v

    fun computeTrigger() = if (tab == TimerAction.TurnOff) TimerScheduler.afterDuration(hours, minutes)
    else TimerScheduler.nextTimeOfDay(hours, minutes)
    val triggerAt = computeTrigger()
    val valid = device != null && (tab == TimerAction.TurnOn || hours * 60 + minutes > 0)

    fun commit() {
        val d = device ?: return
        // Recompute so time spent on this screen doesn't shorten a "Turn off in" timer.
        onSet(AcTimer(d.id, tab, computeTrigger()))
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { commit() }

    fun setTimer() {
        val needsPermission = timerReminders &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else commit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .breezeBackground(tint)
            .navigationBarsPadding()
    ) {
        BreezeAppBar(title = "Timer", onBack = onBack)

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerAction.entries.forEach { t ->
                TabButton(
                    label = if (t == TimerAction.TurnOff) "Turn off in" else "Turn on at",
                    active = tab == t,
                    tint = tint,
                    onClick = { tab = t },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (existing != null) {
            ExistingTimer(existing, onCancel, Modifier.padding(horizontal = 20.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stepper(
                    value = hours,
                    label = "Hours",
                    onUp = { setHours(wrap(hours + 1, 24)) },
                    onDown = { setHours(wrap(hours - 1, 24)) }
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 56.sp,
                    color = ink(0.25f),
                    modifier = Modifier.padding(bottom = 30.dp)
                )
                Stepper(
                    value = minutes,
                    label = "Minutes",
                    onUp = { setMinutes(wrap(minutes + 5, 60)) },
                    onDown = { setMinutes(wrap(minutes - 5, 60)) }
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                summary(context, tab, triggerAt, device?.name, valid),
                style = MaterialTheme.typography.bodySmall,
                color = ink(0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp)) {
            Text(
                "Keep your phone pointed at the AC when the timer goes off.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = ink(0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (!scheduler.canScheduleExact()) {
                val intent = scheduler.exactAlarmSettingsIntent()
                if (intent != null) {
                    RoundButton(
                        "Allow exact timing",
                        onClick = { runCatching { context.startActivity(intent) } },
                        variant = ButtonVariant.Ghost,
                        height = 36.dp,
                        fontSize = 13,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            RoundButton(
                if (existing != null) "Update timer" else "Set timer",
                onClick = { setTimer() },
                enabled = valid,
                tint = tint,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun wrap(v: Int, max: Int) = ((v % max) + max) % max

private fun summary(context: android.content.Context, tab: TimerAction, at: Long, name: String?, valid: Boolean): String {
    if (name == null) return "Pair an AC first."
    if (!valid) return "Pick a duration."
    val clock = DateFormat.getTimeFormat(context).format(Date(at))
    return when (tab) {
        TimerAction.TurnOff -> "$name turns off at $clock"
        TimerAction.TurnOn -> {
            val tomorrow = android.text.format.DateUtils.isToday(at).not()
            "$name turns on at $clock${if (tomorrow) " tomorrow" else ""}"
        }
    }
}

@Composable
private fun TabButton(label: String, active: Boolean, tint: Color, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val bg by animateColorAsState(if (active) tint else neutralFill(), tween(200), label = "tab")
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontSize = 14.sp,
        color = if (active) Color.White else ink(0.6f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .then(if (active) Modifier.shadow(10.dp, shape, ambientColor = tint, spotColor = tint) else Modifier)
            .background(bg, shape)
            .pressScale(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun Stepper(value: Int, label: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircleIconButton(Icons.Rounded.ExpandLess, "$label up", onUp, background = neutralFill(), iconTint = ink(0.7f))
        Text(
            value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayLarge,
            color = ink(),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 88.dp)
        )
        CircleIconButton(Icons.Rounded.ExpandMore, "$label down", onDown, background = neutralFill(), iconTint = ink(0.7f))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.1.sp, color = ink(0.38f))
    }
}

@Composable
private fun ExistingTimer(timer: AcTimer, onCancel: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val clock = DateFormat.getTimeFormat(context).format(Date(timer.triggerAtMillis))
    Row(
        modifier
            .fillMaxWidth()
            .glass(accent = CoolTint, shape = RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Timer, contentDescription = null, tint = CoolTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "${if (timer.action == TimerAction.TurnOff) "Turns off" else "Turns on"} at $clock",
            style = MaterialTheme.typography.labelMedium,
            color = ink(0.8f),
            modifier = Modifier.weight(1f)
        )
        Text(
            "Cancel",
            style = MaterialTheme.typography.labelMedium,
            color = CoolTint,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

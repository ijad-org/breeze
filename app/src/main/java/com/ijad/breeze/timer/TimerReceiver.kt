package com.ijad.breeze.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ijad.breeze.MainActivity
import com.ijad.breeze.R
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.data.TimerAction
import com.ijad.breeze.ir.AcCommands
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.theme.CoolTint
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires a scheduled timer: sends the IR power command for the device (the phone must be
 * pointed at the AC), saves the new power state and, if enabled, posts a reminder.
 */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                fire(context.applicationContext, deviceId)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(context: Context, deviceId: String) {
        val repo = AppRepository(context)
        val timer = repo.timers.first().firstOrNull { it.deviceId == deviceId } ?: return
        repo.removeTimer(deviceId)
        val device = repo.devices.first().firstOrNull { it.id == deviceId } ?: return

        val turnOn = timer.action == TimerAction.TurnOn
        val newState = device.state.copy(poweredOn = turnOn)
        val result = AcCommands.sendState(IrTransmitter(context), device, newState)
        repo.updateDeviceState(device.id, newState)

        if (!repo.timerReminders.first()) return
        val verb = if (turnOn) "turned on" else "turned off"
        val text = when (result) {
            AcCommands.Result.Sent -> "Sent. If it didn't respond, point your phone at the AC and tap power."
            else -> AcCommands.messageFor(result) + " — tap to open the remote."
        }
        notify(context, deviceId.hashCode(), "${device.name} $verb", text)
    }

    companion object {
        const val ACTION_FIRE = "com.ijad.breeze.action.TIMER_FIRE"
        const val EXTRA_DEVICE_ID = "deviceId"
        const val EXTRA_ACTION = "action"
        private const val CHANNEL_ID = "timers"

        fun notify(context: Context, id: Int, title: String, text: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Timers", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "When an AC timer turns your AC on or off"
                }
            )
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mode_fan)
                .setColor(CoolTint.toArgb())
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}

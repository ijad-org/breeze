package com.ijad.breeze.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ijad.breeze.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Alarms don't survive a reboot: reschedule future timers and drop ones that were missed. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val repo = AppRepository(app)
                val scheduler = TimerScheduler(app)
                val now = System.currentTimeMillis()
                repo.timers.first().forEach { timer ->
                    if (timer.triggerAtMillis > now) scheduler.schedule(timer)
                    else repo.removeTimer(timer.deviceId)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

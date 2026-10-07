package com.ijad.breeze.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.ijad.breeze.data.AcTimer
import java.util.Calendar

/**
 * Schedules AC timers with [AlarmManager]. Uses exact alarms when the user allows them
 * (Android 12+ gates this behind "Alarms & reminders"), otherwise an inexact alarm that
 * still fires while idle — usually within a few minutes.
 */
class TimerScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Settings screen where the user can allow exact alarms (Android 12+), or null. */
    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${appContext.packageName}"))
        } else null

    fun schedule(timer: AcTimer) {
        val pi = pendingIntent(timer.deviceId, timer)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.triggerAtMillis, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.triggerAtMillis, pi)
        }
    }

    fun cancel(deviceId: String) {
        alarmManager.cancel(pendingIntent(deviceId, null))
    }

    private fun pendingIntent(deviceId: String, timer: AcTimer?): PendingIntent {
        val intent = Intent(appContext, TimerReceiver::class.java).apply {
            action = TimerReceiver.ACTION_FIRE
            // Unique data URI per device so each device gets its own PendingIntent.
            data = Uri.parse("breeze://timer/$deviceId")
            putExtra(TimerReceiver.EXTRA_DEVICE_ID, deviceId)
            if (timer != null) putExtra(TimerReceiver.EXTRA_ACTION, timer.action.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        /** "Turn off in" — [hours]:[minutes] from now. */
        fun afterDuration(hours: Int, minutes: Int, now: Long = System.currentTimeMillis()): Long =
            now + (hours * 60L + minutes) * 60_000L

        /** "Turn on at" — next occurrence of [hour]:[minute] (today, or tomorrow if passed). */
        fun nextTimeOfDay(hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
    }
}

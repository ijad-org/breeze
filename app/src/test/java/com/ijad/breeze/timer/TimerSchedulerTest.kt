package com.ijad.breeze.timer

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class TimerSchedulerTest {
    private lateinit var savedZone: TimeZone

    @Before
    fun saveZone() {
        savedZone = TimeZone.getDefault()
    }

    @After
    fun restoreZone() {
        TimeZone.setDefault(savedZone)
    }

    private fun millis(zone: String, text: String): Long =
        LocalDateTime.parse(text).atZone(ZoneId.of(zone)).toInstant().toEpochMilli()

    private fun useZone(zone: String) = TimeZone.setDefault(TimeZone.getTimeZone(zone))

    @Test
    fun afterDuration_addsHoursAndMinutes() {
        assertEquals(1_000L + 5 * 60_000L, TimerScheduler.afterDuration(0, 5, now = 1_000L))
        assertEquals(1_000L + 150 * 60_000L, TimerScheduler.afterDuration(2, 30, now = 1_000L))
        assertEquals(1_000L, TimerScheduler.afterDuration(0, 0, now = 1_000L))
    }

    @Test
    fun nextTimeOfDay_laterToday() {
        useZone("Asia/Kolkata")
        val now = millis("Asia/Kolkata", "2026-10-08T14:20:37.123")
        assertEquals(
            millis("Asia/Kolkata", "2026-10-08T22:15:00"),
            TimerScheduler.nextTimeOfDay(22, 15, now)
        )
    }

    @Test
    fun nextTimeOfDay_passedTime_rollsToTomorrow() {
        useZone("Asia/Kolkata")
        val now = millis("Asia/Kolkata", "2026-10-08T14:20:00")
        assertEquals(
            millis("Asia/Kolkata", "2026-10-09T07:00:00"),
            TimerScheduler.nextTimeOfDay(7, 0, now)
        )
    }

    @Test
    fun nextTimeOfDay_sameMinute_rollsToTomorrow() {
        useZone("Asia/Kolkata")
        val now = millis("Asia/Kolkata", "2026-10-08T14:20:00")
        assertEquals(
            millis("Asia/Kolkata", "2026-10-09T14:20:00"),
            TimerScheduler.nextTimeOfDay(14, 20, now)
        )
    }

    @Test
    fun nextTimeOfDay_acrossYearEnd() {
        useZone("UTC")
        val now = millis("UTC", "2026-12-31T23:30:00")
        assertEquals(millis("UTC", "2027-01-01T06:00:00"), TimerScheduler.nextTimeOfDay(6, 0, now))
    }

    @Test
    fun nextTimeOfDay_keepsWallClockAcrossDstChange() {
        // Berlin springs forward on 2026-03-29, so that day is 23 hours long.
        useZone("Europe/Berlin")
        val now = millis("Europe/Berlin", "2026-03-28T22:00:00")
        assertEquals(
            millis("Europe/Berlin", "2026-03-29T07:00:00"),
            TimerScheduler.nextTimeOfDay(7, 0, now)
        )
    }
}

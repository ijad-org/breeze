package com.ijad.breeze.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "breeze")

class AppRepository(context: Context) {
    private val context = context.applicationContext
    private val devicesKey = stringPreferencesKey("devices_json")
    private val activeDeviceKey = stringPreferencesKey("active_device_id")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val tempAlertsKey = booleanPreferencesKey("notif_temp_alerts")
    private val timerRemindersKey = booleanPreferencesKey("notif_timer_reminders")
    private val ecoTipsKey = booleanPreferencesKey("notif_eco_tips")
    private val timersKey = stringPreferencesKey("timers_json")

    val devices: Flow<List<AcDevice>> = context.dataStore.data.map { prefs ->
        decodeDevices(prefs[devicesKey].orEmpty())
    }

    val activeDeviceId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[activeDeviceKey]
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            ThemeMode.Light.name -> ThemeMode.Light
            ThemeMode.Dark.name -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingDoneKey] == true
    }

    val tempAlerts: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[tempAlertsKey] != false
    }

    val timerReminders: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[timerRemindersKey] != false
    }

    val ecoTips: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ecoTipsKey] == true
    }

    val timers: Flow<List<AcTimer>> = context.dataStore.data.map { prefs ->
        decodeTimers(prefs[timersKey].orEmpty())
    }

    suspend fun setOnboardingDone(done: Boolean = true) {
        context.dataStore.edit { it[onboardingDoneKey] = done }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setTempAlerts(enabled: Boolean) {
        context.dataStore.edit { it[tempAlertsKey] = enabled }
    }

    suspend fun setTimerReminders(enabled: Boolean) {
        context.dataStore.edit { it[timerRemindersKey] = enabled }
    }

    suspend fun setEcoTips(enabled: Boolean) {
        context.dataStore.edit { it[ecoTipsKey] = enabled }
    }

    suspend fun setActiveDevice(deviceId: String) {
        context.dataStore.edit { it[activeDeviceKey] = deviceId }
    }

    suspend fun addDevice(
        name: String,
        brand: AcBrand,
        configIndex: Int
    ): AcDevice {
        val device = AcDevice(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "${brand.name} AC" },
            brandId = brand.id,
            brandName = brand.name,
            configIndex = configIndex
        )
        context.dataStore.edit { prefs ->
            // New ACs go to the end so the list in Settings keeps pairing order.
            val list = decodeDevices(prefs[devicesKey].orEmpty()) + device
            prefs[devicesKey] = encodeDevices(list)
            prefs[activeDeviceKey] = device.id
            prefs[onboardingDoneKey] = true
        }
        return device
    }

    suspend fun updateDeviceName(deviceId: String, name: String) {
        updateDevices { list -> list.map { if (it.id == deviceId) it.copy(name = name) else it } }
    }

    suspend fun updateDeviceState(deviceId: String, state: RemoteState) {
        updateDevices { list -> list.map { if (it.id == deviceId) it.copy(state = state) else it } }
    }

    suspend fun removeDevice(deviceId: String) {
        context.dataStore.edit { prefs ->
            val list = decodeDevices(prefs[devicesKey].orEmpty()).filterNot { it.id == deviceId }
            prefs[devicesKey] = encodeDevices(list)
            prefs[timersKey] = encodeTimers(
                decodeTimers(prefs[timersKey].orEmpty()).filterNot { it.deviceId == deviceId }
            )
            if (prefs[activeDeviceKey] == deviceId) {
                prefs[activeDeviceKey] = list.firstOrNull()?.id.orEmpty()
            }
        }
    }

    /** Replaces any existing timer for the same device. */
    suspend fun setTimer(timer: AcTimer) {
        context.dataStore.edit { prefs ->
            val list = decodeTimers(prefs[timersKey].orEmpty())
                .filterNot { it.deviceId == timer.deviceId } + timer
            prefs[timersKey] = encodeTimers(list)
        }
    }

    suspend fun removeTimer(deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[timersKey] = encodeTimers(
                decodeTimers(prefs[timersKey].orEmpty()).filterNot { it.deviceId == deviceId }
            )
        }
    }

    private suspend fun updateDevices(transform: (List<AcDevice>) -> List<AcDevice>) {
        context.dataStore.edit { prefs ->
            prefs[devicesKey] = encodeDevices(transform(decodeDevices(prefs[devicesKey].orEmpty())))
        }
    }

    private fun encodeDevices(list: List<AcDevice>): String {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("name", d.name)
                    .put("brandId", d.brandId)
                    .put("brandName", d.brandName)
                    .put("configIndex", d.configIndex)
                    .put("createdAt", d.createdAt)
                    .put(
                        "state",
                        JSONObject()
                            .put("poweredOn", d.state.poweredOn)
                            .put("mode", d.state.mode.name)
                            .put("temperatureC", d.state.temperatureC)
                            .put("fan", d.state.fan.name)
                            .put("swingOn", d.state.swingOn)
                    )
            )
        }
        return arr.toString()
    }

    private fun decodeDevices(raw: String): List<AcDevice> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        AcDevice(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            brandId = o.getString("brandId"),
                            brandName = o.getString("brandName"),
                            configIndex = o.optInt("configIndex", 0),
                            createdAt = o.optLong("createdAt", 0L),
                            state = decodeState(o.optJSONObject("state"))
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun decodeState(o: JSONObject?): RemoteState {
        val default = RemoteState()
        if (o == null) return default
        return RemoteState(
            poweredOn = o.optBoolean("poweredOn", default.poweredOn),
            mode = AcMode.entries.firstOrNull { it.name == o.optString("mode") } ?: default.mode,
            temperatureC = o.optInt("temperatureC", default.temperatureC).coerceIn(16, 30),
            fan = FanSpeed.entries.firstOrNull { it.name == o.optString("fan") } ?: default.fan,
            swingOn = o.optBoolean("swingOn", default.swingOn)
        )
    }

    private fun encodeTimers(list: List<AcTimer>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject()
                    .put("deviceId", t.deviceId)
                    .put("action", t.action.name)
                    .put("triggerAt", t.triggerAtMillis)
            )
        }
        return arr.toString()
    }

    private fun decodeTimers(raw: String): List<AcTimer> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val action = TimerAction.entries.firstOrNull { it.name == o.optString("action") }
                        ?: continue
                    add(AcTimer(o.getString("deviceId"), action, o.getLong("triggerAt")))
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

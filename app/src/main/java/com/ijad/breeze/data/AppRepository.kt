package com.ijad.breeze.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        StoreJson.decodeDevices(prefs[devicesKey].orEmpty())
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
        StoreJson.decodeTimers(prefs[timersKey].orEmpty())
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
            val list = StoreJson.decodeDevices(prefs[devicesKey].orEmpty()) + device
            prefs[devicesKey] = StoreJson.encodeDevices(list)
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
            val list = StoreJson.decodeDevices(prefs[devicesKey].orEmpty()).filterNot { it.id == deviceId }
            prefs[devicesKey] = StoreJson.encodeDevices(list)
            prefs[timersKey] = StoreJson.encodeTimers(
                StoreJson.decodeTimers(prefs[timersKey].orEmpty()).filterNot { it.deviceId == deviceId }
            )
            if (prefs[activeDeviceKey] == deviceId) {
                prefs[activeDeviceKey] = list.firstOrNull()?.id.orEmpty()
            }
        }
    }

    /** Replaces any existing timer for the same device. */
    suspend fun setTimer(timer: AcTimer) {
        context.dataStore.edit { prefs ->
            val list = StoreJson.decodeTimers(prefs[timersKey].orEmpty())
                .filterNot { it.deviceId == timer.deviceId } + timer
            prefs[timersKey] = StoreJson.encodeTimers(list)
        }
    }

    suspend fun removeTimer(deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[timersKey] = StoreJson.encodeTimers(
                StoreJson.decodeTimers(prefs[timersKey].orEmpty()).filterNot { it.deviceId == deviceId }
            )
        }
    }

    private suspend fun updateDevices(transform: (List<AcDevice>) -> List<AcDevice>) {
        context.dataStore.edit { prefs ->
            val list = StoreJson.decodeDevices(prefs[devicesKey].orEmpty())
            prefs[devicesKey] = StoreJson.encodeDevices(transform(list))
        }
    }
}

package com.ijad.breeze.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON stored in DataStore for the device and timer lists. Decoding uses `opt*` with defaults
 * so JSON written by older versions (for example devices without `state`) still loads.
 */
internal object StoreJson {
    fun encodeDevices(list: List<AcDevice>): String {
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

    fun decodeDevices(raw: String): List<AcDevice> {
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

    fun encodeTimers(list: List<AcTimer>): String {
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

    fun decodeTimers(raw: String): List<AcTimer> {
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

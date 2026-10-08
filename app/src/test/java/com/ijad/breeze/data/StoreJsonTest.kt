package com.ijad.breeze.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreJsonTest {

    @Test
    fun devices_roundTrip() {
        val devices = listOf(
            AcDevice("a", "Living Room", "lg", "LG", 2, createdAt = 1_700_000_000_000L),
            AcDevice(
                "b", "Bedroom", "daikin", "Daikin", 0, createdAt = 5L,
                state = RemoteState(
                    poweredOn = false,
                    mode = AcMode.Heat,
                    temperatureC = 29,
                    fan = FanSpeed.Auto,
                    swingOn = false
                )
            )
        )
        assertEquals(devices, StoreJson.decodeDevices(StoreJson.encodeDevices(devices)))
    }

    @Test
    fun devices_legacyJsonWithoutStateOrOptionalFields_usesDefaults() {
        val raw = """[{"id":"a","name":"Hall","brandId":"lg","brandName":"LG"}]"""
        assertEquals(
            listOf(AcDevice("a", "Hall", "lg", "LG", configIndex = 0, createdAt = 0L, state = RemoteState())),
            StoreJson.decodeDevices(raw)
        )
    }

    @Test
    fun devices_partialOrInvalidState_fallsBackPerField() {
        val raw = """[{"id":"a","name":"Hall","brandId":"lg","brandName":"LG",
            "state":{"mode":"Turbo","temperatureC":40,"fan":"High"}}]"""
        val state = StoreJson.decodeDevices(raw).single().state
        assertEquals(RemoteState(mode = AcMode.Cool, temperatureC = 30, fan = FanSpeed.High), state)
    }

    @Test
    fun devices_blankOrCorrupt_isEmpty() {
        assertEquals(emptyList<AcDevice>(), StoreJson.decodeDevices(""))
        assertEquals(emptyList<AcDevice>(), StoreJson.decodeDevices("{not json"))
        assertEquals(emptyList<AcDevice>(), StoreJson.decodeDevices("""[{"id":"a"}]"""))
    }

    @Test
    fun timers_roundTrip() {
        val timers = listOf(
            AcTimer("a", TimerAction.TurnOff, 1_700_000_000_000L),
            AcTimer("b", TimerAction.TurnOn, 42L)
        )
        assertEquals(timers, StoreJson.decodeTimers(StoreJson.encodeTimers(timers)))
    }

    @Test
    fun timers_unknownActionIsSkipped() {
        val raw = """[{"deviceId":"a","action":"Explode","triggerAt":1},
            {"deviceId":"b","action":"TurnOff","triggerAt":2}]"""
        assertEquals(listOf(AcTimer("b", TimerAction.TurnOff, 2L)), StoreJson.decodeTimers(raw))
    }

    @Test
    fun timers_blankOrCorrupt_isEmpty() {
        assertEquals(emptyList<AcTimer>(), StoreJson.decodeTimers(""))
        assertEquals(emptyList<AcTimer>(), StoreJson.decodeTimers("[1,2"))
    }
}

package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.data.RemoteState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrandIrTest {

    @Test
    fun carrier_routesControlToCarrierCodec() {
        assertArrayEquals(
            CarrierIrCodec.patternFor(true, AcMode.Heat, 22, FanSpeed.High, false),
            BrandIr.controlPattern("carrier", 0, RemoteState(true, AcMode.Heat, 22, FanSpeed.High, false))!!.micros
        )
        assertNull(BrandIr.controlPattern("haier", 0, RemoteState(true, AcMode.Heat, 22, FanSpeed.High, false)))
    }

    @Test
    fun carrierSwing_resendsState() {
        assertArrayEquals(
            BrandIr.controlPattern("carrier", 0, RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, true))!!.micros,
            BrandIr.swingPattern("carrier", 0, RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, true))!!.micros
        )
    }

    @Test
    fun carrierProbes() {
        assertEquals(5, BrandIr.probeCount("carrier", 99))
        val last = BrandIr.powerProbe("carrier", 40)!!
        assertEquals(CarrierIrCodec.FREQUENCY_HZ, last.frequencyHz)
        assertArrayEquals(CarrierIrCodec.powerProbeVariants()[4], last.micros)
    }

    @Test
    fun mitsubishi_routesControlAndSwingToMitsubishiCodec() {
        val state = RemoteState(true, AcMode.Dry, 23, FanSpeed.Medium, true)
        val expected = MitsubishiIrCodec.patternFor(true, AcMode.Dry, 23, FanSpeed.Medium, true)
        assertArrayEquals(expected, BrandIr.controlPattern("mitsubishi", 0, state)!!.micros)
        assertArrayEquals(expected, BrandIr.swingPattern("mitsubishi", 0, state)!!.micros)
        assertEquals(5, BrandIr.probeCount("mitsubishi", 99))
        assertArrayEquals(MitsubishiIrCodec.powerProbeVariants()[4], BrandIr.powerProbe("mitsubishi", 40)!!.micros)
    }

    @Test
    fun voltas_routesControlAndSwingToVoltasCodec() {
        val state = RemoteState(true, AcMode.Heat, 21, FanSpeed.Low, true)
        val expected = VoltasIrCodec.patternFor(true, AcMode.Heat, 21, FanSpeed.Low, true)
        assertArrayEquals(expected, BrandIr.controlPattern("voltas", 0, state)!!.micros)
        assertArrayEquals(expected, BrandIr.swingPattern("voltas", 0, state)!!.micros)
        assertEquals(5, BrandIr.probeCount("voltas", 99))
        assertArrayEquals(VoltasIrCodec.powerProbeVariants()[4], BrandIr.powerProbe("voltas", 40)!!.micros)
    }

    @Test
    fun daikin_configIndexPicksProtocol() {
        val state = RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, true)
        for ((index, protocol) in listOf(0 to DaikinIrCodec.Protocol.Arc4xx, 3 to DaikinIrCodec.Protocol.Arc433b69)) {
            val expected = DaikinIrCodec.patternFor(protocol, true, AcMode.Cool, 24, FanSpeed.Auto, true)
            assertArrayEquals(expected, BrandIr.controlPattern("daikin", index, state)!!.micros)
            assertArrayEquals(expected, BrandIr.swingPattern("daikin", index, state)!!.micros)
        }
        assertEquals(4, BrandIr.probeCount("daikin", 99))
    }
}

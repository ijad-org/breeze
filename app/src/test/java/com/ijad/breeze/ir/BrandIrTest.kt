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
            BrandIr.controlPattern("carrier", 0, RemoteState(true, AcMode.Heat, 22, FanSpeed.High, false), true)!!.micros
        )
        assertNull(BrandIr.controlPattern("acme", 0, RemoteState(true, AcMode.Heat, 22, FanSpeed.High, false), true))
    }

    @Test
    fun carrierSwing_resendsState() {
        assertArrayEquals(
            BrandIr.controlPattern("carrier", 0, RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, true), true)!!.micros,
            BrandIr.swingPattern("carrier", 0, RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, true))!!.micros
        )
    }

    @Test
    fun carrierProbes() {
        assertEquals(5, BrandIr.probeCount("carrier"))
        val last = BrandIr.powerProbe("carrier", 40)!!
        assertEquals(CarrierIrCodec.FREQUENCY_HZ, last.frequencyHz)
        assertArrayEquals(CarrierIrCodec.powerProbeVariants()[4], last.micros)
    }

    @Test
    fun mitsubishi_routesControlAndSwingToMitsubishiCodec() {
        val state = RemoteState(true, AcMode.Dry, 23, FanSpeed.Medium, true)
        val expected = MitsubishiIrCodec.patternFor(true, AcMode.Dry, 23, FanSpeed.Medium, true)
        assertArrayEquals(expected, BrandIr.controlPattern("mitsubishi", 0, state, true)!!.micros)
        assertArrayEquals(expected, BrandIr.swingPattern("mitsubishi", 0, state)!!.micros)
        assertEquals(5, BrandIr.probeCount("mitsubishi"))
        assertArrayEquals(MitsubishiIrCodec.powerProbeVariants()[4], BrandIr.powerProbe("mitsubishi", 40)!!.micros)
    }

    @Test
    fun voltas_routesControlAndSwingToVoltasCodec() {
        val state = RemoteState(true, AcMode.Heat, 21, FanSpeed.Low, true)
        val expected = VoltasIrCodec.patternFor(true, AcMode.Heat, 21, FanSpeed.Low, true)
        assertArrayEquals(expected, BrandIr.controlPattern("voltas", 0, state, true)!!.micros)
        assertArrayEquals(expected, BrandIr.swingPattern("voltas", 0, state)!!.micros)
        assertEquals(5, BrandIr.probeCount("voltas"))
        assertArrayEquals(VoltasIrCodec.powerProbeVariants()[4], BrandIr.powerProbe("voltas", 40)!!.micros)
    }

    @Test
    fun daikin_configIndexPicksProtocol() {
        val state = RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, true)
        for ((index, protocol) in listOf(0 to DaikinIrCodec.Protocol.Arc4xx, 3 to DaikinIrCodec.Protocol.Arc433b69)) {
            val expected = DaikinIrCodec.patternFor(protocol, true, AcMode.Cool, 24, FanSpeed.Auto, true)
            assertArrayEquals(expected, BrandIr.controlPattern("daikin", index, state, true)!!.micros)
            assertArrayEquals(expected, BrandIr.swingPattern("daikin", index, state)!!.micros)
        }
        assertEquals(4, BrandIr.probeCount("daikin"))
    }

    @Test
    fun panasonic_configIndexPicksModel() {
        val state = RemoteState(true, AcMode.Heat, 23, FanSpeed.High, true)
        for ((index, model) in listOf(0 to PanasonicIrCodec.Model.Dke, 5 to PanasonicIrCodec.Model.Jke, 9 to PanasonicIrCodec.Model.Rkr)) {
            val expected = PanasonicIrCodec.patternFor(model, true, AcMode.Heat, 23, FanSpeed.High, true)
            val control = BrandIr.controlPattern("panasonic", index, state, true)!!
            assertEquals(PanasonicIrCodec.FREQUENCY_HZ, control.frequencyHz)
            assertArrayEquals(expected, control.micros)
            assertArrayEquals(expected, BrandIr.swingPattern("panasonic", index, state)!!.micros)
        }
        assertEquals(14, BrandIr.probeCount("panasonic"))
    }

    @Test
    fun haier_configIndexPicksProtocolAndSwingPressesSwing() {
        val state = RemoteState(true, AcMode.Dry, 25, FanSpeed.Low, false)
        for ((index, protocol) in listOf(1 to HaierIrCodec.Protocol.Yrw02, 2 to HaierIrCodec.Protocol.Ac176, 5 to HaierIrCodec.Protocol.Hsu07)) {
            assertArrayEquals(
                HaierIrCodec.patternFor(protocol, true, AcMode.Dry, 25, FanSpeed.Low, false),
                BrandIr.controlPattern("haier", index, state, true)!!.micros
            )
            assertArrayEquals(
                HaierIrCodec.swingPatternFor(protocol, true, AcMode.Dry, 25, FanSpeed.Low, false),
                BrandIr.swingPattern("haier", index, state)!!.micros
            )
        }
        assertEquals(8, BrandIr.probeCount("haier"))
    }

    @Test
    fun otherAndBlueStar_sweepEverySupportedVariant() {
        val probeState = RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, false)
        for (brand in listOf("other", "bluestar")) {
            val count = BrandIr.probeCount(brand)
            assertEquals(18, count)
            // Each probe is the paired variant's own Cool 24° ON frame.
            for (i in 0 until count) {
                val probe = BrandIr.powerProbe(brand, i)!!
                val control = BrandIr.controlPattern(brand, i, probeState, true)!!
                assertEquals(control.frequencyHz, probe.frequencyHz)
                assertArrayEquals(control.micros, probe.micros)
            }
            assertEquals(count, (0 until count).map { BrandIr.powerProbe(brand, it)!!.micros.toList() }.toSet().size)
        }
        val state = RemoteState(true, AcMode.Heat, 21, FanSpeed.Medium, true)
        // Stored indices must keep their variant: Samsung first, LG second, Panasonic DKE eighth.
        assertArrayEquals(
            SamsungIrCodec.patternFor(true, AcMode.Heat, 21, FanSpeed.Medium, true),
            BrandIr.controlPattern("other", 0, state, true)!!.micros
        )
        assertArrayEquals(LgIrCodec.swingPattern(), BrandIr.swingPattern("other", 1, state)!!.micros)
        assertArrayEquals(
            PanasonicIrCodec.patternFor(PanasonicIrCodec.Model.Dke, true, AcMode.Heat, 21, FanSpeed.Medium, true),
            BrandIr.controlPattern("bluestar", 7, state, true)!!.micros
        )
        // Out-of-range codes fall back to the first variant.
        assertArrayEquals(
            BrandIr.controlPattern("other", 0, state, true)!!.micros,
            BrandIr.controlPattern("other", 99, state, true)!!.micros
        )
    }

    @Test
    fun togglingProtocols_toggleOnlyWhenPowerChanges() {
        val on = RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, false)
        for ((index, model) in listOf(10 to PanasonicIrCodec.Model.Ckp, 13 to PanasonicIrCodec.Model.Ac32)) {
            for (changed in listOf(true, false)) {
                assertArrayEquals(
                    PanasonicIrCodec.patternFor(model, true, AcMode.Cool, 24, FanSpeed.Auto, false, powerChanged = changed),
                    BrandIr.controlPattern("panasonic", index, on, changed)!!.micros
                )
            }
            // A swing press never toggles power.
            assertArrayEquals(
                PanasonicIrCodec.patternFor(model, true, AcMode.Cool, 24, FanSpeed.Auto, false, powerChanged = false),
                BrandIr.swingPattern("panasonic", index, on)!!.micros
            )
        }
        // The sweep's CKP and AC32 probes are toggles.
        assertArrayEquals(
            PanasonicIrCodec.patternFor(PanasonicIrCodec.Model.Ac32, true, AcMode.Cool, 24, FanSpeed.Auto, false, powerChanged = true),
            BrandIr.powerProbe("other", 17)!!.micros
        )
        assertArrayEquals(
            HaierIrCodec.patternFor(HaierIrCodec.Protocol.Ac160, true, AcMode.Cool, 24, FanSpeed.Auto, false),
            BrandIr.powerProbe("bluestar", 15)!!.micros
        )
    }

    @Test
    fun everyBrandHasRealProbesAndControl() {
        val state = RemoteState()
        for (brand in com.ijad.breeze.data.Brands.all) {
            assert(BrandIr.probeCount(brand.id) > 0) { brand.id }
            assert(BrandIr.controlPattern(brand.id, 0, state, true) != null) { brand.id }
        }
        assertEquals(0, BrandIr.probeCount("acme"))
        assertNull(BrandIr.powerProbe("acme", 0))
    }
}

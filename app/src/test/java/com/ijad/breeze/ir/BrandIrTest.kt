package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrandIrTest {

    @Test
    fun carrier_routesControlToCarrierCodec() {
        assertArrayEquals(
            CarrierIrCodec.patternFor(true, AcMode.Heat, 22, FanSpeed.High, false),
            BrandIr.controlPattern("carrier", true, AcMode.Heat, 22, FanSpeed.High, false)!!.micros
        )
        assertNull(BrandIr.controlPattern("daikin", true, AcMode.Heat, 22, FanSpeed.High, false))
    }

    @Test
    fun carrierSwing_resendsState() {
        assertArrayEquals(
            BrandIr.controlPattern("carrier", true, AcMode.Cool, 24, FanSpeed.Auto, true)!!.micros,
            BrandIr.swingPattern("carrier", true, AcMode.Cool, 24, FanSpeed.Auto, true)!!.micros
        )
    }

    @Test
    fun carrierProbes() {
        assertEquals(5, BrandIr.probeCount("carrier", 99))
        val last = BrandIr.powerProbe("carrier", 40)!!
        assertEquals(CarrierIrCodec.FREQUENCY_HZ, last.frequencyHz)
        assertArrayEquals(CarrierIrCodec.powerProbeVariants()[4], last.micros)
    }
}

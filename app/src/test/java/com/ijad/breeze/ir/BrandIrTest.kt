package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.data.RemoteState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrandIrTest {
    private val state = RemoteState(true, AcMode.Heat, 22, FanSpeed.High, swingOn = false)

    @Test
    fun lgAndCarrier_haveRealCodes_othersDoNot() {
        assertArrayEquals(
            LgIrCodec.patternFor(true, AcMode.Heat, 22, FanSpeed.High),
            BrandIr.controlPattern("lg", state)!!.micros
        )
        assertArrayEquals(
            CarrierIrCodec.patternFor(true, AcMode.Heat, 22, FanSpeed.High, false),
            BrandIr.controlPattern("carrier", state)!!.micros
        )
        assertNull(BrandIr.controlPattern("samsung", state))
        assertNull(BrandIr.swingPattern("samsung", state))
    }

    @Test
    fun swing_lgUsesToggleFrame_carrierResendsState() {
        assertArrayEquals(LgIrCodec.swingPattern(), BrandIr.swingPattern("lg", state)!!.micros)
        val on = state.copy(swingOn = true)
        assertArrayEquals(
            BrandIr.controlPattern("carrier", on)!!.micros,
            BrandIr.swingPattern("carrier", on)!!.micros
        )
    }

    @Test
    fun probes() {
        assertEquals(5, BrandIr.probeCount("lg", 99))
        assertEquals(5, BrandIr.probeCount("carrier", 99))
        assertEquals(12, BrandIr.probeCount("samsung", 12))
        assertArrayEquals(CarrierIrCodec.powerProbeVariants()[4], BrandIr.powerProbe("carrier", 40)!!.micros)
    }
}

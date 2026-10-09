package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CarrierIrCodecTest {

    /** Reads the 64-bit frame back out of an LSB-first mark/space pattern, checking the timings on the way. */
    private fun decode(pattern: IntArray): Long {
        assertEquals(131, pattern.size)
        assertEquals(8940, pattern[0])
        assertEquals(4556, pattern[1])
        assertEquals(503, pattern.last())
        var frame = 0L
        for (i in 0 until 64) {
            assertEquals(503, pattern[2 + i * 2])
            val bit = when (pattern[3 + i * 2]) {
                1736 -> 1L
                615 -> 0L
                else -> error("bad space ${pattern[3 + i * 2]} at bit $i")
            }
            frame = frame or (bit shl i)
        }
        return frame
    }

    private fun frame(on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean = true) =
        decode(CarrierIrCodec.patternFor(on, mode, temp, fan, swing))

    @Test
    fun knownStatesFromIrremoteEsp8266() {
        // stateReset(): Cool 28° Auto fan, swing on, power off.
        assertEquals(0x109000002C2A5584L, frame(false, AcMode.Cool, 28, FanSpeed.Auto))
        // Same state powered on (listed as a valid state in ir_Carrier_test.cpp).
        assertEquals(0x109000102C2B5584L, frame(true, AcMode.Cool, 28, FanSpeed.Auto))
    }

    @Test
    fun matchesRealCaptureInStateBytes() {
        // Real Carrier capture: Heat 30° Low, swing on, power on. Bytes 5–7 hold
        // timer values (ignored while the timers are disabled) and feed the checksum,
        // so compare bytes 0–4 without the checksum nibble.
        val real = 0x404000102E5E5584L
        val mask = 0x000000FFFFF0FFFFL
        assertEquals(real and mask, frame(true, AcMode.Heat, 30, FanSpeed.Low) and mask)
        assertEquals(0x109000102E505584L, frame(true, AcMode.Heat, 30, FanSpeed.Low))
    }

    @Test
    fun checksum_matchesLibraryExamples() {
        assertEquals(0xC, CarrierIrCodec.checksum(0x90900030205C5584uL.toLong()))
        assertEquals(0xE, CarrierIrCodec.checksum(0x404000102E5E5584L))
        assertEquals(0xA, CarrierIrCodec.checksum(0x109000002C2A5584L))
    }

    @Test
    fun fields_areEncodedForEveryState() {
        for (on in listOf(true, false)) for (swing in listOf(true, false))
            for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
                val f = frame(on, mode, temp, fan, swing)
                val label = "$on $mode $temp $fan $swing"
                assertEquals(label, CarrierIrCodec.checksum(f), ((f ushr 16) and 0xF).toInt())
                assertEquals(label, temp - 16, ((f ushr 24) and 0xF).toInt())
                assertEquals(label, if (swing) 1L else 0L, (f ushr 29) and 1)
                assertEquals(label, if (on) 1L else 0L, (f ushr 36) and 1)
                val expectedMode = when (mode) { AcMode.Heat -> 1; AcMode.Fan -> 3; else -> 2 }
                assertEquals(label, expectedMode, ((f ushr 20) and 0x3).toInt())
                assertEquals(label, fan.ordinal.let { listOf(1, 2, 3, 0)[it] }, ((f ushr 22) and 0x3).toInt())
            }
    }

    @Test
    fun dryAndAuto_fallBackToCool() {
        val cool = frame(true, AcMode.Cool, 24, FanSpeed.Medium)
        assertEquals(cool, frame(true, AcMode.Dry, 24, FanSpeed.Medium))
        assertEquals(cool, frame(true, AcMode.Auto, 24, FanSpeed.Medium))
    }

    @Test
    fun temperature_isClampedTo16to30() {
        assertEquals(frame(true, AcMode.Cool, 16, FanSpeed.Low), frame(true, AcMode.Cool, 10, FanSpeed.Low))
        assertEquals(frame(true, AcMode.Cool, 30, FanSpeed.Low), frame(true, AcMode.Cool, 35, FanSpeed.Low))
    }

    @Test
    fun powerProbes_startWithCool24AutoThenOff() {
        val probes = CarrierIrCodec.powerProbeVariants()
        assertEquals(5, probes.size)
        assertArrayEquals(CarrierIrCodec.patternFor(true, AcMode.Cool, 24, FanSpeed.Auto, true), probes[0])
        assertArrayEquals(CarrierIrCodec.patternFor(false, AcMode.Cool, 24, FanSpeed.Auto, true), probes[1])
    }
}

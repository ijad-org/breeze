package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SamsungIrCodecTest {

    /** Reads the bytes back out of a mark/space pattern, checking the timings on the way. */
    private fun decode(pattern: IntArray): IntArray {
        assertEquals(2 + 3 * (2 + 7 * 16 + 1) + 2, pattern.size)
        assertEquals(690, pattern[0])
        assertEquals(17844, pattern[1])
        val bytes = IntArray(21)
        var i = 2
        for (s in 0 until 3) {
            if (s > 0) assertEquals(2886, pattern[i++])
            assertEquals(3086, pattern[i++])
            assertEquals(8864, pattern[i++])
            for (b in 0 until 56) {
                assertEquals(586, pattern[i++])
                val bit = when (pattern[i++]) {
                    1432 -> 1
                    436 -> 0
                    else -> error("bad space at section $s bit $b")
                }
                bytes[s * 7 + b / 8] = bytes[s * 7 + b / 8] or (bit shl (b % 8))
            }
            assertEquals(586, pattern[i++])
        }
        return bytes
    }

    private fun state(on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean) =
        SamsungIrCodec.stateFor(on, mode, temp, fan, swing)

    @Test
    fun matchesReferenceOnAndOffFrames() {
        // IRSamsungAc::sendOn / sendOff in IRremoteESP8266: Cool 24°, auto fan, swing off.
        assertArrayEquals(
            intArrayOf(
                0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
                0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
                0x01, 0xE2, 0xFE, 0x71, 0x80, 0x11, 0xF0
            ),
            state(true, AcMode.Cool, 24, FanSpeed.Auto, false)
        )
        assertArrayEquals(
            intArrayOf(
                0x02, 0xB2, 0x0F, 0x00, 0x00, 0x00, 0xC0,
                0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
                0x01, 0x02, 0xFF, 0x71, 0x80, 0x11, 0xC0
            ),
            state(false, AcMode.Cool, 24, FanSpeed.Auto, false)
        )
    }

    @Test
    fun pattern_encodesStateLsbFirst() {
        for (on in listOf(true, false)) for (swing in listOf(true, false)) {
            assertArrayEquals(
                state(on, AcMode.Heat, 27, FanSpeed.High, swing),
                decode(SamsungIrCodec.patternFor(on, AcMode.Heat, 27, FanSpeed.High, swing))
            )
        }
    }

    @Test
    fun fields() {
        val s = state(true, AcMode.Dry, 19, FanSpeed.Medium, true)
        assertEquals(19 - 16, s[18] ushr 4)
        assertEquals(4, (s[19] ushr 1) and 0x7) // Medium
        assertEquals(2, (s[19] ushr 4) and 0x7) // Dry
        assertEquals(0b010, (s[16] ushr 4) and 0x7) // Vertical swing
        val modes = mapOf(AcMode.Auto to 0, AcMode.Cool to 1, AcMode.Dry to 2, AcMode.Fan to 3, AcMode.Heat to 4)
        for ((mode, bits) in modes) {
            assertEquals(bits, (state(true, mode, 24, FanSpeed.Low, false)[19] ushr 4) and 0x7)
        }
        val fans = mapOf(FanSpeed.Auto to 0, FanSpeed.Low to 2, FanSpeed.Medium to 4, FanSpeed.High to 5)
        for ((fan, bits) in fans) {
            assertEquals(bits, (state(true, AcMode.Cool, 24, fan, false)[19] ushr 1) and 0x7)
        }
    }

    @Test
    fun autoMode_usesItsOwnAutoFan() {
        for (fan in FanSpeed.entries) {
            assertEquals(6, (state(true, AcMode.Auto, 24, fan, false)[19] ushr 1) and 0x7)
        }
    }

    @Test
    fun everyState_hasValidChecksums() {
        for (on in listOf(true, false)) for (mode in AcMode.entries) for (temp in 16..30)
            for (fan in FanSpeed.entries) for (swing in listOf(true, false)) {
                val s = state(on, mode, temp, fan, swing)
                for (offset in 0 until 21 step 7) {
                    val sum = SamsungIrCodec.sectionChecksum(s, offset)
                    assertEquals(sum and 0x0F, s[offset + 1] ushr 4)
                    assertEquals(sum ushr 4, s[offset + 2] and 0x0F)
                }
            }
    }

    @Test
    fun temperature_isClampedTo16to30() {
        assertArrayEquals(
            state(true, AcMode.Cool, 16, FanSpeed.Low, true),
            state(true, AcMode.Cool, 10, FanSpeed.Low, true)
        )
        assertArrayEquals(
            state(true, AcMode.Heat, 30, FanSpeed.Low, true),
            state(true, AcMode.Heat, 35, FanSpeed.Low, true)
        )
    }

    @Test
    fun powerProbes_startWithCool24AutoThenOff() {
        val probes = SamsungIrCodec.powerProbeVariants()
        assertEquals(5, probes.size)
        assertArrayEquals(SamsungIrCodec.patternFor(true, AcMode.Cool, 24, FanSpeed.Auto, false), probes[0])
        assertArrayEquals(SamsungIrCodec.patternFor(false, AcMode.Cool, 24, FanSpeed.Auto, false), probes[1])
    }

    @Test
    fun brandIr_routesSamsung() {
        val control = BrandIr.controlPattern("samsung", true, AcMode.Cool, 24, FanSpeed.Auto, false)!!
        assertEquals(38_000, control.frequencyHz)
        assertArrayEquals(state(true, AcMode.Cool, 24, FanSpeed.Auto, false), decode(control.micros))
        val swing = BrandIr.swingPattern("samsung", true, AcMode.Cool, 24, FanSpeed.Auto, true)!!
        assertArrayEquals(state(true, AcMode.Cool, 24, FanSpeed.Auto, true), decode(swing.micros))
        assertEquals(5, BrandIr.probeCount("samsung", 12))
        assertEquals(12, BrandIr.probeCount("daikin", 12))
    }
}

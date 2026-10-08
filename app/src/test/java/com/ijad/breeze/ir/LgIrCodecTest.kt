package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LgIrCodecTest {

    /** Reads the 28-bit frame back out of a mark/space pattern, checking the timings on the way. */
    private fun decode(pattern: IntArray): Int {
        assertEquals(59, pattern.size)
        assertEquals(8500, pattern[0])
        assertEquals(4250, pattern[1])
        assertEquals(550, pattern.last())
        var frame = 0
        for (i in 0 until 28) {
            assertEquals(550, pattern[2 + i * 2])
            val bit = when (pattern[3 + i * 2]) {
                1600 -> 1
                550 -> 0
                else -> error("bad space ${pattern[3 + i * 2]} at bit $i")
            }
            frame = (frame shl 1) or bit
        }
        return frame
    }

    private fun frame(mode: AcMode, temp: Int, fan: FanSpeed) =
        decode(LgIrCodec.patternFor(true, mode, temp, fan))

    @Test
    fun powerOff_isFixedOffFrame() {
        assertEquals(0x88C0051, decode(LgIrCodec.patternFor(false, AcMode.Heat, 30, FanSpeed.High)))
        assertEquals(0x88C0051, decode(LgIrCodec.powerOffPattern()))
    }

    @Test
    fun knownFrames() {
        // Signature 0x88, power nibble 0, mode, temp - 15, fan, checksum.
        assertEquals(0x8800347, frame(AcMode.Cool, 18, FanSpeed.High))
        assertEquals(0x880095E, frame(AcMode.Cool, 24, FanSpeed.Auto))
        assertEquals(0x8804952, frame(AcMode.Heat, 24, FanSpeed.Auto))
        assertEquals(0x8801B2E, frame(AcMode.Dry, 26, FanSpeed.Medium))
        assertEquals(0x8802114, frame(AcMode.Fan, 16, FanSpeed.Low))
        assertEquals(0x8803F46, frame(AcMode.Auto, 30, FanSpeed.High))
    }

    @Test
    fun everyFrame_hasSignatureAndValidChecksum() {
        for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
            val f = frame(mode, temp, fan)
            assertEquals(0x88, f ushr 20)
            assertEquals(temp - 15, (f ushr 8) and 0xF)
            val sum = (1..4).sumOf { (f ushr (it * 4)) and 0xF } and 0xF
            assertEquals("checksum for $mode $temp $fan", sum, f and 0xF)
        }
    }

    @Test
    fun temperature_isClampedTo16to30() {
        assertEquals(frame(AcMode.Cool, 16, FanSpeed.Low), frame(AcMode.Cool, 10, FanSpeed.Low))
        assertEquals(frame(AcMode.Cool, 30, FanSpeed.Low), frame(AcMode.Cool, 35, FanSpeed.Low))
    }

    @Test
    fun swingFrames() {
        assertEquals(0x8813149, decode(LgIrCodec.swingPattern()))
        assertEquals(0x881316B, decode(LgIrCodec.swingHorizontalPattern()))
    }

    @Test
    fun powerProbes_startWithCool24AutoThenOff() {
        val probes = LgIrCodec.powerProbeVariants()
        assertEquals(5, probes.size)
        assertArrayEquals(LgIrCodec.patternFor(true, AcMode.Cool, 24, FanSpeed.Auto), probes[0])
        assertArrayEquals(LgIrCodec.powerOffPattern(), probes[1])
    }
}

package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CoolixIrCodecTest {

    /** Reads the 6 sent bytes (each byte, then its inverse) back out of one MSB-first send. */
    private fun decodeSend(pattern: IntArray, start: Int): IntArray {
        assertEquals(4692, pattern[start])
        assertEquals(4416, pattern[start + 1])
        val out = IntArray(6)
        for (i in 0 until 48) {
            assertEquals(552, pattern[start + 2 + i * 2])
            val bit = when (pattern[start + 3 + i * 2]) {
                1656 -> 1
                552 -> 0
                else -> error("bad space at bit $i")
            }
            out[i / 8] = out[i / 8] or (bit shl (7 - i % 8))
        }
        assertEquals(552, pattern[start + 2 + 96])
        return out
    }

    /** The frame is sent twice, 5244 µs apart. */
    private fun decode(pattern: IntArray): IntArray {
        val sendLength = 2 + 96 + 1
        assertEquals(sendLength * 2 + 1, pattern.size)
        assertEquals(5244, pattern[sendLength])
        val first = decodeSend(pattern, 0)
        assertArrayEquals(first, decodeSend(pattern, sendLength + 1))
        return first
    }

    private fun frame(on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed) =
        CoolixIrCodec.stateFor(on, mode, temp, fan)

    @Test
    fun matchesRealRemoteSequenceFromIssue722() {
        // ir_Coolix_test.cpp Issue722, codes from a real remote.
        assertEquals(0xB21F08, frame(true, AcMode.Auto, 17, FanSpeed.Auto))
        assertEquals(0xB27BE0, frame(false, AcMode.Auto, 17, FanSpeed.Auto))
        assertEquals(0xB21F18, frame(true, AcMode.Auto, 18, FanSpeed.Auto))
        assertEquals(0xB2BF10, frame(true, AcMode.Cool, 18, FanSpeed.Auto))
        assertEquals(0xB21F14, frame(true, AcMode.Dry, 18, FanSpeed.Auto))
        assertEquals(0xB2BF1C, frame(true, AcMode.Heat, 18, FanSpeed.Auto))
        assertEquals(0xB2BFE4, frame(true, AcMode.Fan, 18, FanSpeed.Auto))
        assertEquals(0xB29FE4, frame(true, AcMode.Fan, 18, FanSpeed.Low))
        assertEquals(0xB25FE4, frame(true, AcMode.Fan, 18, FanSpeed.Medium))
        assertEquals(0xB23FE4, frame(true, AcMode.Fan, 18, FanSpeed.High))
        // Issue624: Cool 24° fan auto.
        assertEquals(0xB2BF40, frame(true, AcMode.Cool, 24, FanSpeed.Auto))
    }

    @Test
    fun offFrameMatchesBlueStarCapture() {
        // IRremoteESP8266 #498, a Blue Star remote: 0xB24D7B84E01F, i.e. the Coolix off frame with inverses.
        assertArrayEquals(
            intArrayOf(0xB2, 0x4D, 0x7B, 0x84, 0xE0, 0x1F),
            decode(CoolixIrCodec.patternFor(false, AcMode.Cool, 24, FanSpeed.Auto))
        )
    }

    @Test
    fun senderMatchesLibraryOutput() {
        // ir_Coolix_test.cpp SendCoolix SendDataOnly, sendCOOLIX(0x0): header, 3 × (0x00, 0xFF), footer, sent twice.
        val zeros = List(8) { listOf(552, 552) }.flatten()
        val ones = List(8) { listOf(552, 1656) }.flatten()
        val send = listOf(4692, 4416) + List(3) { zeros + ones }.flatten() + 552
        assertArrayEquals((send + 5244 + send).toIntArray(), CoolixIrCodec.frameToPattern(0x0))
    }

    @Test
    fun everyFrameCarriesItsInverse() {
        for (on in listOf(true, false)) for (mode in AcMode.entries) for (temp in 17..30) for (fan in FanSpeed.entries) {
            val bytes = decode(CoolixIrCodec.patternFor(on, mode, temp, fan))
            for (i in 0 until 3) assertEquals(bytes[i * 2] xor 0xFF, bytes[i * 2 + 1])
        }
        assertArrayEquals(
            intArrayOf(0xB2, 0x4D, 0x6B, 0x94, 0xE0, 0x1F),
            decode(CoolixIrCodec.swingPattern())
        )
    }

    @Test
    fun temperature_isClampedTo17to30() {
        assertEquals(frame(true, AcMode.Cool, 17, FanSpeed.Low), frame(true, AcMode.Cool, 16, FanSpeed.Low))
        assertEquals(frame(true, AcMode.Cool, 30, FanSpeed.Low), frame(true, AcMode.Cool, 35, FanSpeed.Low))
    }
}

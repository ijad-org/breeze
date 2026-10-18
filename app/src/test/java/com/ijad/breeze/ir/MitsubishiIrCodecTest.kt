package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MitsubishiIrCodecTest {

    private fun bytes(vararg b: Int) = b

    /** Reads the 18-byte state back out of one LSB-first send, checking the timings on the way. */
    private fun decodeSend(pattern: IntArray, start: Int): IntArray {
        assertEquals(3400, pattern[start])
        assertEquals(1750, pattern[start + 1])
        val state = IntArray(MitsubishiIrCodec.STATE_LENGTH)
        for (i in 0 until MitsubishiIrCodec.STATE_LENGTH * 8) {
            assertEquals(450, pattern[start + 2 + i * 2])
            val bit = when (pattern[start + 3 + i * 2]) {
                1300 -> 1
                420 -> 0
                else -> error("bad space ${pattern[start + 3 + i * 2]} at bit $i")
            }
            state[i / 8] = state[i / 8] or (bit shl (i % 8))
        }
        assertEquals(440, pattern[start + 2 + MitsubishiIrCodec.STATE_LENGTH * 16])
        return state
    }

    /** The message is sent twice, 15.5 ms apart. */
    private fun decode(pattern: IntArray): IntArray {
        val sendLength = 2 + MitsubishiIrCodec.STATE_LENGTH * 16 + 1
        assertEquals(sendLength * 2 + 1, pattern.size)
        assertEquals(15500, pattern[sendLength])
        val first = decodeSend(pattern, 0)
        assertArrayEquals(first, decodeSend(pattern, sendLength + 1))
        return first
    }

    private fun state(on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean = false) =
        decode(MitsubishiIrCodec.patternFor(on, mode, temp, fan, swing))

    @Test
    fun matchesRealCapture() {
        // ir_Mitsubishi_test.cpp DecodeRealExample: power off, Cool 26°, fan 1, vane swing, clock 00:00.
        assertArrayEquals(
            bytes(0x23, 0xCB, 0x26, 0x01, 0x00, 0x00, 0x18, 0x0A, 0x36, 0x79, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xE6),
            state(false, AcMode.Cool, 26, FanSpeed.Low, swing = true)
        )
    }

    @Test
    fun settingsBytesMatchLibraryResetState() {
        // stateReset() (power on, Heat 22°) with its checksum, from ir_Mitsubishi_test.cpp.
        val reset = bytes(0x23, 0xCB, 0x26, 0x01, 0x00, 0x20, 0x08, 0x06, 0x30, 0x45, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F)
        assertEquals(0x1F, MitsubishiIrCodec.checksum(reset))
        // Bytes 0–8 (header, power, mode, temp, mode/wide vane) match; byte 9 differs because the reset uses fan 5.
        assertArrayEquals(reset.copyOfRange(0, 9), state(true, AcMode.Heat, 22, FanSpeed.Auto).copyOfRange(0, 9))
    }

    @Test
    fun fields_areEncodedForEveryState() {
        for (on in listOf(true, false)) for (swing in listOf(true, false))
            for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
                val s = state(on, mode, temp, fan, swing)
                val label = "$on $mode $temp $fan $swing"
                assertEquals(label, MitsubishiIrCodec.checksum(s), s[17])
                assertEquals(label, if (on) 0x20 else 0, s[5])
                val expectedMode = when (mode) {
                    AcMode.Heat -> 1; AcMode.Dry -> 2; AcMode.Cool -> 3; AcMode.Auto -> 4; AcMode.Fan -> 7
                }
                assertEquals(label, expectedMode, (s[6] ushr 3) and 0b111)
                assertEquals(label, temp - 16, s[7] and 0xF)
                assertEquals(label, 3, s[8] ushr 4) // wide vane: middle
                assertEquals(label, listOf(1, 2, 3, 0)[fan.ordinal], s[9] and 0b111)
                assertEquals(label, if (swing) 0b111 else 0, (s[9] ushr 3) and 0b111)
                assertEquals(label, 1, (s[9] ushr 6) and 1)
                assertEquals(label, if (fan == FanSpeed.Auto) 1 else 0, s[9] ushr 7)
            }
    }

    @Test
    fun temperature_isClampedTo16to31() {
        assertArrayEquals(state(true, AcMode.Cool, 16, FanSpeed.Low), state(true, AcMode.Cool, 10, FanSpeed.Low))
        assertArrayEquals(state(true, AcMode.Cool, 31, FanSpeed.Low), state(true, AcMode.Cool, 35, FanSpeed.Low))
    }

    @Test
    fun powerProbes_startWithCool24AutoThenOff() {
        val probes = MitsubishiIrCodec.powerProbeVariants()
        assertEquals(5, probes.size)
        assertArrayEquals(MitsubishiIrCodec.patternFor(true, AcMode.Cool, 24, FanSpeed.Auto, false), probes[0])
        assertArrayEquals(MitsubishiIrCodec.patternFor(false, AcMode.Cool, 24, FanSpeed.Auto, false), probes[1])
    }
}

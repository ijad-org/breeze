package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.ir.HaierIrCodec.Protocol
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HaierIrCodecTest {

    private fun bytes(vararg b: Int) = b

    /** Reads the MSB-first bytes back out of a pattern, checking the double header and timings. */
    private fun decode(pattern: IntArray): IntArray {
        assertEquals(listOf(3000, 3000, 3000, 4300), pattern.take(4))
        val count = (pattern.size - 5) / 16
        assertEquals(4 + count * 16 + 1, pattern.size)
        val out = IntArray(count)
        for (i in 0 until count * 8) {
            assertEquals(520, pattern[4 + i * 2])
            val bit = when (pattern[5 + i * 2]) {
                1650 -> 1
                650 -> 0
                else -> error("bad space ${pattern[5 + i * 2]} at bit $i")
            }
            out[i / 8] = out[i / 8] or (bit shl (7 - i % 8))
        }
        assertEquals(520, pattern.last())
        return out
    }

    private fun state(p: Protocol, on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean = false) =
        decode(HaierIrCodec.patternFor(p, on, mode, temp, fan, swing))

    private fun swingState(p: Protocol, on: Boolean, swing: Boolean) =
        decode(HaierIrCodec.swingPatternFor(p, on, AcMode.Cool, 24, FanSpeed.Auto, swing))

    @Test
    fun hsu07_matchesRealCapture() {
        // ir_Haier_test.cpp Issue668: real capture of "on", Cool 25°, fan low, swing off.
        assertArrayEquals(
            bytes(0xA5, 0x91, 0x20, 0x00, 0x0C, 0xC0, 0x20, 0x00, 0x42),
            state(Protocol.Hsu07, true, AcMode.Cool, 25, FanSpeed.Low)
        )
    }

    @Test
    fun ac176_matchesRealCaptureApartFromSwing() {
        // ir_Haier_test.cpp DecodeHaierAC176 RealExample (issue #1480): on, Cool 24°, fan auto, power button.
        val capture = bytes(
            0xA6, 0x86, 0x00, 0x00, 0x40, 0xA0, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x05, 0x31,
            0xB7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xB7
        )
        assertEquals(0x31, HaierIrCodec.sum(capture, 0, 13))
        assertEquals(0xB7, HaierIrCodec.sum(capture, 14, 21))
        val s = state(Protocol.Ac176, true, AcMode.Cool, 24, FanSpeed.Auto)
        // The capture's vane value (6) isn't a documented position; the app sends 0 (off) or 0xC (auto).
        assertEquals(0x80, s[1])
        assertArrayEquals(capture.copyOfRange(2, 13), s.copyOfRange(2, 13))
        assertArrayEquals(capture.copyOfRange(14, 21), s.copyOfRange(14, 21))
        assertEquals(capture[0], s[0])
        assertEquals(HaierIrCodec.sum(s, 0, 13), s[13])
        assertEquals(capture[21], s[21])
    }

    @Test
    fun ac160_matchesRealCapture() {
        // ir_Haier_test.cpp DecodeHaierAC160 RealExample (issue #1804): on, Cool 26°, fan low, swing auto, power button.
        assertArrayEquals(
            bytes(
                0xA6, 0xAC, 0x00, 0x00, 0x40, 0x60, 0x00, 0x20, 0x00, 0x00,
                0x00, 0x00, 0x05, 0x17, 0xB5, 0x00, 0x60, 0x00, 0x00, 0x15
            ),
            state(Protocol.Ac160, true, AcMode.Cool, 26, FanSpeed.Low, swing = true)
        )
    }

    @Test
    fun ac160_auxHeatingFollowsHeatMode() {
        for (mode in AcMode.entries) for (fan in FanSpeed.entries) {
            val s = state(Protocol.Ac160, true, mode, 22, fan)
            assertEquals("$mode", if (mode == AcMode.Heat) 0xC0 else 0x40, s[4])
            assertEquals("$mode $fan", if (fan == FanSpeed.Auto) 0 else listOf(3, 2, 1)[fan.ordinal], s[16] ushr 5)
            assertEquals(HaierIrCodec.sum(s, 0, 13), s[13])
            assertEquals(HaierIrCodec.sum(s, 14, 19), s[19])
        }
    }

    @Test
    fun yrw02_checksumMatchesRealCaptures() {
        // ir_Haier_test.cpp DecodeHaierAC_YRW02 RealExample (issue #485) and RealStates.
        val captures = listOf(
            bytes(0xA6, 0x12, 0x00, 0x02, 0x40, 0x20, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x05, 0x3F),
            bytes(0xA6, 0xE1, 0x00, 0x00, 0x40, 0x20, 0x00, 0x80, 0x00, 0x00, 0x00, 0x00, 0x07, 0x6E),
            bytes(0xA6, 0x9C, 0x00, 0x02, 0x40, 0xA8, 0x00, 0x20, 0x80, 0x00, 0x00, 0x00, 0x0B, 0xD7)
        )
        for (c in captures) assertEquals(c[13], HaierIrCodec.sum(c, 0, 13))
    }

    @Test
    fun yrw02_isTheFirst14BytesOfAc176() {
        for (on in listOf(true, false)) for (mode in AcMode.entries) for (fan in FanSpeed.entries) {
            val yrw02 = state(Protocol.Yrw02, on, mode, 22, fan, swing = true)
            assertEquals(14, yrw02.size)
            assertArrayEquals(yrw02, state(Protocol.Ac176, on, mode, 22, fan, swing = true).copyOfRange(0, 14))
        }
    }

    @Test
    fun yrw02Fields_areEncodedForEveryState() {
        for (p in listOf(Protocol.Yrw02, Protocol.Ac176)) for (on in listOf(true, false))
            for (swing in listOf(true, false)) for (mode in AcMode.entries) for (temp in 16..30)
                for (fan in FanSpeed.entries) {
                    val s = state(p, on, mode, temp, fan, swing)
                    val label = "$p $on $mode $temp $fan $swing"
                    assertEquals(label, 0xA6, s[0])
                    assertEquals(label, temp - 16, s[1] ushr 4)
                    assertEquals(label, if (swing) 0xC else 0, s[1] and 0xF)
                    assertEquals(label, if (on) 0x40 else 0, s[4])
                    val fanBits = listOf(3, 2, 1, 5)[fan.ordinal]
                    assertEquals(label, fanBits, s[5] ushr 5)
                    assertEquals(label, listOf(1, 4, 2, 6, 0)[mode.ordinal], s[7] ushr 5)
                    assertEquals(label, 5, s[12])
                    assertEquals(label, HaierIrCodec.sum(s, 0, 13), s[13])
                    if (p == Protocol.Ac176) {
                        assertEquals(label, 0xB7, s[14])
                        assertEquals(label, if (fan == FanSpeed.Auto) 0 else fanBits, s[16] ushr 6)
                        assertEquals(label, HaierIrCodec.sum(s, 14, 21), s[21])
                    }
                }
    }

    @Test
    fun hsu07Fields_areEncodedForEveryState() {
        for (on in listOf(true, false)) for (swing in listOf(true, false))
            for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
                val s = state(Protocol.Hsu07, on, mode, temp, fan, swing)
                val label = "$on $mode $temp $fan $swing"
                assertEquals(label, 9, s.size)
                assertEquals(label, if (on) 1 else 0, s[1] and 0xF)
                assertEquals(label, temp - 16, s[1] ushr 4)
                assertEquals(label, if (swing) 3 else 0, s[2] ushr 6)
                assertEquals(label, listOf(3, 2, 1, 0)[fan.ordinal], s[5] ushr 6)
                assertEquals(label, listOf(1, 3, 2, 4, 0)[mode.ordinal], s[6] ushr 5)
                assertEquals(label, HaierIrCodec.sum(s, 0, 8), s[8])
            }
    }

    @Test
    fun swingPress_namesTheSwingButton() {
        for (p in listOf(Protocol.Yrw02, Protocol.Ac176, Protocol.Ac160)) {
            val s = swingState(p, true, swing = true)
            assertEquals(2, s[12])
            assertEquals(0xC, s[1] and 0xF)
            assertEquals(HaierIrCodec.sum(s, 0, 13), s[13])
        }
        assertEquals(0b1101, swingState(Protocol.Hsu07, true, swing = true)[1] and 0xF)
        assertEquals(3, swingState(Protocol.Hsu07, true, swing = true)[2] ushr 6)
        // While off, HSU07 frames always carry the Off command.
        assertEquals(0, swingState(Protocol.Hsu07, false, swing = true)[1] and 0xF)
    }

    @Test
    fun temperature_isClampedTo16to30() {
        for (p in Protocol.entries) {
            assertArrayEquals(state(p, true, AcMode.Cool, 16, FanSpeed.Low), state(p, true, AcMode.Cool, 10, FanSpeed.Low))
            assertArrayEquals(state(p, true, AcMode.Cool, 30, FanSpeed.Low), state(p, true, AcMode.Cool, 35, FanSpeed.Low))
        }
    }

    @Test
    fun powerProbes_areOnThenOffPerProtocol() {
        val probes = HaierIrCodec.powerProbeVariants()
        assertEquals(8, probes.size)
        Protocol.entries.forEachIndexed { i, p ->
            assertArrayEquals(HaierIrCodec.patternFor(p, true, AcMode.Cool, 24, FanSpeed.Auto, false), probes[i * 2])
            assertArrayEquals(HaierIrCodec.patternFor(p, false, AcMode.Cool, 24, FanSpeed.Auto, false), probes[i * 2 + 1])
            assertEquals(p, HaierIrCodec.protocolFor(i * 2 + 1))
        }
        assertEquals(Protocol.Yrw02, HaierIrCodec.protocolFor(9))
        assertEquals(Protocol.Yrw02, HaierIrCodec.protocolFor(-1))
    }
}

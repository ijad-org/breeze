package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.ir.PanasonicIrCodec.Model
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PanasonicIrCodecTest {

    private fun bytes(vararg b: Int) = b

    /** Reads [count] LSB-first bytes of one section starting at [start], checking the timings on the way. */
    private fun decodeSection(pattern: IntArray, start: Int, count: Int): IntArray {
        assertEquals(3456, pattern[start])
        assertEquals(1728, pattern[start + 1])
        val out = IntArray(count)
        for (i in 0 until count * 8) {
            assertEquals(432, pattern[start + 2 + i * 2])
            val bit = when (pattern[start + 3 + i * 2]) {
                1296 -> 1
                432 -> 0
                else -> error("bad space ${pattern[start + 3 + i * 2]} at bit $i")
            }
            out[i / 8] = out[i / 8] or (bit shl (i % 8))
        }
        assertEquals(432, pattern[start + 2 + count * 16])
        return out
    }

    /** An 8-byte section, a 10 ms gap, then the other 19 bytes. */
    private fun decode(pattern: IntArray): IntArray {
        val section1 = 2 + 8 * 16 + 1
        assertEquals(section1 + 1 + 2 + 19 * 16 + 1, pattern.size)
        assertEquals(10_000, pattern[section1])
        return decodeSection(pattern, 0, 8) + decodeSection(pattern, section1 + 1, 19)
    }

    private fun state(model: Model, on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean = false) =
        decode(PanasonicIrCodec.patternFor(model, on, mode, temp, fan, swing))

    @Test
    fun matchesRealJkeCapture() {
        // ir_Panasonic_test.cpp RealExample (issue #525): JKE, power off, Cool 25°, fan auto, vane auto.
        assertArrayEquals(
            bytes(
                0x02, 0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x06, 0x02,
                0x20, 0xE0, 0x04, 0x00, 0x30, 0x32, 0x80, 0xAF, 0x00,
                0x00, 0x06, 0x60, 0x00, 0x00, 0x80, 0x00, 0x06, 0x83
            ),
            state(Model.Jke, false, AcMode.Cool, 25, FanSpeed.Auto, swing = true)
        )
    }

    @Test
    fun matchesRealDkeCaptureApartFromTimerFlags() {
        // ir_Panasonic_test.cpp DkeIonRealMessages (issue #1024): DKE, on, Heat 25°, fan auto, both vanes auto.
        val capture = bytes(
            0x02, 0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x06, 0x02,
            0x20, 0xE0, 0x04, 0x00, 0x4F, 0x32, 0x80, 0xAF, 0x0D,
            0x00, 0x06, 0x60, 0x00, 0x00, 0x01, 0x00, 0x06, 0x30
        )
        assertEquals(0x30, PanasonicIrCodec.checksum(capture))
        val s = state(Model.Dke, true, AcMode.Heat, 25, FanSpeed.Auto, swing = true)
        // That remote also sets the on/off timer flags (at the "unset" time) in byte 13; the app leaves them clear.
        assertEquals(0x41, s[13])
        assertArrayEquals(capture.copyOfRange(0, 13), s.copyOfRange(0, 13))
        assertArrayEquals(capture.copyOfRange(14, 26), s.copyOfRange(14, 26))
        assertEquals(PanasonicIrCodec.checksum(s), s[26])
    }

    @Test
    fun fields_areEncodedForEveryState() {
        for (model in Model.entries) for (on in listOf(true, false)) for (swing in listOf(true, false))
            for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
                val s = state(model, on, mode, temp, fan, swing)
                val label = "$model $on $mode $temp $fan $swing"
                assertEquals(label, PanasonicIrCodec.checksum(s), s[26])
                assertEquals(label, if (on) 1 else 0, s[13] and 1)
                val expectedMode = when (mode) {
                    AcMode.Auto -> 0; AcMode.Dry -> 2; AcMode.Cool -> 3; AcMode.Heat -> 4; AcMode.Fan -> 6
                }
                assertEquals(label, expectedMode, s[13] ushr 4)
                assertEquals(label, if (mode == AcMode.Fan) 27 else temp, s[14] ushr 1)
                assertEquals(label, listOf(4, 5, 6, 10)[fan.ordinal], s[16] ushr 4)
                assertEquals(label, if (swing) 0xF else 0x3, s[16] and 0xF)
            }
    }

    @Test
    fun modelBytes_matchLibrarySetModel() {
        fun s(model: Model) = state(model, true, AcMode.Cool, 24, FanSpeed.Auto, swing = true)
        assertEquals(0x0D, s(Model.Dke)[17])
        assertEquals(0x01, s(Model.Dke)[23])
        assertEquals(0x06, s(Model.Nke)[17])
        assertEquals(0x81, s(Model.Nke)[23])
        assertEquals(0x02, s(Model.Lke)[13] and 0x0E)
        assertEquals(0x06, s(Model.Lke)[17])
        assertEquals(0x08, s(Model.Rkr)[13] and 0x0E)
        assertEquals(0x89, s(Model.Rkr)[23])
        // Horizontal swing follows the swing toggle on DKE and RKR only.
        assertEquals(0x06, state(Model.Rkr, true, AcMode.Cool, 24, FanSpeed.Auto, swing = false)[17])
        assertEquals(0x06, state(Model.Nke, true, AcMode.Cool, 24, FanSpeed.Auto, swing = false)[17])
    }

    @Test
    fun temperature_isClampedTo16to30() {
        assertArrayEquals(
            state(Model.Dke, true, AcMode.Cool, 16, FanSpeed.Low),
            state(Model.Dke, true, AcMode.Cool, 10, FanSpeed.Low)
        )
        assertArrayEquals(
            state(Model.Dke, true, AcMode.Cool, 30, FanSpeed.Low),
            state(Model.Dke, true, AcMode.Cool, 35, FanSpeed.Low)
        )
    }

    @Test
    fun powerProbes_areOnThenOffPerModel() {
        val probes = PanasonicIrCodec.powerProbeVariants()
        assertEquals(Model.entries.size * 2, probes.size)
        Model.entries.forEachIndexed { i, model ->
            assertArrayEquals(PanasonicIrCodec.patternFor(model, true, AcMode.Cool, 24, FanSpeed.Auto, false), probes[i * 2])
            assertArrayEquals(PanasonicIrCodec.patternFor(model, false, AcMode.Cool, 24, FanSpeed.Auto, false), probes[i * 2 + 1])
            assertEquals(model, PanasonicIrCodec.modelFor(i * 2))
            assertEquals(model, PanasonicIrCodec.modelFor(i * 2 + 1))
        }
        assertEquals(Model.Dke, PanasonicIrCodec.modelFor(-1))
        assertEquals(Model.Dke, PanasonicIrCodec.modelFor(10))
    }
}

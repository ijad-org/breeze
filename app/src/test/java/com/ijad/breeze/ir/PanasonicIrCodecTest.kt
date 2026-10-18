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

    private fun state(
        model: Model, on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean = false, powerChanged: Boolean = true
    ) = decode(PanasonicIrCodec.patternFor(model, on, mode, temp, fan, swing, powerChanged))

    /** The 27-byte models, i.e. everything but AC32. */
    private val models216 = Model.entries - Model.Ac32

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
        for (model in models216) for (on in listOf(true, false)) for (swing in listOf(true, false))
            for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
                val s = state(model, on, mode, temp, fan, swing)
                val label = "$model $on $mode $temp $fan $swing"
                assertEquals(label, PanasonicIrCodec.checksum(s), s[26])
                // CKP's power bit is a toggle; powerChanged defaults to true here.
                assertEquals(label, if (on || model == Model.Ckp) 1 else 0, s[13] and 1)
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
        assertEquals(14, probes.size)
        Model.entries.forEachIndexed { i, model ->
            if (PanasonicIrCodec.togglesPower(model)) {
                assertArrayEquals(PanasonicIrCodec.patternFor(model, true, AcMode.Cool, 24, FanSpeed.Auto, false, powerChanged = true), probes[i * 2])
                assertArrayEquals(PanasonicIrCodec.patternFor(model, true, AcMode.Cool, 24, FanSpeed.Auto, false, powerChanged = false), probes[i * 2 + 1])
            } else {
                assertArrayEquals(PanasonicIrCodec.patternFor(model, true, AcMode.Cool, 24, FanSpeed.Auto, false), probes[i * 2])
                assertArrayEquals(PanasonicIrCodec.patternFor(model, false, AcMode.Cool, 24, FanSpeed.Auto, false), probes[i * 2 + 1])
            }
            assertEquals(model, PanasonicIrCodec.modelFor(i * 2))
            assertEquals(model, PanasonicIrCodec.modelFor(i * 2 + 1))
        }
        assertEquals(Model.Dke, PanasonicIrCodec.modelFor(-1))
        assertEquals(Model.Dke, PanasonicIrCodec.modelFor(14))
    }

    @Test
    fun ckp_powerBitIsAToggle() {
        // The power bit is set only when power changes, whichever way.
        assertEquals(1, state(Model.Ckp, true, AcMode.Cool, 24, FanSpeed.Auto, powerChanged = true)[13] and 1)
        assertEquals(1, state(Model.Ckp, false, AcMode.Cool, 24, FanSpeed.Auto, powerChanged = true)[13] and 1)
        assertEquals(0, state(Model.Ckp, true, AcMode.Cool, 24, FanSpeed.Auto, powerChanged = false)[13] and 1)
        // Non-toggling models ignore powerChanged.
        assertArrayEquals(
            state(Model.Dke, true, AcMode.Cool, 24, FanSpeed.Auto, powerChanged = true),
            state(Model.Dke, true, AcMode.Cool, 24, FanSpeed.Auto, powerChanged = false)
        )
    }

    @Test
    fun ckp_matchesRealCaptureSettings() {
        // ir_Panasonic_test.cpp CkpModelSpecifics (issue #544): CKP, no toggle, Heat 23°, fan auto, vane auto, powerful on.
        val capture = bytes(
            0x02, 0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x06, 0x02,
            0x20, 0xE0, 0x04, 0x00, 0x4E, 0x2E, 0x80, 0xAF, 0x00,
            0x00, 0x0E, 0xE0, 0x11, 0x00, 0x01, 0x00, 0x06, 0xB7
        )
        assertEquals(0xB7, PanasonicIrCodec.checksum(capture))
        val s = state(Model.Ckp, true, AcMode.Heat, 23, FanSpeed.Auto, swing = true, powerChanged = false)
        // Mode, no toggle, temp, fan/vane, no horizontal vane and the model bytes match. That remote also had
        // its timer flags set (13, 19, 20) and Powerful on (21 bit 0); the app leaves both off.
        assertEquals(capture[13] and 0xF1, s[13])
        for (i in listOf(14, 15, 16, 17, 23, 25)) assertEquals("byte $i", capture[i], s[i])
        assertEquals(0x10, s[21])
        assertEquals(PanasonicIrCodec.checksum(s), s[26])
    }

    /** Parses IRremoteESP8266's `outputStr()` format ("m3543s3450m920s828…") into a pattern. */
    private fun timings(text: String): IntArray =
        Regex("[ms](\\d+)").findAll(text).map { it.groupValues[1].toInt() }.toList().toIntArray()

    @Test
    fun ac32_senderMatchesLibrarySyntheticMessage() {
        // ir_Panasonic_test.cpp DecodePanasonicAC32 SyntheticMessage: sendPanasonicAC32(0x0DF236FC).
        val sectionA = "m3543s3450" +
            "m920s828m920s2575m920s828m920s828m920s2575m920s2575m920s2575m920s2575" +
            "m920s828m920s2575m920s828m920s828m920s2575m920s2575m920s2575m920s2575" +
            "m920s2575m920s828m920s2575m920s2575m920s828m920s828m920s828m920s828" +
            "m920s2575m920s828m920s2575m920s2575m920s828m920s828m920s828m920s828"
        val sectionB = "m3543s3450" +
            "m920s828m920s828m920s2575m920s2575m920s2575m920s2575m920s2575m920s2575" +
            "m920s828m920s828m920s2575m920s2575m920s2575m920s2575m920s2575m920s2575" +
            "m920s828m920s2575m920s2575m920s828m920s2575m920s2575m920s828m920s828" +
            "m920s828m920s2575m920s2575m920s828m920s2575m920s2575m920s828m920s828"
        val footer = "m3543s3450m920s13946"
        val expected = timings(sectionA + sectionA + footer + sectionB + sectionB + footer)
        // The app's pattern ends on the last mark, without the trailing gap.
        assertArrayEquals(expected.copyOf(expected.size - 1), PanasonicIrCodec.ac32Pattern(0x0DF236FCL))
    }

    @Test
    fun ac32_matchesRealCaptureApartFromAutoModeValue() {
        // ir_Panasonic_test.cpp DecodePanasonicAC32 RealMessage (issue #1307): 17°, auto mode, both swings auto,
        // fan auto, no power toggle. The remote sent mode 5 for auto; the library (and the app) use 6.
        val capture = 0x0DF236FCL
        val s = PanasonicIrCodec.ac32StateFor(AcMode.Auto, 17, FanSpeed.Auto, swingOn = true, powerChanged = false)
        assertEquals(capture and 0x00FFFFFFL, s and 0x00FFFFFFL)
        assertEquals(0x0E, (s ushr 24).toInt())
        // Known-good state (Cool 16°, auto fan, swings auto, no toggle).
        assertEquals(0x0AF136FCL, PanasonicIrCodec.ac32StateFor(AcMode.Cool, 16, FanSpeed.Auto, true, false))
    }

    @Test
    fun ac32_fieldsAreEncoded() {
        for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries)
            for (swing in listOf(true, false)) for (toggle in listOf(true, false)) {
                val s = PanasonicIrCodec.ac32StateFor(mode, temp, fan, swing, toggle)
                val label = "$mode $temp $fan $swing $toggle"
                val b0 = (s and 0xFF).toInt()
                assertEquals(label, 0x84, b0 and 0x87)
                assertEquals(label, if (swing) 7 else 3, (b0 ushr 4) and 0x7)
                assertEquals(label, if (swing) 1 else 0, (b0 ushr 3) and 1)
                assertEquals(label, 0x36L, (s ushr 8) and 0xFF)
                assertEquals(label, (temp - 15).toLong(), (s ushr 16) and 0xF)
                assertEquals(label, listOf(3, 4, 5, 15)[fan.ordinal].toLong(), (s ushr 20) and 0xF)
                assertEquals(label, listOf(2, 4, 3, 1, 6)[mode.ordinal].toLong(), (s ushr 24) and 0x7)
                assertEquals(label, if (toggle) 0L else 1L, (s ushr 27) and 1)
                assertEquals(label, 0L, s ushr 28)
            }
        // The pattern is the encoded state.
        assertArrayEquals(
            PanasonicIrCodec.ac32Pattern(PanasonicIrCodec.ac32StateFor(AcMode.Heat, 24, FanSpeed.Medium, false, true)),
            PanasonicIrCodec.patternFor(Model.Ac32, true, AcMode.Heat, 24, FanSpeed.Medium, false, powerChanged = true)
        )
    }
}

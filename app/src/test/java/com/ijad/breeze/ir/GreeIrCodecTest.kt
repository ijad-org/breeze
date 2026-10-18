package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.ir.GreeIrCodec.Variant
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GreeIrCodecTest {

    private fun bytes(vararg b: Int) = b

    /** Parses IRremoteESP8266's `outputStr()` format ("m9000s4500…") into a pattern. */
    private fun timings(text: String): IntArray =
        Regex("[ms](\\d+)").findAll(text).map { it.groupValues[1].toInt() }.toList().toIntArray()

    private fun state(v: Variant, on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean = false) =
        GreeIrCodec.stateFor(v, on, mode, temp, fan, swing)

    @Test
    fun senderMatchesLibraryOutput() {
        // ir_Gree_test.cpp SendGreeChars SendData.
        val expected = timings(
            "m9000s4500" +
                "m620s540m620s1600m620s540m620s540m620s1600m620s540m620s540m620s540" +
                "m620s540m620s540m620s1600m620s540m620s1600m620s1600m620s540m620s540" +
                "m620s540m620s1600m620s1600m620s540m620s1600m620s540m620s1600m620s540" +
                "m620s540m620s540m620s540m620s1600m620s1600m620s1600m620s1600m620s540" +
                "m620s540m620s1600m620s540" +
                "m620s19980" +
                "m620s540m620s540m620s540m620s540m620s1600m620s540m620s540m620s1600" +
                "m620s1600m620s1600m620s540m620s1600m620s540m620s1600m620s540m620s1600" +
                "m620s1600m620s540m620s1600m620s1600m620s540m620s540m620s1600m620s1600" +
                "m620s1600m620s1600m620s1600m620s1600m620s540m620s1600m620s1600m620s1600" +
                "m620s19980"
        )
        // The app's pattern ends on the last mark, without the trailing gap.
        assertArrayEquals(
            expected.copyOf(expected.size - 1),
            GreeIrCodec.stateToPattern(bytes(0x12, 0x34, 0x56, 0x78, 0x90, 0xAB, 0xCD, 0xEF))
        )
    }

    @Test
    fun matchesRealIssue814States() {
        // ir_Gree_test.cpp Issue814Power: Cool 23°, fan 1, vane auto, light on.
        assertArrayEquals(
            bytes(0x59, 0x07, 0x20, 0x50, 0x01, 0x20, 0x00, 0xC0),
            state(Variant.Ybofb, true, AcMode.Cool, 23, FanSpeed.Low, swing = true)
        )
        assertArrayEquals(
            bytes(0x59, 0x07, 0x60, 0x50, 0x01, 0x20, 0x00, 0xC0),
            state(Variant.Yaw1f, true, AcMode.Cool, 23, FanSpeed.Low, swing = true)
        )
        val off = bytes(0x51, 0x07, 0x20, 0x50, 0x01, 0x20, 0x00, 0x40)
        assertArrayEquals(off, state(Variant.Yaw1f, false, AcMode.Cool, 23, FanSpeed.Low, swing = true))
        assertArrayEquals(off, state(Variant.Ybofb, false, AcMode.Cool, 23, FanSpeed.Low, swing = true))
    }

    @Test
    fun checksumMatchesRealIssue386Capture() {
        // ir_Gree_test.cpp NormalRealExample.
        assertEquals(0xF, GreeIrCodec.checksum(bytes(0x19, 0x0A, 0x60, 0x50, 0x02, 0x23, 0x00, 0xF0)))
    }

    /** Arduino IRremote printed these 32-bit codes MSB-first; Gree is LSB-first, so each byte is bit-reversed. */
    private fun blueStarCapture(code: Long): IntArray = IntArray(4) { i ->
        val b = ((code ushr (24 - 8 * i)) and 0xFF).toInt()
        Integer.reverse(b) ushr 24
    }

    @Test
    fun blueStarVariantMatchesEveryPublishedBlueStarCapture() {
        // techantidote.com "Decoding IR Signals of a Blue Star Air Conditioner": the first 4 bytes of each frame.
        val temps = listOf(
            0x90000E0AL, 0x90800E0AL, 0x90400E0AL, 0x90C00E0AL, 0x90200E0AL, 0x90A00E0AL, 0x90600E0AL, 0x90E00E0AL,
            0x90100E0AL, 0x90900E0AL, 0x90500E0AL, 0x90D00E0AL, 0x90300E0AL, 0x90B00E0AL, 0x90700E0AL
        )
        temps.forEachIndexed { i, code ->
            assertArrayEquals(
                "${16 + i}°",
                blueStarCapture(code),
                state(Variant.BlueStar, true, AcMode.Cool, 16 + i, FanSpeed.Auto).copyOfRange(0, 4)
            )
        }
        // Power off at 25°.
        assertArrayEquals(
            blueStarCapture(0x80900C0AL),
            state(Variant.BlueStar, false, AcMode.Cool, 25, FanSpeed.Auto).copyOfRange(0, 4)
        )
    }

    @Test
    fun fields_areEncodedForEveryState() {
        for (v in Variant.entries) for (on in listOf(true, false)) for (swing in listOf(true, false))
            for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
                val s = state(v, on, mode, temp, fan, swing)
                val label = "$v $on $mode $temp $fan $swing"
                assertEquals(label, listOf(1, 4, 2, 3, 0)[mode.ordinal], s[0] and 0x7)
                assertEquals(label, if (on) 1 else 0, (s[0] ushr 3) and 1)
                val fanBits = if (mode == AcMode.Dry) 1 else listOf(1, 2, 3, 0)[fan.ordinal]
                assertEquals(label, fanBits, (s[0] ushr 4) and 0x3)
                assertEquals(label, if (swing) 1 else 0, (s[0] ushr 6) and 1)
                assertEquals(label, if (mode == AcMode.Auto) 9 else temp - 16, s[1])
                assertEquals(label, if (on && v != Variant.Ybofb) 1 else 0, (s[2] ushr 6) and 1)
                assertEquals(label, if (v == Variant.BlueStar) 1 else 0, (s[2] ushr 4) and 1)
                assertEquals(label, if (swing) 1 else 0, s[4])
                assertEquals(label, GreeIrCodec.checksum(s), s[7] ushr 4)
            }
    }

    @Test
    fun temperature_isClampedTo16to30() {
        assertArrayEquals(
            state(Variant.Yaw1f, true, AcMode.Cool, 16, FanSpeed.Low),
            state(Variant.Yaw1f, true, AcMode.Cool, 10, FanSpeed.Low)
        )
        assertArrayEquals(
            state(Variant.Yaw1f, true, AcMode.Cool, 30, FanSpeed.Low),
            state(Variant.Yaw1f, true, AcMode.Cool, 35, FanSpeed.Low)
        )
    }
}

package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.ir.DaikinIrCodec.Protocol
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DaikinIrCodecTest {

    /**
     * Reads the state bytes back out of a mark/space pattern, checking the timings on the way.
     * [leader] is the ARC4xx 5-bit zero leader; [sections] are the byte counts per section.
     */
    private fun decode(
        pattern: IntArray,
        leader: Boolean,
        sections: List<Int>,
        hdr: Pair<Int, Int>,
        bitMark: Int,
        one: Int,
        zero: Int,
        gap: Int
    ): IntArray {
        var i = 0
        fun next() = pattern[i++]
        if (leader) {
            repeat(5) { assertEquals(bitMark, next()); assertEquals(zero, next()) }
            assertEquals(bitMark, next()); assertEquals(gap, next())
        }
        val out = ArrayList<Int>()
        sections.forEachIndexed { s, len ->
            assertEquals(hdr.first, next())
            assertEquals(hdr.second, next())
            repeat(len) {
                var byte = 0
                for (bit in 0 until 8) {
                    assertEquals(bitMark, next())
                    when (val space = next()) {
                        one -> byte = byte or (1 shl bit)
                        zero -> Unit
                        else -> error("bad space $space in section $s")
                    }
                }
                out.add(byte)
            }
            assertEquals(bitMark, next())
            if (s < sections.lastIndex) assertEquals(gap, next())
        }
        assertEquals("trailing entries", pattern.size, i)
        return out.toIntArray()
    }

    private fun decodeArc4xx(p: IntArray) =
        decode(p, leader = true, listOf(8, 8, 19), 3650 to 1623, 428, 1280, 428, 29_428)

    private fun decodeArc433b69(p: IntArray) =
        decode(p, leader = false, listOf(8, 19), 3440 to 1750, 420, 1300, 450, 29_650)

    private fun pattern(p: Protocol, on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean) =
        DaikinIrCodec.patternFor(p, on, mode, temp, fan, swing)

    @Test
    fun arc4xx_cool24AutoSwing() {
        val expected = intArrayOf(
            0x11, 0xDA, 0x27, 0x00, 0xC5, 0x00, 0x00, 0xD7,
            0x11, 0xDA, 0x27, 0x00, 0x42, 0x00, 0x00, 0x54,
            0x11, 0xDA, 0x27, 0x00, 0x00, 0x39, 0x30, 0x00, 0xAF, 0x00,
            0x00, 0x06, 0x60, 0x00, 0x00, 0xC0, 0x00, 0x00, 0x50
        )
        assertArrayEquals(expected, decodeArc4xx(pattern(Protocol.Arc4xx, true, AcMode.Cool, 24, FanSpeed.Auto, true)))
    }

    @Test
    fun arc4xx_leaderAndFirstByteMatchIrremoteEsp8266() {
        // From IRremoteESP8266 TestSendDaikin.SendDataOnly: leader, gap, header, then 0x11 LSB-first.
        val p = pattern(Protocol.Arc4xx, true, AcMode.Cool, 24, FanSpeed.Auto, false)
        val expected = intArrayOf(
            428, 428, 428, 428, 428, 428, 428, 428, 428, 428, 428, 29_428,
            3650, 1623,
            428, 1280, 428, 428, 428, 428, 428, 428, 428, 1280, 428, 428, 428, 428, 428, 428
        )
        assertArrayEquals(expected, p.copyOf(expected.size))
        assertEquals(583, p.size)
    }

    @Test
    fun arc433b69_matchesRealRemoteCapture() {
        // IRremoteESP8266 issue #689: ARC433B69 capture, Off / Auto / 19C / fan Auto / swing off.
        val expected = intArrayOf(
            0x11, 0xDA, 0x27, 0xF0, 0x00, 0x00, 0x00, 0x02,
            0x11, 0xDA, 0x27, 0x00, 0x00, 0x00, 0x26, 0x00, 0xA0, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0xC0, 0x00, 0x00, 0x98
        )
        val p = pattern(Protocol.Arc433b69, false, AcMode.Auto, 19, FanSpeed.Auto, false)
        assertArrayEquals(expected, decodeArc433b69(p))
        assertArrayEquals(expected, DaikinIrCodec.stateFor(Protocol.Arc433b69, false, AcMode.Auto, 19, FanSpeed.Auto, false))
    }

    @Test
    fun fieldsLandInTheRightBits() {
        val modes = mapOf(AcMode.Auto to 0, AcMode.Dry to 2, AcMode.Cool to 3, AcMode.Heat to 4, AcMode.Fan to 6)
        val fans = mapOf(FanSpeed.Low to 3, FanSpeed.Medium to 5, FanSpeed.High to 7, FanSpeed.Auto to 0xA)
        for (on in listOf(true, false)) for ((mode, m) in modes) for (temp in 16..30) for ((fan, f) in fans)
            for (swing in listOf(true, false)) {
                val a = decodeArc4xx(pattern(Protocol.Arc4xx, on, mode, temp, fan, swing))
                assertEquals(if (on) 1 else 0, a[21] and 1)
                assertEquals(0x08, a[21] and 0x0E)
                assertEquals(m, (a[21] ushr 4) and 0x7)
                assertEquals(temp * 2, a[22])
                assertEquals(f, a[24] ushr 4)
                assertEquals(if (swing) 0xF else 0, a[24] and 0xF)
                assertChecksums(a, listOf(0 until 8, 8 until 16, 16 until 35))

                val b = decodeArc433b69(pattern(Protocol.Arc433b69, on, mode, temp, fan, swing))
                assertEquals(if (on) 1 else 0, b[13] and 1)
                assertEquals(m, (b[13] ushr 4) and 0x7)
                assertEquals(temp, (b[14] ushr 1) and 0x3F)
                assertEquals(f, b[16] ushr 4)
                assertEquals(if (swing) 0xF else 0, b[16] and 0xF)
                assertChecksums(b, listOf(0 until 8, 8 until 27))
            }
    }

    private fun assertChecksums(state: IntArray, sections: List<IntRange>) {
        for (r in sections) assertEquals("checksum $r", (r.first until r.last).sumOf { state[it] } and 0xFF, state[r.last])
    }

    @Test
    fun temperature_isClampedTo10to32() {
        assertEquals(20, DaikinIrCodec.stateFor(Protocol.Arc4xx, true, AcMode.Heat, 5, FanSpeed.Low, false)[22])
        assertEquals(64, DaikinIrCodec.stateFor(Protocol.Arc4xx, true, AcMode.Cool, 40, FanSpeed.Low, false)[22])
        assertEquals(32 shl 1, DaikinIrCodec.stateFor(Protocol.Arc433b69, true, AcMode.Cool, 40, FanSpeed.Low, false)[14])
    }

    @Test
    fun probes_areOnThenOffPerProtocol_andMapBackToProtocol() {
        val probes = DaikinIrCodec.powerProbeVariants()
        assertEquals(4, probes.size)
        assertEquals(1, decodeArc4xx(probes[0])[21] and 1)
        assertEquals(0, decodeArc4xx(probes[1])[21] and 1)
        assertEquals(1, decodeArc433b69(probes[2])[13] and 1)
        assertEquals(0, decodeArc433b69(probes[3])[13] and 1)
        assertEquals(listOf(Protocol.Arc4xx, Protocol.Arc4xx, Protocol.Arc433b69, Protocol.Arc433b69), (0..3).map(DaikinIrCodec::protocolFor))
        // Older Daikin pairings stored placeholder indices up to 11.
        assertEquals(Protocol.Arc4xx, DaikinIrCodec.protocolFor(11))
        assertEquals(Protocol.Arc4xx, DaikinIrCodec.protocolFor(-1))
    }

    @Test
    fun patternsFitConsumerIrLimit() {
        // ConsumerIrManager rejects patterns longer than 2 s.
        for (p in DaikinIrCodec.powerProbeVariants()) assertTrue(p.sum() < 2_000_000)
    }
}

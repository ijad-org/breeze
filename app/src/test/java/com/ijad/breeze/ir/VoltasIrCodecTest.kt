package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VoltasIrCodecTest {

    private fun bytes(vararg b: Int) = b

    /** Reads the 10-byte state back out of an MSB-first mark/space pattern, checking the timings on the way. */
    private fun decode(pattern: IntArray): IntArray {
        assertEquals(VoltasIrCodec.STATE_LENGTH * 16 + 1, pattern.size)
        assertEquals(1026, pattern.last())
        val state = IntArray(VoltasIrCodec.STATE_LENGTH)
        for (i in 0 until VoltasIrCodec.STATE_LENGTH * 8) {
            assertEquals(1026, pattern[i * 2])
            val bit = when (pattern[i * 2 + 1]) {
                2553 -> 1
                554 -> 0
                else -> error("bad space ${pattern[i * 2 + 1]} at bit $i")
            }
            state[i / 8] = (state[i / 8] shl 1) or bit
        }
        return state
    }

    private fun state(on: Boolean, mode: AcMode, temp: Int, fan: FanSpeed, swing: Boolean = false) =
        decode(VoltasIrCodec.patternFor(on, mode, temp, fan, swing))

    @Test
    fun knownStateFromIrremoteEsp8266() {
        // stateReset(): Cool 23° high fan, swing and power off.
        assertArrayEquals(
            bytes(0x33, 0x28, 0x00, 0x17, 0x3B, 0x3B, 0x3B, 0x11, 0x00, 0xCB),
            state(false, AcMode.Cool, 23, FanSpeed.High)
        )
    }

    @Test
    fun matchesRealCaptureApartFromWifiBit() {
        // ir_Voltas_test.cpp real capture: power on, Dry 24° low fan, swing off, WiFi on.
        val real = bytes(0x33, 0x84, 0x88, 0x18, 0x3B, 0x3B, 0x3B, 0x11, 0x00, 0xE6)
        assertEquals(0xE6, VoltasIrCodec.checksum(real))
        // The app never sets the remote's WiFi bit (byte 2, 0x08); everything else matches.
        val withoutWifi = real.copyOf().also { it[2] = it[2] and 0x08.inv(); it[9] = VoltasIrCodec.checksum(it) }
        assertArrayEquals(withoutWifi, state(true, AcMode.Dry, 24, FanSpeed.Low))
    }

    @Test
    fun fields_areEncodedForEveryState() {
        for (on in listOf(true, false)) for (swing in listOf(true, false))
            for (mode in AcMode.entries) for (temp in 16..30) for (fan in FanSpeed.entries) {
                val s = state(on, mode, temp, fan, swing)
                val label = "$on $mode $temp $fan $swing"
                assertEquals(label, VoltasIrCodec.checksum(s), s[9])
                assertEquals(label, if (on) 1 else 0, s[2] ushr 7)
                assertEquals(label, if (swing) 0b111 else 0, s[2] and 0b111)
                val expectedMode = when (mode) {
                    AcMode.Fan -> 1; AcMode.Heat -> 2; AcMode.Dry -> 4; AcMode.Cool, AcMode.Auto -> 8
                }
                assertEquals(label, expectedMode, s[1] and 0xF)
                val expectedFan = when {
                    mode == AcMode.Dry -> 0b100
                    mode == AcMode.Fan && fan == FanSpeed.Auto -> 0b001
                    else -> listOf(0b100, 0b010, 0b001, 0b111)[fan.ordinal]
                }
                assertEquals(label, expectedFan, s[1] ushr 5)
                assertEquals(label, (if (mode == AcMode.Dry) 24 else temp) - 16, s[3] and 0xF)
                assertEquals(label, 0x10, s[3] and 0xF0)
            }
    }

    @Test
    fun autoMode_isSentAsCool() {
        assertArrayEquals(state(true, AcMode.Cool, 25, FanSpeed.Medium), state(true, AcMode.Auto, 25, FanSpeed.Medium))
    }

    @Test
    fun temperature_isClampedTo16to30() {
        assertArrayEquals(state(true, AcMode.Cool, 16, FanSpeed.Low), state(true, AcMode.Cool, 10, FanSpeed.Low))
        assertArrayEquals(state(true, AcMode.Cool, 30, FanSpeed.Low), state(true, AcMode.Cool, 35, FanSpeed.Low))
    }

    @Test
    fun powerProbes_startWithCool24AutoThenOff() {
        val probes = VoltasIrCodec.powerProbeVariants()
        assertEquals(5, probes.size)
        assertArrayEquals(VoltasIrCodec.patternFor(true, AcMode.Cool, 24, FanSpeed.Auto, false), probes[0])
        assertArrayEquals(VoltasIrCodec.patternFor(false, AcMode.Cool, 24, FanSpeed.Auto, false), probes[1])
    }
}

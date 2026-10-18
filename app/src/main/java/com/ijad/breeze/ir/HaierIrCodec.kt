package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Haier AC IR encoder for its three common full-state protocols.
 *
 * Constants, byte layouts and reset states from crankyoldgit/IRremoteESP8266
 * `ir_Haier.{h,cpp}` (`HAIER_AC_YRW02`, `HAIER_AC176`, `HAIER_AC`), with the
 * settings the library's `IRac` sends (health, sleep, turbo, quiet and timers off).
 *
 * Bytes are sent MSB-first at 38 kHz after a double header. Every frame carries
 * the whole state; it also names the button that was pressed, which is Power
 * for control frames and Swing for swing frames.
 */
object HaierIrCodec {
    const val FREQUENCY_HZ = 38_000

    enum class Protocol {
        /** `HAIER_AC_YRW02` 112-bit (YR-W02 remote, HSU-09HMC203). */
        Yrw02,

        /** `HAIER_AC176` 176-bit (V9014557 remote; also Mabe and Daichi). */
        Ac176,

        /** `HAIER_AC` 72-bit (HSU07-HEA03 remote). */
        Hsu07
    }

    private const val HDR = 3000
    private const val HDR_GAP = 4300
    private const val BIT_MARK = 520
    private const val ONE_SPACE = 1650
    private const val ZERO_SPACE = 650

    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 30

    private const val YRW02_LENGTH = 14
    private const val AC176_LENGTH = 22
    private const val HSU07_LENGTH = 9

    private const val YRW02_MODEL_A = 0xA6
    private const val AC176_PREFIX = 0xB7
    private const val YRW02_SWING_V_OFF = 0x0
    private const val YRW02_SWING_V_AUTO = 0xC
    private const val YRW02_FAN_AUTO = 0b101
    private const val YRW02_BUTTON_SWING_V = 0b00010
    private const val YRW02_BUTTON_POWER = 0b00101

    private const val HSU07_PREFIX = 0xA5
    private const val HSU07_CMD_OFF = 0b0000
    private const val HSU07_CMD_ON = 0b0001
    private const val HSU07_CMD_SWING = 0b1101
    private const val HSU07_SWING_V_OFF = 0b00
    private const val HSU07_SWING_V_CHANGE = 0b11
    /** IRremoteESP8266 `stateReset()` default off-timer hours, kept in real captures (#668). */
    private const val HSU07_OFF_HOURS = 12

    /**
     * Pairing probes, two per protocol: Cool 24° Auto ON, then OFF.
     * The chosen probe index is the device's `configIndex`; see [protocolFor].
     */
    fun powerProbeVariants(): List<IntArray> = Protocol.entries.flatMap { p ->
        listOf(
            patternFor(p, true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
            patternFor(p, false, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false)
        )
    }

    /** Maps a paired probe index to its protocol. Unknown indices (e.g. older placeholder pairings) use [Protocol.Yrw02]. */
    fun protocolFor(configIndex: Int): Protocol =
        Protocol.entries.getOrNull(configIndex / 2)?.takeIf { configIndex >= 0 } ?: Protocol.Yrw02

    fun patternFor(
        protocol: Protocol,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray = stateToPattern(stateFor(protocol, poweredOn, mode, temperatureC, fan, swingOn, swingPress = false))

    /** Same state as [patternFor], sent as a press of the swing button. */
    fun swingPatternFor(
        protocol: Protocol,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray = stateToPattern(stateFor(protocol, poweredOn, mode, temperatureC, fan, swingOn, swingPress = true))

    /** The raw state bytes (0–255) with checksums filled in. */
    internal fun stateFor(
        protocol: Protocol,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean,
        swingPress: Boolean = false
    ): IntArray {
        val temp = temperatureC.coerceIn(MIN_TEMP, MAX_TEMP) - MIN_TEMP
        return when (protocol) {
            Protocol.Yrw02, Protocol.Ac176 -> {
                val length = if (protocol == Protocol.Yrw02) YRW02_LENGTH else AC176_LENGTH
                val fanBits = yrw02FanBits(fan)
                IntArray(length).also { s ->
                    s[0] = YRW02_MODEL_A
                    s[1] = (temp shl 4) or (if (swingOn) YRW02_SWING_V_AUTO else YRW02_SWING_V_OFF)
                    s[4] = if (poweredOn) 0x40 else 0
                    s[5] = fanBits shl 5
                    s[7] = yrw02ModeBits(mode) shl 5
                    s[12] = if (swingPress) YRW02_BUTTON_SWING_V else YRW02_BUTTON_POWER
                    s[13] = sum(s, 0, YRW02_LENGTH - 1)
                    if (protocol == Protocol.Ac176) {
                        s[14] = AC176_PREFIX
                        // Fan2 repeats the fan speed, with 0 for Auto.
                        s[16] = (if (fan == FanSpeed.Auto) 0 else fanBits) shl 6
                        s[AC176_LENGTH - 1] = sum(s, YRW02_LENGTH, AC176_LENGTH - 1)
                    }
                }
            }
            Protocol.Hsu07 -> IntArray(HSU07_LENGTH).also { s ->
                val command = when {
                    !poweredOn -> HSU07_CMD_OFF
                    swingPress -> HSU07_CMD_SWING
                    else -> HSU07_CMD_ON
                }
                s[0] = HSU07_PREFIX
                s[1] = (temp shl 4) or command
                s[2] = 0x20 or ((if (swingOn) HSU07_SWING_V_CHANGE else HSU07_SWING_V_OFF) shl 6)
                s[4] = HSU07_OFF_HOURS
                s[5] = hsu07FanBits(fan) shl 6
                s[6] = hsu07ModeBits(mode) shl 5
                s[HSU07_LENGTH - 1] = sum(s, 0, HSU07_LENGTH - 1)
            }
        }
    }

    private fun yrw02ModeBits(mode: AcMode): Int = when (mode) {
        AcMode.Auto -> 0b000
        AcMode.Cool -> 0b001
        AcMode.Dry -> 0b010
        AcMode.Heat -> 0b100
        AcMode.Fan -> 0b110
    }

    private fun yrw02FanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.High -> 0b001
        FanSpeed.Medium -> 0b010
        FanSpeed.Low -> 0b011
        FanSpeed.Auto -> YRW02_FAN_AUTO
    }

    private fun hsu07ModeBits(mode: AcMode): Int = when (mode) {
        AcMode.Auto -> 0
        AcMode.Cool -> 1
        AcMode.Dry -> 2
        AcMode.Heat -> 3
        AcMode.Fan -> 4
    }

    /** Raw fan field: IRremoteESP8266 `setFan()` stores High as 1 and Low as 3. */
    private fun hsu07FanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Auto -> 0
        FanSpeed.High -> 1
        FanSpeed.Medium -> 2
        FanSpeed.Low -> 3
    }

    /** Sum of bytes [from] until [until], low 8 bits. */
    internal fun sum(state: IntArray, from: Int, until: Int): Int {
        var sum = 0
        for (i in from until until) sum += state[i]
        return sum and 0xFF
    }

    private fun stateToPattern(state: IntArray): IntArray {
        val out = ArrayList<Int>(4 + state.size * 16 + 1)
        out.add(HDR)
        out.add(HDR)
        out.add(HDR)
        out.add(HDR_GAP)
        for (byte in state) {
            for (bit in 7 downTo 0) {
                out.add(BIT_MARK)
                out.add(if ((byte ushr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE)
            }
        }
        out.add(BIT_MARK)
        return out.toIntArray()
    }
}

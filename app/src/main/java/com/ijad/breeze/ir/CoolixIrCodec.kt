package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Coolix 24-bit AC IR encoder (COOLIX), used by Midea-built units sold under many
 * names (the library lists Midea, Comfee, Beko, Bosch, Airwell, Tokio, Kaysun and
 * some Toshiba). A Blue Star remote's capture (IRremoteESP8266 #498) is the Coolix
 * off frame, and many Midea-built Carrier units use it too.
 *
 * Protocol constants and state layout from crankyoldgit/IRremoteESP8266
 * `ir_Coolix.{h,cpp}` (`sendCOOLIX`, `IRCoolixAC`).
 *
 * 3 bytes, MSB-first, 38 kHz, each byte followed by its inverse, and the whole
 * frame sent twice. A state frame carries mode, temp and fan and turns the unit
 * on; off is its own fixed frame. Swing is a toggle command, sent on a swing
 * press. Fan mode is Dry with a special temperature code, like the remote.
 */
object CoolixIrCodec {
    const val FREQUENCY_HZ = 38_000

    private const val TICK = 276
    private const val BIT_MARK = 2 * TICK
    private const val ONE_SPACE = 6 * TICK
    private const val ZERO_SPACE = 2 * TICK
    private const val HDR_MARK = 17 * TICK
    private const val HDR_SPACE = 16 * TICK
    private const val MIN_GAP = (17 + 2) * TICK
    private const val SENDS = 2

    private const val MIN_TEMP = 17
    private const val MAX_TEMP = 30

    const val OFF = 0xB27BE0
    const val SWING_TOGGLE = 0xB26BE0

    /** Fixed top byte (0xB, then 0b0010) and "no sensor temperature" (0x1F). */
    private const val BASE = 0xB20000
    private const val SENSOR_TEMP_IGNORE = 0x1F
    private const val FAN_MODE_TEMP_CODE = 0b1110

    /** Gray-code-like temperature codes for 17–30°, from `kCoolixTempMap`. */
    private val TEMP_CODES = intArrayOf(
        0b0000, 0b0001, 0b0011, 0b0010, 0b0110, 0b0111, 0b0101,
        0b0100, 0b1100, 0b1101, 0b1001, 0b1000, 0b1010, 0b1011
    )

    fun patternFor(poweredOn: Boolean, mode: AcMode, temperatureC: Int, fan: FanSpeed): IntArray =
        frameToPattern(stateFor(poweredOn, mode, temperatureC, fan))

    fun swingPattern(): IntArray = frameToPattern(SWING_TOGGLE)

    /** Pairing probes: Cool 24° Auto ON, then OFF. */
    fun powerProbeVariants(): List<IntArray> = listOf(
        patternFor(true, AcMode.Cool, 24, FanSpeed.Auto),
        patternFor(false, AcMode.Cool, 24, FanSpeed.Auto)
    )

    /** The 24-bit frame for a state, or [OFF]. */
    internal fun stateFor(poweredOn: Boolean, mode: AcMode, temperatureC: Int, fan: FanSpeed): Int {
        if (!poweredOn) return OFF
        val tempCode = if (mode == AcMode.Fan) FAN_MODE_TEMP_CODE
        else TEMP_CODES[temperatureC.coerceIn(MIN_TEMP, MAX_TEMP) - MIN_TEMP]
        return BASE or (fanBits(mode, fan) shl 13) or (SENSOR_TEMP_IGNORE shl 8) or (tempCode shl 4) or (modeBits(mode) shl 2)
    }

    /** Fan mode is sent as Dry (with [FAN_MODE_TEMP_CODE]). */
    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Cool -> 0b00
        AcMode.Dry, AcMode.Fan -> 0b01
        AcMode.Auto -> 0b10
        AcMode.Heat -> 0b11
    }

    /** Auto is 0b101, except in Auto and Dry modes, which use 0b000 (`setMode()`). */
    private fun fanBits(mode: AcMode, fan: FanSpeed): Int = when (fan) {
        FanSpeed.Low -> 0b100
        FanSpeed.Medium -> 0b010
        FanSpeed.High -> 0b001
        FanSpeed.Auto -> if (mode == AcMode.Auto || mode == AcMode.Dry) 0b000 else 0b101
    }

    internal fun frameToPattern(frame: Int): IntArray {
        val out = ArrayList<Int>(SENDS * (2 + 48 * 2 + 2))
        repeat(SENDS) { send ->
            if (send > 0) out.add(MIN_GAP)
            out.add(HDR_MARK)
            out.add(HDR_SPACE)
            for (shift in intArrayOf(16, 8, 0)) {
                val byte = (frame ushr shift) and 0xFF
                for (value in intArrayOf(byte, byte xor 0xFF)) {
                    for (bit in 7 downTo 0) {
                        out.add(BIT_MARK)
                        out.add(if ((value ushr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE)
                    }
                }
            }
            out.add(BIT_MARK)
        }
        return out.toIntArray()
    }
}

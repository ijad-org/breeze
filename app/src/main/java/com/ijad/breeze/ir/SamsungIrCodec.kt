package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Samsung AC IR encoder (AR-series remotes, e.g. AR09FSSDAWKNFA).
 *
 * Protocol constants and state layout from crankyoldgit/IRremoteESP8266
 * `ir_Samsung.{h,cpp}` (`sendSamsungAC`, `IRSamsungAc`).
 *
 * The state is 7-byte sections sent LSB-first at 38 kHz. A normal message has
 * two sections; the unit only turns on or off on the 21-byte "extended" form,
 * whose third section carries the settings. The app doesn't know what the unit
 * last received, so every frame is extended: it powers on (or off) and sets
 * mode, temperature, fan and swing in one go.
 */
object SamsungIrCodec {
    const val FREQUENCY_HZ = 38_000

    private const val SECTION_LENGTH = 7
    const val EXTENDED_LENGTH = 21

    private const val HDR_MARK = 690
    private const val HDR_SPACE = 17844
    private const val SECTION_MARK = 3086
    private const val SECTION_SPACE = 8864
    private const val SECTION_GAP = 2886
    private const val BIT_MARK = 586
    private const val ONE_SPACE = 1432
    private const val ZERO_SPACE = 436

    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 30

    private const val POWER_ON = 0b11
    private const val SWING_VERTICAL = 0b010
    private const val SWING_OFF = 0b111
    private const val FAN_AUTO_IN_AUTO_MODE = 6

    fun patternFor(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray = stateToPattern(stateFor(poweredOn, mode, temperatureC, fan, swingOn))

    /**
     * Pairing probes, same order as LG: Cool 24° Auto ON, then OFF, Cool 22, Heat 24, Fan.
     */
    fun powerProbeVariants(): List<IntArray> = listOf(
        patternFor(true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
        patternFor(false, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
        patternFor(true, AcMode.Cool, 22, FanSpeed.Auto, swingOn = false),
        patternFor(true, AcMode.Heat, 24, FanSpeed.Auto, swingOn = false),
        patternFor(true, AcMode.Fan, 24, FanSpeed.Auto, swingOn = false)
    )

    /** The 21-byte extended state, with checksums. */
    internal fun stateFor(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray {
        val power = if (poweredOn) POWER_ON else 0
        val fanBits = if (mode == AcMode.Auto) FAN_AUTO_IN_AUTO_MODE else fanBits(fan)
        val temp = temperatureC.coerceIn(MIN_TEMP, MAX_TEMP) - MIN_TEMP
        val swing = if (swingOn) SWING_VERTICAL else SWING_OFF
        val state = intArrayOf(
            // Section 1: power in bits 4-5 of byte 6.
            0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0xC0 or (power shl 4),
            // Section 2: fixed in extended messages.
            0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
            // Section 3: the settings.
            0x01,
            0x02,
            0x80 or (swing shl 4),
            0x71, // Display on, no special fan, no clean toggle.
            temp shl 4,
            0x01 or (fanBits shl 1) or (modeBits(mode) shl 4),
            0xC0 or (power shl 4)
        )
        for (offset in 0 until EXTENDED_LENGTH step SECTION_LENGTH) setChecksum(state, offset)
        return state
    }

    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Auto -> 0
        AcMode.Cool -> 1
        AcMode.Dry -> 2
        AcMode.Fan -> 3
        AcMode.Heat -> 4
    }

    private fun fanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Auto -> 0
        FanSpeed.Low -> 2
        FanSpeed.Medium -> 4
        FanSpeed.High -> 5
    }

    /**
     * Inverted count of set bits in byte 0, the low nibble of byte 1, the high
     * nibble of byte 2 and bytes 3-6. Its low nibble goes in the high nibble of
     * byte 1, its high nibble in the low nibble of byte 2.
     */
    internal fun sectionChecksum(state: IntArray, offset: Int): Int {
        var bits = Integer.bitCount(state[offset])
        bits += Integer.bitCount(state[offset + 1] and 0x0F)
        bits += Integer.bitCount(state[offset + 2] and 0xF0)
        for (i in 3 until SECTION_LENGTH) bits += Integer.bitCount(state[offset + i])
        return bits.inv() and 0xFF
    }

    private fun setChecksum(state: IntArray, offset: Int) {
        val sum = sectionChecksum(state, offset)
        state[offset + 1] = (state[offset + 1] and 0x0F) or ((sum and 0x0F) shl 4)
        state[offset + 2] = (state[offset + 2] and 0xF0) or (sum ushr 4)
    }

    private fun stateToPattern(state: IntArray): IntArray {
        val sections = state.size / SECTION_LENGTH
        val out = ArrayList<Int>(2 + sections * (2 + SECTION_LENGTH * 16 + 2))
        out.add(HDR_MARK)
        out.add(HDR_SPACE)
        for (s in 0 until sections) {
            if (s > 0) out.add(SECTION_GAP)
            out.add(SECTION_MARK)
            out.add(SECTION_SPACE)
            for (i in 0 until SECTION_LENGTH) {
                val byte = state[s * SECTION_LENGTH + i]
                for (bit in 0 until 8) {
                    out.add(BIT_MARK)
                    out.add(if ((byte ushr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE)
                }
            }
            out.add(BIT_MARK)
        }
        return out.toIntArray()
    }
}

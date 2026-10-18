package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Gree 64-bit AC IR encoder (GREE), also used by many OEM units, including Blue Star.
 *
 * Protocol constants and state layout from crankyoldgit/IRremoteESP8266
 * `ir_Gree.{h,cpp}` (`sendGree`, `IRGreeAC`), with the settings the library's
 * `IRac` sends (light on; turbo, sleep, X-fan and timers off).
 *
 * 8 bytes, LSB-first, 38 kHz, full state per frame: 4 bytes, a 3-bit `010`
 * footer and a 20 ms gap, then the other 4 bytes. Auto mode always sends 25°
 * and Dry always sends low fan, like the library.
 */
object GreeIrCodec {
    const val FREQUENCY_HZ = 38_000

    const val STATE_LENGTH = 8

    private const val HDR_MARK = 9000
    private const val HDR_SPACE = 4500
    private const val BIT_MARK = 620
    private const val ONE_SPACE = 1600
    private const val ZERO_SPACE = 540
    private const val MSG_SPACE = 19_980
    private const val BLOCK_FOOTER = 0b010
    private const val BLOCK_FOOTER_BITS = 3

    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 30
    private const val AUTO_TEMP = 25

    private const val POWER = 0x08
    private const val SWING_AUTO = 0x40
    private const val LIGHT = 0x20
    private const val MODEL_A = 0x40
    private const val TURBO_BIT = 0x10
    private const val SWING_V_LAST_POS = 0x0
    private const val SWING_V_AUTO = 0x1
    private const val CHECKSUM_START = 10

    enum class Variant {
        /** YAW1F remotes: the "model A" bit is set while powered on. */
        Yaw1f,

        /** YBOFB remotes: no model bit. */
        Ybofb,

        /**
         * YAW1F as a Blue Star remote sends it. Every published Blue Star capture
         * (techantidote.com: power on/off and 16–30°) also has byte 2 bit 4 set,
         * which IRremoteESP8266 calls Turbo, so this variant sets it too.
         */
        BlueStar
    }

    fun patternFor(
        variant: Variant,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray = stateToPattern(stateFor(variant, poweredOn, mode, temperatureC, fan, swingOn))

    /** Pairing probes for one variant: Cool 24° Auto ON, then OFF. */
    fun powerProbeVariants(variant: Variant): List<IntArray> = listOf(
        patternFor(variant, true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
        patternFor(variant, false, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false)
    )

    /** The 8-byte state with its checksum nibble. */
    internal fun stateFor(
        variant: Variant,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray {
        val temp = if (mode == AcMode.Auto) AUTO_TEMP else temperatureC.coerceIn(MIN_TEMP, MAX_TEMP)
        val fanBits = if (mode == AcMode.Dry) 1 else fanBits(fan)
        val state = IntArray(STATE_LENGTH)
        state[0] = modeBits(mode) or (if (poweredOn) POWER else 0) or (fanBits shl 4) or
            (if (swingOn) SWING_AUTO else 0)
        state[1] = temp - MIN_TEMP
        val modelA = poweredOn && variant != Variant.Ybofb
        state[2] = LIGHT or (if (modelA) MODEL_A else 0) or (if (variant == Variant.BlueStar) TURBO_BIT else 0)
        state[3] = 0x50
        state[4] = if (swingOn) SWING_V_AUTO else SWING_V_LAST_POS
        state[5] = 0x20
        state[7] = checksum(state) shl 4
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
        FanSpeed.Low -> 1
        FanSpeed.Medium -> 2
        FanSpeed.High -> 3
    }

    /**
     * Kelvinator's block checksum: 10, plus the low nibbles of bytes 0–3 and the high
     * nibbles of bytes 4–6, mod 16.
     */
    internal fun checksum(state: IntArray): Int {
        var sum = CHECKSUM_START
        for (i in 0 until 4) sum += state[i] and 0xF
        for (i in 4 until STATE_LENGTH - 1) sum += state[i] ushr 4
        return sum and 0xF
    }

    internal fun stateToPattern(state: IntArray): IntArray {
        val out = ArrayList<Int>(2 + STATE_LENGTH * 16 + BLOCK_FOOTER_BITS * 2 + 4)
        fun bits(value: Int, count: Int) {
            for (bit in 0 until count) {
                out.add(BIT_MARK)
                out.add(if ((value ushr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE)
            }
        }
        out.add(HDR_MARK)
        out.add(HDR_SPACE)
        for (i in 0 until 4) bits(state[i], 8)
        bits(BLOCK_FOOTER, BLOCK_FOOTER_BITS)
        out.add(BIT_MARK)
        out.add(MSG_SPACE)
        for (i in 4 until STATE_LENGTH) bits(state[i], 8)
        out.add(BIT_MARK)
        return out.toIntArray()
    }
}

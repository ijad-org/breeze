package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Mitsubishi Electric 144-bit AC IR encoder (MITSUBISHI_AC).
 *
 * Protocol constants and state layout from crankyoldgit/IRremoteESP8266
 * `ir_Mitsubishi.{h,cpp}` (`sendMitsubishiAC`, `IRMitsubishiAC`).
 *
 * 18 bytes, LSB-first, 38 kHz, full state per frame. The message is sent twice
 * with a repeat gap, like the real remote (`kMitsubishiACMinRepeat`).
 * Swing maps to the vane's "swing" setting; off is the vane's "auto" position.
 */
object MitsubishiIrCodec {
    const val FREQUENCY_HZ = 38_000

    const val STATE_LENGTH = 18

    private const val HDR_MARK = 3400
    private const val HDR_SPACE = 1750
    private const val BIT_MARK = 450
    private const val ONE_SPACE = 1300
    private const val ZERO_SPACE = 420
    private const val RPT_MARK = 440
    private const val RPT_SPACE = 15500
    private const val SENDS = 2

    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 31

    private const val POWER = 0x20
    private const val VANE_BIT = 0x40
    private const val FAN_AUTO = 0x80
    private const val VANE_AUTO = 0b000
    private const val VANE_SWING = 0b111

    /** Fixed header bytes 0–4 of every frame. */
    private val HEADER = intArrayOf(0x23, 0xCB, 0x26, 0x01, 0x00)

    fun patternFor(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray = stateToPattern(stateFor(poweredOn, mode, temperatureC, fan, swingOn))

    /**
     * Pairing probes, same order as the other codecs: Cool 24° Auto ON, then OFF, Cool 22, Heat 24, Fan.
     */
    fun powerProbeVariants(): List<IntArray> = listOf(
        patternFor(true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
        patternFor(false, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
        patternFor(true, AcMode.Cool, 22, FanSpeed.Auto, swingOn = false),
        patternFor(true, AcMode.Heat, 24, FanSpeed.Auto, swingOn = false),
        patternFor(true, AcMode.Fan, 24, FanSpeed.Auto, swingOn = false)
    )

    /**
     * The 18-byte state with its checksum. Clock, timers and the extra feature
     * bytes are zero, as in real captures; the wide vane stays in the middle.
     */
    internal fun stateFor(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray {
        val state = IntArray(STATE_LENGTH)
        HEADER.copyInto(state)
        state[5] = if (poweredOn) POWER else 0
        state[6] = modeBits(mode) shl 3
        state[7] = temperatureC.coerceIn(MIN_TEMP, MAX_TEMP) - MIN_TEMP
        state[8] = modeByte8(mode)
        val vane = if (swingOn) VANE_SWING else VANE_AUTO
        state[9] = fanBits(fan) or (vane shl 3) or VANE_BIT or (if (fan == FanSpeed.Auto) FAN_AUTO else 0)
        state[STATE_LENGTH - 1] = checksum(state)
        return state
    }

    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Heat -> 0b001
        AcMode.Dry -> 0b010
        AcMode.Cool -> 0b011
        AcMode.Auto -> 0b100
        AcMode.Fan -> 0b111
    }

    /** IRremoteESP8266 `setMode()` also rewrites byte 8: wide vane middle plus mode-specific low bits. */
    private fun modeByte8(mode: AcMode): Int = when (mode) {
        AcMode.Heat, AcMode.Auto -> 0b00110000
        AcMode.Dry -> 0b00110010
        AcMode.Cool -> 0b00110110
        AcMode.Fan -> 0b00110111
    }

    /** Auto is 0 plus the FanAuto bit; Low/Mid/High follow IRremoteESP8266 `convertFan`. */
    private fun fanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Auto -> 0
        FanSpeed.Low -> 1
        FanSpeed.Medium -> 2
        FanSpeed.High -> 3
    }

    /** Sum of bytes 0–16, low 8 bits. */
    internal fun checksum(state: IntArray): Int {
        var sum = 0
        for (i in 0 until STATE_LENGTH - 1) sum += state[i]
        return sum and 0xFF
    }

    private fun stateToPattern(state: IntArray): IntArray {
        val out = ArrayList<Int>(SENDS * (2 + STATE_LENGTH * 16 + 2))
        repeat(SENDS) { send ->
            if (send > 0) out.add(RPT_SPACE)
            out.add(HDR_MARK)
            out.add(HDR_SPACE)
            for (byte in state) {
                for (bit in 0 until 8) {
                    out.add(BIT_MARK)
                    out.add(if ((byte ushr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE)
                }
            }
            out.add(RPT_MARK)
        }
        return out.toIntArray()
    }
}

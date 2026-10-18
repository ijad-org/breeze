package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Voltas 80-bit AC IR encoder (VOLTAS, 122LZF remote).
 *
 * Protocol constants and state layout from crankyoldgit/IRremoteESP8266
 * `ir_Voltas.{h,cpp}` (`sendVoltas`, `IRVoltas`).
 *
 * 10 bytes, MSB-first, 38 kHz, no header, full state per frame.
 * The protocol has no Auto mode, so Auto is sent as Cool (`convertMode`).
 * Like the real remote, Dry always sends 24° and low fan, and Fan mode sends
 * high fan when Auto is picked.
 */
object VoltasIrCodec {
    const val FREQUENCY_HZ = 38_000

    const val STATE_LENGTH = 10

    private const val BIT_MARK = 1026
    private const val ONE_SPACE = 2553
    private const val ZERO_SPACE = 554

    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 30
    private const val DRY_TEMP = 24

    private const val MODE_FAN = 0b0001
    private const val MODE_HEAT = 0b0010
    private const val MODE_DRY = 0b0100
    private const val MODE_COOL = 0b1000

    private const val FAN_HIGH = 0b001
    private const val FAN_MEDIUM = 0b010
    private const val FAN_LOW = 0b100
    private const val FAN_AUTO = 0b111

    private const val POWER = 0x80
    private const val SWING_V_ON = 0b111

    /**
     * IRremoteESP8266 `stateReset()` without its checksum: Cool 23° high fan, swing and
     * power off, timers off. Byte 0 is the 122LZF "no horizontal swing change" value.
     */
    private val BASE_STATE = intArrayOf(0x33, 0x28, 0x00, 0x17, 0x3B, 0x3B, 0x3B, 0x11, 0x00)

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

    /** The 10-byte state with its checksum. */
    internal fun stateFor(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray {
        val modeBits = modeBits(mode)
        val temp = if (modeBits == MODE_DRY) DRY_TEMP else temperatureC.coerceIn(MIN_TEMP, MAX_TEMP)
        val fanBits = when {
            modeBits == MODE_DRY -> FAN_LOW
            modeBits == MODE_FAN && fan == FanSpeed.Auto -> FAN_HIGH
            else -> fanBits(fan)
        }
        val state = BASE_STATE.copyOf(STATE_LENGTH)
        state[1] = (state[1] and 0x10) or modeBits or (fanBits shl 5)
        state[2] = (if (swingOn) SWING_V_ON else 0) or (if (poweredOn) POWER else 0)
        state[3] = (state[3] and 0xF0) or (temp - MIN_TEMP)
        state[STATE_LENGTH - 1] = checksum(state)
        return state
    }

    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Fan -> MODE_FAN
        AcMode.Heat -> MODE_HEAT
        AcMode.Dry -> MODE_DRY
        AcMode.Cool, AcMode.Auto -> MODE_COOL
    }

    private fun fanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Low -> FAN_LOW
        FanSpeed.Medium -> FAN_MEDIUM
        FanSpeed.High -> FAN_HIGH
        FanSpeed.Auto -> FAN_AUTO
    }

    /** Inverted sum of bytes 0–8, low 8 bits. */
    internal fun checksum(state: IntArray): Int {
        var sum = 0
        for (i in 0 until STATE_LENGTH - 1) sum += state[i]
        return sum.inv() and 0xFF
    }

    private fun stateToPattern(state: IntArray): IntArray {
        val out = ArrayList<Int>(STATE_LENGTH * 16 + 1)
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

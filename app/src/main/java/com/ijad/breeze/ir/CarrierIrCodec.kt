package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Carrier 64-bit AC IR encoder (CARRIER_AC64).
 *
 * Protocol constants and frame layout from crankyoldgit/IRremoteESP8266
 * `ir_Carrier.{h,cpp}` (Carrier/Surrey 42QG5A55970 remote, 619EGX* and 53NGK* units).
 *
 * Pulse-distance, LSB-first, 38 kHz. Full state per frame, so power off and
 * swing are the current state with one bit changed, not fixed frames.
 *
 * The protocol only knows Heat, Cool and Fan: Dry and Auto are sent as Cool,
 * which matches IRremoteESP8266's `convertMode`.
 */
object CarrierIrCodec {
    const val FREQUENCY_HZ = 38_000

    /** IRremoteESP8266 `stateReset()`: Cool 28° Auto fan, swing on, power off, no timers. */
    private const val BASE_STATE = 0x109000002C2A5584L

    private const val SUM_OFFSET = 16
    private const val MODE_OFFSET = 20
    private const val FAN_OFFSET = 22
    private const val TEMP_OFFSET = 24
    private const val SWING_BIT = 29
    private const val POWER_BIT = 36
    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 30

    private const val HDR_MARK = 8940
    private const val HDR_SPACE = 4556
    private const val BIT_MARK = 503
    private const val ONE_SPACE = 1736
    private const val ZERO_SPACE = 615
    private const val TRAIL_MARK = 503

    fun patternFor(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray = frameToPattern(encode(poweredOn, mode, temperatureC, fan, swingOn))

    /**
     * Light set of Power probe frames for pairing.
     * Index 0 = Cool 24° Auto ON; then OFF; Cool 22; Heat 24; Fan Auto.
     */
    fun powerProbeVariants(): List<IntArray> = listOf(
        patternFor(true, AcMode.Cool, 24, FanSpeed.Auto, true),
        patternFor(false, AcMode.Cool, 24, FanSpeed.Auto, true),
        patternFor(true, AcMode.Cool, 22, FanSpeed.Auto, true),
        patternFor(true, AcMode.Heat, 24, FanSpeed.Auto, true),
        patternFor(true, AcMode.Fan, 24, FanSpeed.Auto, true)
    )

    internal fun encode(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): Long {
        val temp = temperatureC.coerceIn(MIN_TEMP, MAX_TEMP)
        var code = BASE_STATE
        code = setBits(code, MODE_OFFSET, 2, modeBits(mode))
        code = setBits(code, FAN_OFFSET, 2, fanBits(fan))
        code = setBits(code, TEMP_OFFSET, 4, temp - MIN_TEMP)
        code = setBits(code, SWING_BIT, 1, if (swingOn) 1 else 0)
        code = setBits(code, POWER_BIT, 1, if (poweredOn) 1 else 0)
        return setBits(code, SUM_OFFSET, 4, checksum(code))
    }

    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Heat -> 1
        AcMode.Fan -> 3
        AcMode.Cool, AcMode.Dry, AcMode.Auto -> 2
    }

    private fun fanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Auto -> 0
        FanSpeed.Low -> 1
        FanSpeed.Medium -> 2
        FanSpeed.High -> 3
    }

    /** Sum of every nibble above the checksum field, low 4 bits. */
    internal fun checksum(frame: Long): Int {
        var data = frame ushr (SUM_OFFSET + 4)
        var sum = 0
        while (data != 0L) {
            sum += (data and 0xF).toInt()
            data = data ushr 4
        }
        return sum and 0xF
    }

    private fun setBits(frame: Long, offset: Int, size: Int, value: Int): Long {
        val mask = ((1L shl size) - 1) shl offset
        return (frame and mask.inv()) or ((value.toLong() shl offset) and mask)
    }

    private fun frameToPattern(frame64: Long): IntArray {
        val out = ArrayList<Int>(131)
        out.add(HDR_MARK)
        out.add(HDR_SPACE)
        for (i in 0 until 64) {
            val bit = (frame64 ushr i) and 1L
            out.add(BIT_MARK)
            out.add(if (bit == 1L) ONE_SPACE else ZERO_SPACE)
        }
        out.add(TRAIL_MARK)
        return out.toIntArray()
    }
}

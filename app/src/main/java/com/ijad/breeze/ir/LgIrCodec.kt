package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Classic LG 28-bit AC IR encoder (signature 0x88).
 *
 * Protocol constants and frame layout from open-source references:
 * - crankyoldgit/IRremoteESP8266 `ir_LG.{h,cpp}` (LG / GE6711AR2853M family)
 * - Nosdave/ha-lg-ac-infrared `codec.py`
 * - Arduino-IRremote `ac_LG.h`
 *
 * Pulse-distance, MSB-first, 38 kHz. Not LG2 (AKB74* / 3200µs header).
 */
object LgIrCodec {
    const val FREQUENCY_HZ = 38_000

    private const val SIGNATURE = 0x88
    private const val OFF_FRAME = 0x88C0051
    private const val SWING_V_SWING = 0x8813149
    private const val SWING_H_AUTO = 0x881316B
    private const val TEMP_OFFSET = 15

    private const val HDR_MARK = 8500
    private const val HDR_SPACE = 4250
    private const val BIT_MARK = 550
    private const val ONE_SPACE = 1600
    private const val ZERO_SPACE = 550
    private const val TRAIL_MARK = 550

    fun patternFor(
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed
    ): IntArray {
        val frame = if (!poweredOn) OFF_FRAME else encodeOn(mode, temperatureC, fan)
        return frameToPattern(frame)
    }

    fun swingPattern(): IntArray = frameToPattern(SWING_V_SWING)

    fun swingHorizontalPattern(): IntArray = frameToPattern(SWING_H_AUTO)

    /**
     * Light set of Power probe frames for pairing.
     * Index 0 = Cool 24° Auto ON; then OFF; Cool 22; Heat 24; Fan Auto.
     */
    fun powerProbeVariants(): List<IntArray> = listOf(
        patternFor(true, AcMode.Cool, 24, FanSpeed.Auto),
        powerOffPattern(),
        patternFor(true, AcMode.Cool, 22, FanSpeed.Auto),
        patternFor(true, AcMode.Heat, 24, FanSpeed.Auto),
        patternFor(true, AcMode.Fan, 18, FanSpeed.Auto)
    )

    fun powerOffPattern(): IntArray = frameToPattern(OFF_FRAME)

    private fun encodeOn(mode: AcMode, temperatureC: Int, fan: FanSpeed): Int {
        val temp = temperatureC.coerceIn(16, 30)
        var code = (SIGNATURE and 0xFF) shl 20
        code = code or ((modeBits(mode) and 0x7) shl 12)
        code = code or (((temp - TEMP_OFFSET) and 0xF) shl 8)
        code = code or ((fanBits(fan) and 0xF) shl 4)
        code = code or checksum(code)
        return code and 0x0FFFFFFF
    }

    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Cool -> 0
        AcMode.Dry -> 1
        AcMode.Fan -> 2
        AcMode.Auto -> 3
        AcMode.Heat -> 4
    }

    private fun fanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Low -> 1
        FanSpeed.Medium -> 2
        FanSpeed.High -> 4
        FanSpeed.Auto -> 5
    }

    private fun checksum(frameNoCsum: Int): Int {
        var body = (frameNoCsum ushr 4) and 0xFFFF
        var sum = 0
        repeat(4) {
            sum += body and 0xF
            body = body ushr 4
        }
        return sum and 0xF
    }

    private fun frameToPattern(frame28: Int): IntArray {
        val out = ArrayList<Int>(59)
        out.add(HDR_MARK)
        out.add(HDR_SPACE)
        for (i in 27 downTo 0) {
            val bit = (frame28 ushr i) and 1
            out.add(BIT_MARK)
            out.add(if (bit == 1) ONE_SPACE else ZERO_SPACE)
        }
        out.add(TRAIL_MARK)
        return out.toIntArray()
    }
}

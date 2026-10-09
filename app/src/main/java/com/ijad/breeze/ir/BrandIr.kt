package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Resolves IR patterns per brand. LG uses classic 28-bit @ 38 kHz, Samsung its
 * 21-byte extended state @ 38 kHz, Carrier 64-bit full state @ 38 kHz; other
 * brands return null so the UI can toast "codes coming".
 */
object BrandIr {
    data class Pattern(val frequencyHz: Int, val micros: IntArray)

    fun controlPattern(
        brandId: String,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): Pattern? = when (brandId) {
        "lg" -> Pattern(
            LgIrCodec.FREQUENCY_HZ,
            LgIrCodec.patternFor(poweredOn, mode, temperatureC, fan)
        )
        "samsung" -> Pattern(
            SamsungIrCodec.FREQUENCY_HZ,
            SamsungIrCodec.patternFor(poweredOn, mode, temperatureC, fan, swingOn)
        )
        "carrier" -> Pattern(
            CarrierIrCodec.FREQUENCY_HZ,
            CarrierIrCodec.patternFor(poweredOn, mode, temperatureC, fan, swingOn)
        )
        else -> null
    }

    /**
     * LG has a stateless swing command. Samsung and Carrier keep swing in their
     * state, so the whole state is resent with the new swing setting.
     */
    fun swingPattern(
        brandId: String,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): Pattern? = when (brandId) {
        "lg" -> Pattern(LgIrCodec.FREQUENCY_HZ, LgIrCodec.swingPattern())
        "samsung", "carrier" -> controlPattern(brandId, poweredOn, mode, temperatureC, fan, swingOn)
        else -> null
    }

    fun powerProbe(brandId: String, index: Int): Pattern? {
        val variants = probeVariants(brandId)
            // Placeholder NEC-like chirp so pairing UX still "sends" something
            // on IR hardware while real codes are pending.
            ?: return placeholderChirp(index)
        return variants[index.coerceIn(0, variants.lastIndex)]
    }

    fun probeCount(brandId: String, fallback: Int): Int =
        probeVariants(brandId)?.size ?: fallback

    private fun probeVariants(brandId: String): List<Pattern>? = when (brandId) {
        "lg" -> LgIrCodec.powerProbeVariants().map { Pattern(LgIrCodec.FREQUENCY_HZ, it) }
        "samsung" -> SamsungIrCodec.powerProbeVariants().map { Pattern(SamsungIrCodec.FREQUENCY_HZ, it) }
        "carrier" -> CarrierIrCodec.powerProbeVariants().map { Pattern(CarrierIrCodec.FREQUENCY_HZ, it) }
        else -> null
    }

    private fun placeholderChirp(seed: Int): Pattern {
        // Minimal NEC-ish pulse train — not a real AC command.
        val hdrMark = 9000
        val hdrSpace = 4500
        val bitMark = 560
        val oneSpace = 1690
        val zeroSpace = 560
        val out = ArrayList<Int>(35)
        out.add(hdrMark)
        out.add(hdrSpace)
        var bits = 0x20A0A0 or ((seed and 0xF) shl 4)
        for (i in 15 downTo 0) {
            val bit = (bits ushr i) and 1
            out.add(bitMark)
            out.add(if (bit == 1) oneSpace else zeroSpace)
        }
        out.add(bitMark)
        return Pattern(38_000, out.toIntArray())
    }
}

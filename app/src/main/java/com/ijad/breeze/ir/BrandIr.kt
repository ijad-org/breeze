package com.ijad.breeze.ir

import com.ijad.breeze.data.RemoteState

/**
 * Resolves IR patterns per brand. LG uses classic 28-bit @ 38 kHz, Samsung its
 * 21-byte extended state @ 38 kHz, Carrier 64-bit full state @ 38 kHz, Daikin
 * full-state frames with the protocol picked by the paired code
 * ([DaikinIrCodec.protocolFor]); other brands return null so the UI can toast
 * "codes coming".
 */
object BrandIr {
    data class Pattern(val frequencyHz: Int, val micros: IntArray)

    fun controlPattern(brandId: String, configIndex: Int, state: RemoteState): Pattern? = when (brandId) {
        "lg" -> Pattern(
            LgIrCodec.FREQUENCY_HZ,
            LgIrCodec.patternFor(state.poweredOn, state.mode, state.temperatureC, state.fan)
        )
        "samsung" -> Pattern(
            SamsungIrCodec.FREQUENCY_HZ,
            SamsungIrCodec.patternFor(state.poweredOn, state.mode, state.temperatureC, state.fan, state.swingOn)
        )
        "carrier" -> Pattern(
            CarrierIrCodec.FREQUENCY_HZ,
            CarrierIrCodec.patternFor(state.poweredOn, state.mode, state.temperatureC, state.fan, state.swingOn)
        )
        "daikin" -> daikinPattern(configIndex, state)
        else -> null
    }

    /**
     * [state] already carries the new swing setting. LG has a stateless swing command.
     * Samsung, Carrier and Daikin keep swing in their state, so the whole state is resent.
     */
    fun swingPattern(brandId: String, configIndex: Int, state: RemoteState): Pattern? = when (brandId) {
        "lg" -> Pattern(LgIrCodec.FREQUENCY_HZ, LgIrCodec.swingPattern())
        "samsung", "carrier", "daikin" -> controlPattern(brandId, configIndex, state)
        else -> null
    }

    private fun daikinPattern(configIndex: Int, state: RemoteState) = Pattern(
        DaikinIrCodec.FREQUENCY_HZ,
        DaikinIrCodec.patternFor(
            DaikinIrCodec.protocolFor(configIndex),
            state.poweredOn, state.mode, state.temperatureC, state.fan, state.swingOn
        )
    )

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
        "daikin" -> DaikinIrCodec.powerProbeVariants().map { Pattern(DaikinIrCodec.FREQUENCY_HZ, it) }
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

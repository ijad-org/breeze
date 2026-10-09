package com.ijad.breeze.ir

import com.ijad.breeze.data.RemoteState

/**
 * Resolves IR patterns per brand. LG uses classic 28-bit and Carrier uses
 * 64-bit full-state frames, both @ 38 kHz; other brands return null so the
 * UI can toast "codes coming".
 */
object BrandIr {
    data class Pattern(val frequencyHz: Int, val micros: IntArray)

    fun controlPattern(brandId: String, state: RemoteState): Pattern? = when (brandId) {
        "lg" -> Pattern(
            LgIrCodec.FREQUENCY_HZ,
            LgIrCodec.patternFor(state.poweredOn, state.mode, state.temperatureC, state.fan)
        )
        "carrier" -> Pattern(
            CarrierIrCodec.FREQUENCY_HZ,
            CarrierIrCodec.patternFor(
                state.poweredOn, state.mode, state.temperatureC, state.fan, state.swingOn
            )
        )
        else -> null
    }

    /** [state] already holds the new swing value; LG toggles with a fixed frame, Carrier resends the state. */
    fun swingPattern(brandId: String, state: RemoteState): Pattern? = when (brandId) {
        "lg" -> Pattern(LgIrCodec.FREQUENCY_HZ, LgIrCodec.swingPattern())
        "carrier" -> controlPattern(brandId, state)
        else -> null
    }

    fun powerProbe(brandId: String, index: Int): Pattern? {
        val (frequencyHz, variants) = probeVariants(brandId)
            // Placeholder NEC-like chirp so pairing UX still "sends" something
            // on IR hardware while real codes are pending.
            ?: return placeholderChirp(index)
        return Pattern(frequencyHz, variants[index.coerceIn(0, variants.lastIndex)])
    }

    fun probeCount(brandId: String, fallback: Int): Int =
        probeVariants(brandId)?.second?.size ?: fallback

    private fun probeVariants(brandId: String): Pair<Int, List<IntArray>>? = when (brandId) {
        "lg" -> LgIrCodec.FREQUENCY_HZ to LgIrCodec.powerProbeVariants()
        "carrier" -> CarrierIrCodec.FREQUENCY_HZ to CarrierIrCodec.powerProbeVariants()
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

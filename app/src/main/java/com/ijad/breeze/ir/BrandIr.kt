package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Resolves IR patterns per brand. LG uses classic 28-bit @ 38 kHz;
 * other brands return null so the UI can toast "codes coming".
 */
object BrandIr {
    data class Pattern(val frequencyHz: Int, val micros: IntArray)

    fun controlPattern(
        brandId: String,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed
    ): Pattern? {
        if (brandId != "lg") return null
        return Pattern(
            LgIrCodec.FREQUENCY_HZ,
            LgIrCodec.patternFor(poweredOn, mode, temperatureC, fan)
        )
    }

    fun swingPattern(brandId: String): Pattern? {
        if (brandId != "lg") return null
        return Pattern(LgIrCodec.FREQUENCY_HZ, LgIrCodec.swingPattern())
    }

    fun powerProbe(brandId: String, index: Int): Pattern? {
        if (brandId == "lg") {
            val variants = LgIrCodec.powerProbeVariants()
            val i = index.coerceIn(0, variants.lastIndex)
            return Pattern(LgIrCodec.FREQUENCY_HZ, variants[i])
        }
        // Placeholder NEC-like chirp so pairing UX still "sends" something
        // on IR hardware while real codes are pending.
        return placeholderChirp(index)
    }

    fun probeCount(brandId: String, fallback: Int): Int {
        return if (brandId == "lg") LgIrCodec.powerProbeVariants().size else fallback
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

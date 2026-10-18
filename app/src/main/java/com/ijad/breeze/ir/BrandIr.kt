package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.data.RemoteState

/**
 * Resolves IR patterns per brand. LG uses classic 28-bit @ 38 kHz, Samsung its
 * 21-byte extended state @ 38 kHz, Carrier 64-bit full state @ 38 kHz,
 * Mitsubishi 144-bit full state @ 38 kHz, Voltas 80-bit full state @ 38 kHz.
 * Daikin, Panasonic and Haier have several protocols or remote models, picked
 * by the paired code (two probes, ON and OFF, per variant).
 *
 * Brands without a codec of their own ("Other", and Blue Star, whose protocol
 * isn't documented) step through every supported variant during pairing, one
 * probe each, and the paired code picks the variant from [SWEEP].
 */
object BrandIr {
    data class Pattern(val frequencyHz: Int, val micros: IntArray)

    /** One protocol variant: its carrier, a full-state frame and a swing press. */
    private class Codec(
        val frequencyHz: Int,
        val control: (RemoteState) -> IntArray,
        val swing: (RemoteState) -> IntArray = control
    )

    private val LG = Codec(
        LgIrCodec.FREQUENCY_HZ,
        { LgIrCodec.patternFor(it.poweredOn, it.mode, it.temperatureC, it.fan) },
        // LG has a stateless swing command.
        { LgIrCodec.swingPattern() }
    )
    private val SAMSUNG = Codec(SamsungIrCodec.FREQUENCY_HZ, {
        SamsungIrCodec.patternFor(it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn)
    })
    private val CARRIER = Codec(CarrierIrCodec.FREQUENCY_HZ, {
        CarrierIrCodec.patternFor(it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn)
    })
    private val MITSUBISHI = Codec(MitsubishiIrCodec.FREQUENCY_HZ, {
        MitsubishiIrCodec.patternFor(it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn)
    })
    private val VOLTAS = Codec(VoltasIrCodec.FREQUENCY_HZ, {
        VoltasIrCodec.patternFor(it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn)
    })

    private fun daikin(protocol: DaikinIrCodec.Protocol) = Codec(DaikinIrCodec.FREQUENCY_HZ, {
        DaikinIrCodec.patternFor(protocol, it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn)
    })

    private fun panasonic(model: PanasonicIrCodec.Model) = Codec(PanasonicIrCodec.FREQUENCY_HZ, {
        PanasonicIrCodec.patternFor(model, it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn)
    })

    private fun haier(protocol: HaierIrCodec.Protocol) = Codec(
        HaierIrCodec.FREQUENCY_HZ,
        { HaierIrCodec.patternFor(protocol, it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn) },
        { HaierIrCodec.swingPatternFor(protocol, it.poweredOn, it.mode, it.temperatureC, it.fan, it.swingOn) }
    )

    /**
     * Every supported variant, roughly most common first. A paired "Other" AC stores its
     * index here as `configIndex`, so only ever append to this list.
     */
    private val SWEEP: List<Codec> = listOf(
        SAMSUNG,
        LG,
        daikin(DaikinIrCodec.Protocol.Arc4xx),
        MITSUBISHI,
        VOLTAS,
        CARRIER,
        haier(HaierIrCodec.Protocol.Yrw02),
        panasonic(PanasonicIrCodec.Model.Dke),
        daikin(DaikinIrCodec.Protocol.Arc433b69),
        haier(HaierIrCodec.Protocol.Ac176),
        haier(HaierIrCodec.Protocol.Hsu07),
        panasonic(PanasonicIrCodec.Model.Nke),
        panasonic(PanasonicIrCodec.Model.Jke),
        panasonic(PanasonicIrCodec.Model.Lke),
        panasonic(PanasonicIrCodec.Model.Rkr)
    )

    /** Sweep probe: Cool 24° Auto ON, the same first probe the brand codecs use. */
    private val SWEEP_PROBE_STATE = RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false)

    /** The variant a paired device uses. Out-of-range codes (older placeholder pairings) use the first variant. */
    private fun codecFor(brandId: String, configIndex: Int): Codec? = when (brandId) {
        "lg" -> LG
        "samsung" -> SAMSUNG
        "carrier" -> CARRIER
        "mitsubishi" -> MITSUBISHI
        "voltas" -> VOLTAS
        "daikin" -> daikin(DaikinIrCodec.protocolFor(configIndex))
        "panasonic" -> panasonic(PanasonicIrCodec.modelFor(configIndex))
        "haier" -> haier(HaierIrCodec.protocolFor(configIndex))
        "other", "bluestar" -> SWEEP.getOrNull(configIndex) ?: SWEEP.first()
        else -> null
    }

    fun controlPattern(brandId: String, configIndex: Int, state: RemoteState): Pattern? =
        codecFor(brandId, configIndex)?.let { Pattern(it.frequencyHz, it.control(state)) }

    /**
     * [state] already carries the new swing setting. LG sends its swing command; the other
     * protocols keep swing in their state, so the whole state is resent.
     */
    fun swingPattern(brandId: String, configIndex: Int, state: RemoteState): Pattern? =
        codecFor(brandId, configIndex)?.let { Pattern(it.frequencyHz, it.swing(state)) }

    fun powerProbe(brandId: String, index: Int): Pattern? {
        val variants = probeVariants(brandId) ?: return null
        return variants[index.coerceIn(0, variants.lastIndex)]
    }

    fun probeCount(brandId: String): Int = probeVariants(brandId)?.size ?: 0

    private fun probeVariants(brandId: String): List<Pattern>? = when (brandId) {
        "lg" -> LgIrCodec.powerProbeVariants().map { Pattern(LgIrCodec.FREQUENCY_HZ, it) }
        "samsung" -> SamsungIrCodec.powerProbeVariants().map { Pattern(SamsungIrCodec.FREQUENCY_HZ, it) }
        "carrier" -> CarrierIrCodec.powerProbeVariants().map { Pattern(CarrierIrCodec.FREQUENCY_HZ, it) }
        "daikin" -> DaikinIrCodec.powerProbeVariants().map { Pattern(DaikinIrCodec.FREQUENCY_HZ, it) }
        "mitsubishi" -> MitsubishiIrCodec.powerProbeVariants().map { Pattern(MitsubishiIrCodec.FREQUENCY_HZ, it) }
        "voltas" -> VoltasIrCodec.powerProbeVariants().map { Pattern(VoltasIrCodec.FREQUENCY_HZ, it) }
        "panasonic" -> PanasonicIrCodec.powerProbeVariants().map { Pattern(PanasonicIrCodec.FREQUENCY_HZ, it) }
        "haier" -> HaierIrCodec.powerProbeVariants().map { Pattern(HaierIrCodec.FREQUENCY_HZ, it) }
        "other", "bluestar" -> SWEEP.map { Pattern(it.frequencyHz, it.control(SWEEP_PROBE_STATE)) }
        else -> null
    }
}

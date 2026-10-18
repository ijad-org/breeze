package com.ijad.breeze.ir

import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.Brands
import com.ijad.breeze.data.FanSpeed
import com.ijad.breeze.data.RemoteState

/**
 * Resolves IR patterns per brand. LG uses classic 28-bit @ 38 kHz, Samsung its
 * 21-byte extended state @ 38 kHz, Carrier 64-bit full state @ 38 kHz,
 * Mitsubishi 144-bit full state @ 38 kHz, Voltas 80-bit full state @ 38 kHz.
 * Daikin, Panasonic, Haier and Blue Star (Gree or Coolix based) have several
 * protocols or remote models, picked by the paired code (two probes, ON and OFF,
 * per variant). Carrier adds Coolix probes after its own for Midea-built units.
 * Panasonic CKP and AC32 toggle power, so control frames say whether power changed.
 *
 * "Other" steps through every supported variant during pairing, one probe each,
 * and the paired code picks the variant from [SWEEP].
 */
object BrandIr {
    data class Pattern(val frequencyHz: Int, val micros: IntArray)

    /**
     * One protocol variant: its carrier, a full-state frame (given whether power changed,
     * which only toggling protocols use) and a swing press (power unchanged).
     */
    private class Codec(
        val frequencyHz: Int,
        val control: (RemoteState, Boolean) -> IntArray,
        val swing: (RemoteState) -> IntArray = { control(it, false) }
    )

    private val LG = Codec(
        LgIrCodec.FREQUENCY_HZ,
        { s, _ -> LgIrCodec.patternFor(s.poweredOn, s.mode, s.temperatureC, s.fan) },
        // LG has a stateless swing command.
        { LgIrCodec.swingPattern() }
    )
    private val SAMSUNG = Codec(SamsungIrCodec.FREQUENCY_HZ, { s, _ ->
        SamsungIrCodec.patternFor(s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn)
    })
    private val CARRIER = Codec(CarrierIrCodec.FREQUENCY_HZ, { s, _ ->
        CarrierIrCodec.patternFor(s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn)
    })
    private val MITSUBISHI = Codec(MitsubishiIrCodec.FREQUENCY_HZ, { s, _ ->
        MitsubishiIrCodec.patternFor(s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn)
    })
    private val VOLTAS = Codec(VoltasIrCodec.FREQUENCY_HZ, { s, _ ->
        VoltasIrCodec.patternFor(s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn)
    })

    private val COOLIX = Codec(
        CoolixIrCodec.FREQUENCY_HZ,
        { s, _ -> CoolixIrCodec.patternFor(s.poweredOn, s.mode, s.temperatureC, s.fan) },
        // Coolix swing is a toggle command, like LG's.
        { CoolixIrCodec.swingPattern() }
    )

    private fun gree(variant: GreeIrCodec.Variant) = Codec(GreeIrCodec.FREQUENCY_HZ, { s, _ ->
        GreeIrCodec.patternFor(variant, s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn)
    })

    private fun daikin(protocol: DaikinIrCodec.Protocol) = Codec(DaikinIrCodec.FREQUENCY_HZ, { s, _ ->
        DaikinIrCodec.patternFor(protocol, s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn)
    })

    private fun panasonic(model: PanasonicIrCodec.Model) = Codec(PanasonicIrCodec.FREQUENCY_HZ, { s, powerChanged ->
        PanasonicIrCodec.patternFor(model, s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn, powerChanged)
    })

    private fun haier(protocol: HaierIrCodec.Protocol) = Codec(
        HaierIrCodec.FREQUENCY_HZ,
        { s, _ -> HaierIrCodec.patternFor(protocol, s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn) },
        { s -> HaierIrCodec.swingPatternFor(protocol, s.poweredOn, s.mode, s.temperatureC, s.fan, s.swingOn) }
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
        panasonic(PanasonicIrCodec.Model.Rkr),
        haier(HaierIrCodec.Protocol.Ac160),
        panasonic(PanasonicIrCodec.Model.Ckp),
        panasonic(PanasonicIrCodec.Model.Ac32),
        gree(GreeIrCodec.Variant.Yaw1f),
        gree(GreeIrCodec.Variant.Ybofb),
        COOLIX
    )

    /**
     * Blue Star variants, two probes each. Published Blue Star captures are Gree frames
     * (techantidote.com) and the Coolix off frame (IRremoteESP8266 #498). Only append.
     */
    private val BLUE_STAR: List<Codec> = listOf(
        gree(GreeIrCodec.Variant.BlueStar),
        gree(GreeIrCodec.Variant.Yaw1f),
        COOLIX
    )
    private val BLUE_STAR_PROBES: List<List<IntArray>> = listOf(
        GreeIrCodec.powerProbeVariants(GreeIrCodec.Variant.BlueStar),
        GreeIrCodec.powerProbeVariants(GreeIrCodec.Variant.Yaw1f),
        CoolixIrCodec.powerProbeVariants()
    )

    /** Carrier's own CARRIER_AC64 probes come first; later codes are Coolix. */
    private val CARRIER_AC64_PROBES = CarrierIrCodec.powerProbeVariants().size

    /** Sweep probe: Cool 24° Auto ON, the same first probe the brand codecs use. */
    private val SWEEP_PROBE_STATE = RemoteState(true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false)

    /** The variant a paired device uses. Out-of-range codes (older placeholder pairings) use the first variant. */
    private fun codecFor(brandId: String, configIndex: Int): Codec? = when (brandId) {
        "lg" -> LG
        "samsung" -> SAMSUNG
        "carrier" -> if (configIndex >= CARRIER_AC64_PROBES) COOLIX else CARRIER
        "mitsubishi" -> MITSUBISHI
        "voltas" -> VOLTAS
        "daikin" -> daikin(DaikinIrCodec.protocolFor(configIndex))
        "panasonic" -> panasonic(PanasonicIrCodec.modelFor(configIndex))
        "haier" -> haier(HaierIrCodec.protocolFor(configIndex))
        "bluestar" -> BLUE_STAR.getOrNull(configIndex / 2)?.takeIf { configIndex >= 0 } ?: BLUE_STAR.first()
        "other" -> SWEEP.getOrNull(configIndex) ?: SWEEP.first()
        else -> null
    }

    /**
     * [powerChanged]: whether `state.poweredOn` differs from the AC's last known power. Protocols
     * that toggle power (Panasonic CKP and AC32) send the toggle only then; the rest ignore it.
     */
    fun controlPattern(brandId: String, configIndex: Int, state: RemoteState, powerChanged: Boolean): Pattern? =
        codecFor(brandId, configIndex)?.let { Pattern(it.frequencyHz, it.control(state, powerChanged)) }

    /**
     * [state] already carries the new swing setting. LG and Coolix send their swing toggle
     * command; the other protocols keep swing in their state, so the whole state is resent.
     */
    fun swingPattern(brandId: String, configIndex: Int, state: RemoteState): Pattern? =
        codecFor(brandId, configIndex)?.let { Pattern(it.frequencyHz, it.swing(state)) }

    fun powerProbe(brandId: String, index: Int): Pattern? {
        val variants = probeVariants(brandId) ?: return null
        return variants[index.coerceIn(0, variants.lastIndex)]
    }

    /** Brands whose paired code picks one of several protocols (see [codecFor]). */
    private val CODE_PICKS_PROTOCOL = setOf("carrier", "daikin", "panasonic", "haier", "bluestar", "other")

    /**
     * True when [device] was paired against an older code list and its code now picks a
     * protocol that may never have been tried on it. Single-protocol brands ignore the code.
     */
    fun needsNewCode(device: AcDevice): Boolean =
        device.codeListVersion < Brands.CODE_LIST_VERSION && device.brandId in CODE_PICKS_PROTOCOL

    fun probeCount(brandId: String): Int = probeVariants(brandId)?.size ?: 0

    private fun probeVariants(brandId: String): List<Pattern>? = when (brandId) {
        "lg" -> LgIrCodec.powerProbeVariants().map { Pattern(LgIrCodec.FREQUENCY_HZ, it) }
        "samsung" -> SamsungIrCodec.powerProbeVariants().map { Pattern(SamsungIrCodec.FREQUENCY_HZ, it) }
        "carrier" -> CarrierIrCodec.powerProbeVariants().map { Pattern(CarrierIrCodec.FREQUENCY_HZ, it) } +
            CoolixIrCodec.powerProbeVariants().map { Pattern(CoolixIrCodec.FREQUENCY_HZ, it) }
        "daikin" -> DaikinIrCodec.powerProbeVariants().map { Pattern(DaikinIrCodec.FREQUENCY_HZ, it) }
        "mitsubishi" -> MitsubishiIrCodec.powerProbeVariants().map { Pattern(MitsubishiIrCodec.FREQUENCY_HZ, it) }
        "voltas" -> VoltasIrCodec.powerProbeVariants().map { Pattern(VoltasIrCodec.FREQUENCY_HZ, it) }
        "panasonic" -> PanasonicIrCodec.powerProbeVariants().map { Pattern(PanasonicIrCodec.FREQUENCY_HZ, it) }
        "haier" -> HaierIrCodec.powerProbeVariants().map { Pattern(HaierIrCodec.FREQUENCY_HZ, it) }
        "bluestar" -> BLUE_STAR.zip(BLUE_STAR_PROBES).flatMap { (codec, probes) ->
            probes.map { Pattern(codec.frequencyHz, it) }
        }
        // A power toggle for the toggling protocols, so the probe turns an off AC on.
        "other" -> SWEEP.map { Pattern(it.frequencyHz, it.control(SWEEP_PROBE_STATE, true)) }
        else -> null
    }
}

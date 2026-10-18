package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Panasonic 216-bit AC IR encoder (PANASONIC_AC).
 *
 * Protocol constants and state layout from crankyoldgit/IRremoteESP8266
 * `ir_Panasonic.{h,cpp}` (`sendPanasonicAC`, `IRPanasonicAc`).
 *
 * 27 bytes, LSB-first, 36.7 kHz, full state per frame, sent as an 8-byte
 * section and a 19-byte section 10 ms apart. The remote models differ only in
 * a few fixed bytes, so pairing offers two probes per [Model].
 * Swing on is the vane's auto (swing) setting, swing off its middle position;
 * DKE, RKR and AC32 also swing horizontally.
 *
 * CKP remotes and the separate 32-bit `PANASONIC_AC32` protocol toggle power
 * instead of setting it, so their frames carry the toggle only when the power
 * state changes (`powerChanged`). Their probes are a toggle, then a frame
 * without one.
 */
object PanasonicIrCodec {
    const val FREQUENCY_HZ = 36_700

    const val STATE_LENGTH = 27
    private const val SECTION1_LENGTH = 8

    private const val HDR_MARK = 3456
    private const val HDR_SPACE = 1728
    private const val BIT_MARK = 432
    private const val ONE_SPACE = 1296
    private const val ZERO_SPACE = 432
    private const val SECTION_GAP = 10_000

    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 30
    private const val FAN_MODE_TEMP = 27

    private const val FAN_DELTA = 3
    private const val SWING_V_MIDDLE = 0x3
    private const val SWING_V_AUTO = 0xF
    private const val SWING_H_MIDDLE = 0x6
    private const val SWING_H_AUTO = 0xD
    private const val CHECKSUM_INIT = 0xF4

    enum class Model {
        /** DKE, DKW and PKR series (A75C2616-1, A75C3704, PN1122V remotes); also swings horizontally. */
        Dke,

        /** NKE series. */
        Nke,

        /** JKE series (A75C3747 remote, CS-YW9MKD). */
        Jke,

        /** LKE series. */
        Lke,

        /** RKR series (A75C4762 remote, CS-Z9RKR); also swings horizontally. */
        Rkr,

        /** CKP series (A75C2311 remote, CS-ME10CKPG …); the power bit toggles. */
        Ckp,

        /** The separate 32-bit `PANASONIC_AC32` protocol (A75C2295 remote, CS-E9CKP); toggles power. */
        Ac32
    }

    /**
     * Bytes 0–12 are fixed, and 18–20 and 24–25 hold the remote's "unset" timer and
     * clock value (0x600), as in real captures.
     */
    private val BASE_STATE = intArrayOf(
        0x02, 0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x06,
        0x02, 0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00,
        0x00, 0x06, 0x60, 0x00, 0x00, 0x00, 0x00, 0x06
    )

    /**
     * Pairing probes, two per model: Cool 24° Auto ON, then OFF. For the toggling
     * models ([togglesPower]) they are a power toggle, then the same frame without one.
     * See [modelFor].
     */
    fun powerProbeVariants(): List<IntArray> = Model.entries.flatMap { m ->
        if (togglesPower(m)) listOf(
            patternFor(m, true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false, powerChanged = true),
            patternFor(m, true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false, powerChanged = false)
        ) else listOf(
            patternFor(m, true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
            patternFor(m, false, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false)
        )
    }

    /** True for models whose frames toggle power rather than set it. */
    fun togglesPower(model: Model): Boolean = model == Model.Ckp || model == Model.Ac32

    /** Maps a paired probe index to its model. Unknown indices (e.g. older placeholder pairings) use [Model.Dke]. */
    fun modelFor(configIndex: Int): Model =
        Model.entries.getOrNull(configIndex / 2)?.takeIf { configIndex >= 0 } ?: Model.Dke

    /**
     * [powerChanged] only matters for the toggling models: it sets their power toggle,
     * so pass whether [poweredOn] differs from the AC's last known power state.
     */
    fun patternFor(
        model: Model,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean,
        powerChanged: Boolean = true
    ): IntArray =
        if (model == Model.Ac32) ac32Pattern(ac32StateFor(mode, temperatureC, fan, swingOn, powerChanged))
        else stateToPattern(stateFor(model, poweredOn, mode, temperatureC, fan, swingOn, powerChanged))

    /** The 27-byte state with its checksum. Not used for [Model.Ac32]. */
    internal fun stateFor(
        model: Model,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean,
        powerChanged: Boolean = true
    ): IntArray {
        require(model != Model.Ac32) { "AC32 has a 32-bit state; see ac32StateFor" }
        // IRremoteESP8266 `setMode()`: Fan mode always sends 27°.
        val temp = if (mode == AcMode.Fan) FAN_MODE_TEMP else temperatureC.coerceIn(MIN_TEMP, MAX_TEMP)
        val state = BASE_STATE.copyOf(STATE_LENGTH)
        // CKP's power bit means "toggle"; the others set the power state.
        val powerBit = if (model == Model.Ckp) powerChanged else poweredOn
        state[13] = (modeBits(mode) shl 4) or modelByte13(model) or (if (powerBit) 1 else 0)
        state[14] = temp shl 1
        state[16] = ((fanBits(fan) + FAN_DELTA) shl 4) or (if (swingOn) SWING_V_AUTO else SWING_V_MIDDLE)
        state[17] = when (model) {
            Model.Dke, Model.Rkr -> if (swingOn) SWING_H_AUTO else SWING_H_MIDDLE
            Model.Nke, Model.Lke -> SWING_H_MIDDLE
            Model.Jke, Model.Ckp, Model.Ac32 -> 0x00
        }
        // CKP's model bit (IRremoteESP8266 `setModel()`); quiet and powerful stay off.
        if (model == Model.Ckp) state[21] = 0x10
        state[23] = when (model) {
            Model.Dke, Model.Ckp, Model.Ac32 -> 0x01
            // Real JKE captures (IRremoteESP8266 #525) send 0x80; the library writes 0x81.
            Model.Jke -> 0x80
            Model.Rkr -> 0x89
            Model.Nke, Model.Lke -> 0x81
        }
        state[STATE_LENGTH - 1] = checksum(state)
        return state
    }

    /** Fixed low bits of byte 13 per model (IRremoteESP8266 `setModel()`). */
    private fun modelByte13(model: Model): Int = when (model) {
        Model.Lke -> 0x02
        Model.Rkr -> 0x08
        Model.Dke, Model.Nke, Model.Jke, Model.Ckp, Model.Ac32 -> 0x00
    }

    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Auto -> 0
        AcMode.Dry -> 2
        AcMode.Cool -> 3
        AcMode.Heat -> 4
        AcMode.Fan -> 6
    }

    /** IRremoteESP8266 `convertFan`; Auto is 7. */
    private fun fanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Low -> 1
        FanSpeed.Medium -> 2
        FanSpeed.High -> 3
        FanSpeed.Auto -> 7
    }

    /** 0xF4 plus the sum of bytes 0–25, low 8 bits. */
    internal fun checksum(state: IntArray): Int {
        var sum = CHECKSUM_INIT
        for (i in 0 until STATE_LENGTH - 1) sum += state[i]
        return sum and 0xFF
    }

    private fun stateToPattern(state: IntArray): IntArray {
        val out = ArrayList<Int>(2 * 3 + STATE_LENGTH * 16 + 1)
        for ((start, end) in listOf(0 to SECTION1_LENGTH, SECTION1_LENGTH to STATE_LENGTH)) {
            if (start > 0) out.add(SECTION_GAP)
            out.add(HDR_MARK)
            out.add(HDR_SPACE)
            for (i in start until end) {
                for (bit in 0 until 8) {
                    out.add(BIT_MARK)
                    out.add(if ((state[i] ushr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE)
                }
            }
            out.add(BIT_MARK)
        }
        return out.toIntArray()
    }

    // PANASONIC_AC32 (IRremoteESP8266 `IRPanasonicAc32`, `sendPanasonicAC32`).

    private const val AC32_HDR_MARK = 3543
    private const val AC32_HDR_SPACE = 3450
    private const val AC32_BIT_MARK = 920
    private const val AC32_ONE_SPACE = 2575
    private const val AC32_ZERO_SPACE = 828
    private const val AC32_SECTION_GAP = 13_946
    private const val AC32_SWING_V_MIDDLE = 0x3
    private const val AC32_SWING_V_AUTO = 0x7

    /**
     * The 32-bit state, byte 0 lowest. Byte 0: bit 7 and bit 2 always set, vertical vane in
     * bits 4–6, horizontal swing in bit 3. Byte 1 is always 0x36. Byte 2: temp − 15, fan.
     * Byte 3: mode, then "keep power" (0 means toggle).
     */
    internal fun ac32StateFor(
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean,
        powerChanged: Boolean
    ): Long {
        val byte0 = 0x84 or ((if (swingOn) AC32_SWING_V_AUTO else AC32_SWING_V_MIDDLE) shl 4) or (if (swingOn) 0x08 else 0)
        val temp = temperatureC.coerceIn(MIN_TEMP, MAX_TEMP) - (MIN_TEMP - 1)
        val byte2 = (ac32FanBits(fan) shl 4) or temp
        val byte3 = ac32ModeBits(mode) or (if (powerChanged) 0 else 0x08)
        return byte0.toLong() or (0x36L shl 8) or (byte2.toLong() shl 16) or (byte3.toLong() shl 24)
    }

    private fun ac32ModeBits(mode: AcMode): Int = when (mode) {
        AcMode.Fan -> 1
        AcMode.Cool -> 2
        AcMode.Dry -> 3
        AcMode.Heat -> 4
        AcMode.Auto -> 6
    }

    private fun ac32FanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Low -> 3
        FanSpeed.Medium -> 4
        FanSpeed.High -> 5
        FanSpeed.Auto -> 0xF
    }

    /**
     * Two sections (high 16 bits, then low 16 bits). Each sends its bytes doubled,
     * low byte first (LL LL HH HH, LSB-first), twice, then a bare header and footer mark.
     */
    internal fun ac32Pattern(state: Long): IntArray {
        val out = ArrayList<Int>(2 * (2 * (2 + 64) + 4))
        for (section in 0 until 2) {
            if (section > 0) out.add(AC32_SECTION_GAP)
            val value = ((state ushr (16 * (1 - section))) and 0xFFFF).toInt()
            val lo = value and 0xFF
            val hi = value ushr 8
            repeat(2) {
                out.add(AC32_HDR_MARK)
                out.add(AC32_HDR_SPACE)
                for (byte in intArrayOf(lo, lo, hi, hi)) {
                    for (bit in 0 until 8) {
                        out.add(AC32_BIT_MARK)
                        out.add(if ((byte ushr bit) and 1 == 1) AC32_ONE_SPACE else AC32_ZERO_SPACE)
                    }
                }
            }
            out.add(AC32_HDR_MARK)
            out.add(AC32_HDR_SPACE)
            out.add(AC32_BIT_MARK)
        }
        return out.toIntArray()
    }
}

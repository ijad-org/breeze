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
 * a few fixed bytes, so pairing offers one ON and one OFF probe per [Model].
 * CKP remotes toggle power instead of setting it, so they aren't covered.
 * Swing on is the vane's auto (swing) setting, swing off its middle position;
 * DKE and RKR also swing horizontally.
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
        Rkr
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

    /** Pairing probes, two per model: Cool 24° Auto ON, then OFF. See [modelFor]. */
    fun powerProbeVariants(): List<IntArray> = Model.entries.flatMap { m ->
        listOf(
            patternFor(m, true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
            patternFor(m, false, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false)
        )
    }

    /** Maps a paired probe index to its model. Unknown indices (e.g. older placeholder pairings) use [Model.Dke]. */
    fun modelFor(configIndex: Int): Model =
        Model.entries.getOrNull(configIndex / 2)?.takeIf { configIndex >= 0 } ?: Model.Dke

    fun patternFor(
        model: Model,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray = stateToPattern(stateFor(model, poweredOn, mode, temperatureC, fan, swingOn))

    /** The 27-byte state with its checksum. */
    internal fun stateFor(
        model: Model,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray {
        // IRremoteESP8266 `setMode()`: Fan mode always sends 27°.
        val temp = if (mode == AcMode.Fan) FAN_MODE_TEMP else temperatureC.coerceIn(MIN_TEMP, MAX_TEMP)
        val state = BASE_STATE.copyOf(STATE_LENGTH)
        state[13] = (modeBits(mode) shl 4) or modelByte13(model) or (if (poweredOn) 1 else 0)
        state[14] = temp shl 1
        state[16] = ((fanBits(fan) + FAN_DELTA) shl 4) or (if (swingOn) SWING_V_AUTO else SWING_V_MIDDLE)
        state[17] = when (model) {
            Model.Dke, Model.Rkr -> if (swingOn) SWING_H_AUTO else SWING_H_MIDDLE
            Model.Nke, Model.Lke -> SWING_H_MIDDLE
            Model.Jke -> 0x00
        }
        state[23] = when (model) {
            Model.Dke -> 0x01
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
        Model.Dke, Model.Nke, Model.Jke -> 0x00
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
}

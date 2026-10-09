package com.ijad.breeze.ir

import com.ijad.breeze.data.AcMode
import com.ijad.breeze.data.FanSpeed

/**
 * Daikin AC IR encoder for the two most common full-state Daikin protocols.
 *
 * Constants, byte layouts and reset states from crankyoldgit/IRremoteESP8266
 * `ir_Daikin.{h,cpp}` (`DAIKIN` and `DAIKIN216`), cross-checked against
 * blafois/Daikin-IR-Reverse. Every press sends the whole state (power, mode,
 * temp, fan, swing), so there are no toggle frames.
 *
 * Bytes are sent LSB-first at 38 kHz. Each section ends in a checksum byte
 * (sum of the section's other bytes, mod 256).
 */
object DaikinIrCodec {
    const val FREQUENCY_HZ = 38_000

    enum class Protocol {
        /** `DAIKIN` 280-bit, 3 sections (ARC423, ARC433, ARC470, ARC480, ARC484 remotes …). */
        Arc4xx,

        /** `DAIKIN216` 216-bit, 2 sections (ARC433B69 remote). */
        Arc433b69
    }

    private const val MIN_TEMP = 10
    private const val MAX_TEMP = 32

    /**
     * Pairing probes, two per protocol: Cool 24° Auto ON, then OFF.
     * The chosen probe index is the device's `configIndex`; see [protocolFor].
     */
    fun powerProbeVariants(): List<IntArray> = Protocol.entries.flatMap { p ->
        listOf(
            patternFor(p, true, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false),
            patternFor(p, false, AcMode.Cool, 24, FanSpeed.Auto, swingOn = false)
        )
    }

    /** Maps a paired probe index to its protocol. Unknown indices (e.g. older placeholder pairings) use [Protocol.Arc4xx]. */
    fun protocolFor(configIndex: Int): Protocol =
        Protocol.entries.getOrNull(configIndex / 2)?.takeIf { configIndex >= 0 } ?: Protocol.Arc4xx

    fun patternFor(
        protocol: Protocol,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray {
        val state = stateFor(protocol, poweredOn, mode, temperatureC, fan, swingOn)
        return when (protocol) {
            Protocol.Arc4xx -> arc4xxPattern(state)
            Protocol.Arc433b69 -> arc433b69Pattern(state)
        }
    }

    /** The raw state bytes (0–255) with checksums filled in. */
    internal fun stateFor(
        protocol: Protocol,
        poweredOn: Boolean,
        mode: AcMode,
        temperatureC: Int,
        fan: FanSpeed,
        swingOn: Boolean
    ): IntArray {
        val temp = temperatureC.coerceIn(MIN_TEMP, MAX_TEMP)
        val power = if (poweredOn) 1 else 0
        val modeAndPower = (modeBits(mode) shl 4) or power
        val fanAndSwing = (fanBits(fan) shl 4) or (if (swingOn) 0xF else 0x0)
        return when (protocol) {
            Protocol.Arc4xx -> IntArray(35).also { s ->
                s.put(0, 0x11, 0xDA, 0x27, 0x00, 0xC5)
                s.put(8, 0x11, 0xDA, 0x27, 0x00, 0x42)       // 13–14: clock 00:00, unset
                s.put(16, 0x11, 0xDA, 0x27)
                s[21] = modeAndPower or 0x08                 // bit 3 is always set
                s[22] = temp * 2                             // half-degree steps
                s[24] = fanAndSwing
                s.put(26, 0x00, 0x06, 0x60)                  // on/off timers unused (0x600)
                s[31] = 0xC0
                s.checksum(0, 8)
                s.checksum(8, 8)
                s.checksum(16, 19)
            }
            Protocol.Arc433b69 -> IntArray(27).also { s ->
                s.put(0, 0x11, 0xDA, 0x27, 0xF0)
                s.put(8, 0x11, 0xDA, 0x27)
                s[13] = modeAndPower
                s[14] = temp shl 1
                s[16] = fanAndSwing
                s[23] = 0xC0
                s.checksum(0, 8)
                s.checksum(8, 19)
            }
        }
    }

    private fun modeBits(mode: AcMode): Int = when (mode) {
        AcMode.Auto -> 0b000
        AcMode.Dry -> 0b010
        AcMode.Cool -> 0b011
        AcMode.Heat -> 0b100
        AcMode.Fan -> 0b110
    }

    /** Daikin speeds 1–5 are sent as 3–7; Auto is 0xA. */
    private fun fanBits(fan: FanSpeed): Int = when (fan) {
        FanSpeed.Low -> 3
        FanSpeed.Medium -> 5
        FanSpeed.High -> 7
        FanSpeed.Auto -> 0xA
    }

    private fun IntArray.put(at: Int, vararg bytes: Int) = bytes.copyInto(this, at)

    /** Last byte of the section = sum of the others, mod 256. */
    private fun IntArray.checksum(start: Int, length: Int) {
        var sum = 0
        for (i in start until start + length - 1) sum += this[i]
        this[start + length - 1] = sum and 0xFF
    }

    // ARC4xx: a 5-bit zero leader, then sections of 8, 8 and 19 bytes.
    private const val ARC4XX_HDR_MARK = 3650
    private const val ARC4XX_HDR_SPACE = 1623
    private const val ARC4XX_BIT_MARK = 428
    private const val ARC4XX_ONE_SPACE = 1280
    private const val ARC4XX_ZERO_SPACE = 428
    private const val ARC4XX_GAP = ARC4XX_ZERO_SPACE + 29_000

    private fun arc4xxPattern(state: IntArray): IntArray {
        val out = ArrayList<Int>(584)
        repeat(5) {
            out.add(ARC4XX_BIT_MARK)
            out.add(ARC4XX_ZERO_SPACE)
        }
        out.add(ARC4XX_BIT_MARK)
        out.add(ARC4XX_GAP)
        val sections = listOf(0 until 8, 8 until 16, 16 until 35)
        sections.forEachIndexed { i, range ->
            out.addSection(
                state, range, ARC4XX_HDR_MARK, ARC4XX_HDR_SPACE, ARC4XX_BIT_MARK,
                ARC4XX_ONE_SPACE, ARC4XX_ZERO_SPACE, gap = if (i < sections.lastIndex) ARC4XX_GAP else null
            )
        }
        return out.toIntArray()
    }

    // ARC433B69: sections of 8 and 19 bytes, no leader.
    private const val ARC433B69_HDR_MARK = 3440
    private const val ARC433B69_HDR_SPACE = 1750
    private const val ARC433B69_BIT_MARK = 420
    private const val ARC433B69_ONE_SPACE = 1300
    private const val ARC433B69_ZERO_SPACE = 450
    private const val ARC433B69_GAP = 29_650

    private fun arc433b69Pattern(state: IntArray): IntArray {
        val out = ArrayList<Int>(436)
        out.addSection(
            state, 0 until 8, ARC433B69_HDR_MARK, ARC433B69_HDR_SPACE, ARC433B69_BIT_MARK,
            ARC433B69_ONE_SPACE, ARC433B69_ZERO_SPACE, gap = ARC433B69_GAP
        )
        out.addSection(
            state, 8 until 27, ARC433B69_HDR_MARK, ARC433B69_HDR_SPACE, ARC433B69_BIT_MARK,
            ARC433B69_ONE_SPACE, ARC433B69_ZERO_SPACE, gap = null
        )
        return out.toIntArray()
    }

    /** Header, bytes LSB-first, footer mark, then [gap] (omitted after the last section so the pattern ends on a mark). */
    private fun MutableList<Int>.addSection(
        state: IntArray,
        bytes: IntRange,
        hdrMark: Int,
        hdrSpace: Int,
        bitMark: Int,
        oneSpace: Int,
        zeroSpace: Int,
        gap: Int?
    ) {
        add(hdrMark)
        add(hdrSpace)
        for (i in bytes) for (bit in 0 until 8) {
            add(bitMark)
            add(if ((state[i] ushr bit) and 1 == 1) oneSpace else zeroSpace)
        }
        add(bitMark)
        if (gap != null) add(gap)
    }
}

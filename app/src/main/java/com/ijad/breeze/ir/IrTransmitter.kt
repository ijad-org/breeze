package com.ijad.breeze.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Thin wrapper around [ConsumerIrManager] with has-emitter check
 * and light haptic feedback on successful transmit.
 */
class IrTransmitter(context: Context) {
    private val appContext = context.applicationContext
    private val irManager: ConsumerIrManager? =
        appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val hasIrEmitter: Boolean
        get() = irManager?.hasIrEmitter() == true

    data class TransmitResult(
        val success: Boolean,
        val message: String
    )

    fun transmit(frequencyHz: Int, patternMicros: IntArray): TransmitResult {
        val manager = irManager
        if (manager == null || !manager.hasIrEmitter()) {
            return TransmitResult(false, "No IR blaster on this device")
        }
        if (patternMicros.isEmpty()) {
            return TransmitResult(false, "Empty IR pattern")
        }
        return try {
            manager.transmit(frequencyHz, patternMicros)
            hapticTick()
            TransmitResult(true, "Sent")
        } catch (t: Throwable) {
            TransmitResult(false, t.message ?: "Transmit failed")
        }
    }

    private fun hapticTick() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            v.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Throwable) {
            // Ignore haptic failures — IR still sent.
        }
    }
}

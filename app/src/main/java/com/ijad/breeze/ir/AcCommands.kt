package com.ijad.breeze.ir

import com.ijad.breeze.data.AcDevice
import com.ijad.breeze.data.RemoteState

/** Sends full-state / swing commands for a paired device; shared by the remote and timers. */
object AcCommands {
    sealed interface Result {
        data object Sent : Result
        data object NoEmitter : Result
        data class Unsupported(val brandName: String) : Result
        data class Failed(val message: String) : Result
    }

    fun sendState(ir: IrTransmitter, device: AcDevice, state: RemoteState): Result {
        if (!ir.hasIrEmitter) return Result.NoEmitter
        val pattern = BrandIr.controlPattern(
            device.brandId, state.poweredOn, state.mode, state.temperatureC, state.fan
        ) ?: return Result.Unsupported(device.brandName)
        return ir.transmit(pattern.frequencyHz, pattern.micros).toResult()
    }

    fun sendSwing(ir: IrTransmitter, device: AcDevice): Result {
        if (!ir.hasIrEmitter) return Result.NoEmitter
        val pattern = BrandIr.swingPattern(device.brandId) ?: return Result.Unsupported(device.brandName)
        return ir.transmit(pattern.frequencyHz, pattern.micros).toResult()
    }

    /** User-facing message for anything other than [Result.Sent]; null when sent. */
    fun messageFor(result: Result): String? = when (result) {
        Result.Sent -> null
        Result.NoEmitter -> "No IR blaster on this device"
        is Result.Unsupported -> "Codes coming for ${result.brandName}"
        is Result.Failed -> result.message
    }

    private fun IrTransmitter.TransmitResult.toResult(): Result =
        if (success) Result.Sent else Result.Failed(message)
}

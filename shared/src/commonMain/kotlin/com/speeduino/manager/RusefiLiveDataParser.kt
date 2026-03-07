package com.speeduino.manager

import com.speeduino.manager.model.OutputField
import com.speeduino.manager.model.SpeeduinoOutputChannels
import kotlin.math.roundToInt

object RusefiLiveDataParser {
    fun fromOutputChannels(data: ByteArray): SpeeduinoLiveData {
        val minimumSize = 64
        require(data.size >= minimumSize) {
            "rusEFI output channels incompletos: esperado $minimumSize, recebido ${data.size}"
        }

        val secl = fieldInt(data, "seconds", "secl")
        val rpm = fieldInt(data, "rpm", "RPMValue")
        val advance = fieldDouble(data, "advance", "ignitionAdvanceCyl1").roundToInt()
        val mapPressure = fieldDouble(data, "map", "MAPValue").roundToInt()
        val intakeTemp = fieldDouble(data, "intake", "iatRaw").roundToInt()
        val coolantTemp = fieldDouble(data, "coolant", "coolantRaw").roundToInt()
        val tps = fieldDouble(data, "tps", "TPSValue").roundToInt()
        val batteryVoltage = fieldDouble(data, "batteryVoltage", "VBatt")
        val lambda = fieldDouble(data, "lambdaValue")
        val o2 = if (lambda > 0.0) {
            (lambda * 14.7).roundToInt()
        } else {
            fieldDouble(data, "afr", "AFRValue").roundToInt()
        }
        val engineStatus = if (rpm > 0) 0x01 else 0x00
        val sparkStatus = if (rpm > 0) 0x01 else 0x00

        return SpeeduinoLiveData(
            secl = secl,
            rpm = rpm,
            coolantTemp = coolantTemp,
            intakeTemp = intakeTemp,
            mapPressure = mapPressure,
            tps = tps,
            batteryVoltage = batteryVoltage,
            advance = advance,
            o2 = o2,
            engineStatus = engineStatus,
            sparkStatus = sparkStatus,
            outputChannelBlockSize = data.size,
            outputChannelData = data.copyOf(),
        )
    }

    private fun fieldInt(data: ByteArray, vararg names: String): Int {
        return findField(data.size, *names)?.parseInt(data) ?: 0
    }

    private fun fieldDouble(data: ByteArray, vararg names: String): Double {
        return findField(data.size, *names)?.parse(data) ?: 0.0
    }

    private fun findField(blockSize: Int, vararg names: String): OutputField? {
        return names.firstNotNullOfOrNull { SpeeduinoOutputChannels.getField(blockSize, it) }
    }
}

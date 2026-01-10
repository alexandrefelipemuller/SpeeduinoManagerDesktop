package com.speeduino.manager

import com.speeduino.manager.model.SpeeduinoOutputChannels

object SpeeduinoLiveDataParser {
    fun fromLegacyFrame(data: ByteArray): SpeeduinoLiveData {
        val rpm = u16le(data, 15)

        return SpeeduinoLiveData(
            secl = u8(data, 1),
            rpm = rpm,
            coolantTemp = u8(data, 8) - 40,
            intakeTemp = u8(data, 7) - 40,
            mapPressure = (u8(data, 6) shl 8) or u8(data, 5),
            tps = u8(data, 26),
            batteryVoltage = u8(data, 10) / 10.0,
            advance = u8(data, 25),
            o2 = u8(data, 11),
            engineStatus = u8(data, 3),
            sparkStatus = u8(data, 33)
        )
    }

    fun fromOutputChannels(data: ByteArray): SpeeduinoLiveData {
        val blockSize = data.size
        val secl = fieldInt(data, blockSize, "secl")
        val rpm = fieldInt(data, blockSize, "rpm")
        val coolant = fieldInt(data, blockSize, "coolantRaw")
        val intake = fieldInt(data, blockSize, "iatRaw")
        val map = fieldInt(data, blockSize, "map")
        val tps = fieldInt(data, blockSize, "tps")
        val battery = fieldDouble(data, blockSize, "batteryVoltage")
        val advance = fieldInt(data, blockSize, "advance")
        val o2 = fieldInt(data, blockSize, "afr")
        val engineStatus = fieldInt(data, blockSize, "engine")
        val sparkStatus = fieldInt(data, blockSize, "spark")

        return SpeeduinoLiveData(
            secl = secl,
            rpm = rpm,
            coolantTemp = coolant,
            intakeTemp = intake,
            mapPressure = map,
            tps = tps,
            batteryVoltage = battery,
            advance = advance,
            o2 = o2,
            engineStatus = engineStatus,
            sparkStatus = sparkStatus
        )
    }

    private fun fieldInt(data: ByteArray, blockSize: Int, name: String): Int {
        val field = SpeeduinoOutputChannels.getField(blockSize, name)
        return field?.parseInt(data) ?: 0
    }

    private fun fieldDouble(data: ByteArray, blockSize: Int, name: String): Double {
        val field = SpeeduinoOutputChannels.getField(blockSize, name)
        return field?.parse(data) ?: 0.0
    }

    private fun u8(data: ByteArray, index: Int): Int {
        return data.getOrNull(index)?.toInt()?.and(0xFF) ?: 0
    }

    private fun u16le(data: ByteArray, lsbIndex: Int): Int {
        val lsb = u8(data, lsbIndex)
        val msb = u8(data, lsbIndex + 1)
        return lsb or (msb shl 8)
    }
}

package com.speeduino.manager

object Ms2LiveDataParser {
    private const val MIN_OUTPUT_BLOCK_SIZE = 212

    fun fromOutputChannels(data: ByteArray): SpeeduinoLiveData {
        require(data.size >= MIN_OUTPUT_BLOCK_SIZE) {
            "MS2 output channels incompletos: esperado $MIN_OUTPUT_BLOCK_SIZE, recebido ${data.size}"
        }

        val secl = u16be(data, 0)
        val rpm = u16be(data, 6)
        val advance = s16be(data, 8) / 10
        val sparkStatus = u8(data, 10)
        val engineStatus = u8(data, 11)
        val mapPressure = s16be(data, 18) / 10
        val intakeTemp = tempRawToC(s16be(data, 20))
        val coolantTemp = tempRawToC(s16be(data, 22))
        val tps = s16be(data, 24) / 10
        val batteryVoltage = s16be(data, 26) / 10.0
        val o2 = s16be(data, 28) / 10

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
            outputChannelData = data.copyOf()
        )
    }

    private fun tempRawToC(raw: Int): Int {
        return (((raw / 10.0) - 32.0) * 5.0 / 9.0).toInt()
    }

    private fun u8(data: ByteArray, index: Int): Int {
        return data.getOrNull(index)?.toInt()?.and(0xFF) ?: 0
    }

    private fun u16be(data: ByteArray, index: Int): Int {
        val msb = u8(data, index)
        val lsb = u8(data, index + 1)
        return (msb shl 8) or lsb
    }

    private fun s16be(data: ByteArray, index: Int): Int {
        return u16be(data, index).let { raw ->
            if (raw and 0x8000 != 0) raw - 0x10000 else raw
        }
    }
}

package com.speeduino.manager.model

/**
 * Secondary Serial (Serial3) configuration - Page 9, byte 0 (bits)
 *
 * Bits layout (U08 at offset 0):
 * - bit 0: enable_secondarySerial (0 = Disable, 1 = Enable)
 * - bits 3..6: secondarySerialProtocol (0..15)
 */
data class SecondarySerialConfig(
    val enabled: Boolean,
    val protocol: SecondarySerialProtocol,
    val protocolRaw: Int
) {
    companion object {
        const val PAGE_NUMBER: Int = 9
        const val OFFSET: Int = 0

        fun fromByte(value: Int): SecondarySerialConfig {
            val enabled = (value and 0x01) != 0
            val protocolRaw = (value shr 3) and 0x0F
            val protocol = SecondarySerialProtocol.fromRaw(protocolRaw)
            return SecondarySerialConfig(
                enabled = enabled,
                protocol = protocol,
                protocolRaw = protocolRaw
            )
        }
    }

    fun applyToByte(original: Int): Int {
        var result = original
        result = if (enabled) {
            result or 0x01
        } else {
            result and 0xFE
        }

        // Clear bits 3..6
        result = result and (0xFF xor (0x0F shl 3))

        val rawProtocol = if (protocol == SecondarySerialProtocol.UNKNOWN) {
            protocolRaw.coerceIn(0, 0x0F)
        } else {
            protocol.rawValue.coerceIn(0, 0x0F)
        }
        result = result or ((rawProtocol and 0x0F) shl 3)
        return result
    }
}

enum class SecondarySerialProtocol(val rawValue: Int) {
    GENERIC_FIXED(0),
    GENERIC_INI(1),
    CAN(2),
    MSDROID(3),
    REALDASH(4),
    TUNERSTUDIO(5),
    UNKNOWN(-1);

    companion object {
        fun fromRaw(value: Int): SecondarySerialProtocol {
            return values().firstOrNull { it.rawValue == value } ?: UNKNOWN
        }
    }
}

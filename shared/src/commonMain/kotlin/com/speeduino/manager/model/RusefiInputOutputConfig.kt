package com.speeduino.manager.model

data class RusefiIoEntry(
    val label: String,
    val value: String,
)

data class RusefiInputOutputSnapshot(
    val inputs: List<RusefiIoEntry>,
    val fuelOutputs: List<RusefiIoEntry>,
    val ignitionOutputs: List<RusefiIoEntry>,
    val auxiliaryOutputs: List<RusefiIoEntry>,
)

object RusefiInputOutputConfig {
    fun fromMainPage(data: ByteArray, schemaId: String = "rusefi-main"): RusefiInputOutputSnapshot {
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val minimumSize = if (isF407Discovery) 1640 else 1668
        require(data.size >= minimumSize) { "rusEFI page 0x0000 deve ter pelo menos $minimumSize bytes" }

        val inputs = buildList {
            add(RusefiIoEntry("Primary Trigger Input", pinLabel(readU16Le(data, if (isF407Discovery) 740 else 748), "Input")))
            add(RusefiIoEntry("Secondary Trigger Input", pinLabel(readU16Le(data, if (isF407Discovery) 742 else 750), "Input")))
        }

        val fuelOutputs = buildList {
            for (index in 0 until 12) {
                val offset = (if (isF407Discovery) 648 else 656) + (index * 2)
                add(RusefiIoEntry("Injection Output ${index + 1}", pinLabel(readU16Le(data, offset), "Output")))
            }
        }

        val ignitionOutputs = buildList {
            for (index in 0 until 12) {
                val offset = (if (isF407Discovery) 672 else 680) + (index * 2)
                add(RusefiIoEntry("Ignition Output ${index + 1}", pinLabel(readU16Le(data, offset), "Output")))
            }
        }

        val auxiliaryOutputs = buildList {
            add(RusefiIoEntry("Fuel Pump", pinWithMode(readU16Le(data, if (isF407Discovery) 698 else 706), readU8(data, if (isF407Discovery) 700 else 708))))
            add(RusefiIoEntry("MIL / Check Engine", pinWithMode(readU16Le(data, if (isF407Discovery) 702 else 710), readU8(data, if (isF407Discovery) 704 else 712))))
            add(RusefiIoEntry("Alternator Control", pinWithMode(readU16Le(data, if (isF407Discovery) 708 else 716), readU8(data, if (isF407Discovery) 710 else 718))))
            add(RusefiIoEntry("Main Relay", pinWithMode(readU16Le(data, if (isF407Discovery) 52 else 56), readU8(data, if (isF407Discovery) 780 else 788))))
            add(RusefiIoEntry("Tach Output", pinWithMode(readU16Le(data, if (isF407Discovery) 64 else 68), readU8(data, if (isF407Discovery) 66 else 70))))
            add(RusefiIoEntry("O2 Heater", pinWithMode(readU16Le(data, if (isF407Discovery) 762 else 770), readU8(data, if (isF407Discovery) 764 else 772))))
            add(RusefiIoEntry("Idle Solenoid", pinWithMode(readU16Le(data, if (isF407Discovery) 628 else 636), readU8(data, if (isF407Discovery) 634 else 642))))
            add(RusefiIoEntry("Boost Solenoid", pinWithMode(readU16Le(data, if (isF407Discovery) 462 else 466), readU8(data, if (isF407Discovery) 464 else 468))))
            add(RusefiIoEntry("Fan Output", pinWithMode(readU16Le(data, if (isF407Discovery) 496 else 500), readU8(data, if (isF407Discovery) 498 else 502))))
            add(RusefiIoEntry("VVT Solenoid 1", pinLabel(readU16Le(data, if (isF407Discovery) 1636 else 1664), "Output")))
            add(RusefiIoEntry("VVT Solenoid 2", pinLabel(readU16Le(data, if (isF407Discovery) 1638 else 1666), "Output")))
        }

        return RusefiInputOutputSnapshot(
            inputs = inputs,
            fuelOutputs = fuelOutputs,
            ignitionOutputs = ignitionOutputs,
            auxiliaryOutputs = auxiliaryOutputs,
        )
    }

    private fun pinWithMode(pin: Int, mode: Int): String {
        val modeLabel = when (mode and 0x03) {
            1 -> "Open drain"
            2 -> "Inverted"
            else -> "Push-pull"
        }
        return "${pinLabel(pin, "Output")} ($modeLabel)"
    }

    private fun pinLabel(pin: Int, prefix: String): String {
        return if (pin == 0) "Not assigned" else "$prefix $pin"
    }

    private fun readU8(data: ByteArray, offset: Int): Int = data[offset].toInt() and 0xFF

    private fun readU16Le(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
    }
}

package com.speeduino.manager.model

/**
 * Central registry for supported ECU families.
 *
 * PR2 wires MS3 only up to handshake/family detection. Read/write/runtime
 * support remains gated by capabilities until later PRs land.
 */
object EcuDefinitionRegistry {
    private val providers: List<EcuDefinitionProvider> = listOf(
        SpeeduinoDefinitionProvider,
        MegaSpeedDefinitionProvider,
        Ms2DefinitionProvider,
        Ms3DefinitionProvider,
        RusefiDefinitionProvider,
    )

    fun resolve(signature: String, productString: String? = null): EcuDefinition {
        val provider = providers.firstOrNull { it.supports(signature, productString) }
            ?: throw UnsupportedFirmwareException("Unsupported ECU signature: $signature")
        return provider.resolve(signature, productString)
    }

    fun getSupportedFamilies(): List<String> = listOf(
        "speeduino YYYYMM",
        "MS2Extra MegaSpeed",
        "MS2Extra comms342h2",
        "MS3 Format 0523.15",
        "rusEFI master.YYYY.MM.DD.board.hash",
    )
}

object MegaSpeedDefinitionProvider : EcuDefinitionProvider {
    override val family: EcuFamily = EcuFamily.MEGASPEED

    private val megaSpeedPageCatalog = listOf(
        EcuPageDescriptor(0x04, 1024, "Fuel / AFR / Core Settings"),
        EcuPageDescriptor(0x05, 1024, "Idle / Auxiliary Settings"),
        EcuPageDescriptor(0x08, 1024, "Ignition Table 3 / Test"),
        EcuPageDescriptor(0x09, 1024, "VE Tables"),
        EcuPageDescriptor(0x0A, 1024, "Ignition Tables 1/2"),
        EcuPageDescriptor(0x0B, 1024, "Trim / Secondary Tables"),
        EcuPageDescriptor(0x0C, 1024, "Spare Config"),
    )

    override fun supports(signature: String, productString: String?): Boolean {
        return signature.trim().startsWith("MS2Extra MegaSpeed", ignoreCase = true)
    }

    override fun resolve(signature: String, productString: String?): EcuDefinition {
        val normalized = signature.trim().replace(Regex("\\s+"), " ")
        return EcuDefinition(
            family = family,
            normalizedSignature = normalized,
            runtime = EcuRuntimeSchema(
                blockSize = 219,
                byteOrder = EcuByteOrder.BIG_ENDIAN,
                schemaId = "megaspeed-ms2x"
            ),
            capabilities = EcuCapabilities(
                supportsModernProtocol = true,
                supportsLegacyProtocol = true,
                supportsPageRead = true,
                supportsPageWrite = true,
                supportsBurn = true,
                supportsLiveData = true
            ),
            tableDefinitions = null,
            pageCatalog = megaSpeedPageCatalog,
            productString = productString,
        )
    }
}

/**
 * Minimal MS2/Extra profile used for the first basic integration.
 */
object Ms2DefinitionProvider : EcuDefinitionProvider {
    override val family: EcuFamily = EcuFamily.MS2

    private val ms2PageCatalog = listOf(
        EcuPageDescriptor(0x04, 1024, "Fuel / AFR Settings"),
        EcuPageDescriptor(0x05, 1024, "General Settings"),
        EcuPageDescriptor(0x08, 1024, "Ignition Table 3"),
        EcuPageDescriptor(0x09, 1024, "VE Tables"),
        EcuPageDescriptor(0x0A, 1024, "Ignition Tables 1/2"),
        EcuPageDescriptor(0x0B, 1024, "Trim Tables"),
        EcuPageDescriptor(0x0C, 1024, "Spare Config"),
    )

    override fun supports(signature: String, productString: String?): Boolean {
        return signature.trim().startsWith("MS2Extra comms", ignoreCase = true)
    }

    override fun resolve(signature: String, productString: String?): EcuDefinition {
        val normalized = signature.trim().replace(Regex("\\s+"), " ")
        return EcuDefinition(
            family = family,
            normalizedSignature = normalized,
            runtime = EcuRuntimeSchema(
                blockSize = 212,
                byteOrder = EcuByteOrder.BIG_ENDIAN,
                schemaId = "ms2extra-342"
            ),
            capabilities = EcuCapabilities(
                supportsModernProtocol = true,
                supportsLegacyProtocol = true,
                supportsPageRead = true,
                supportsPageWrite = true,
                supportsBurn = true,
                supportsLiveData = true
            ),
            tableDefinitions = Ms2TableDefinitions.TABLE_DEFINITIONS,
            pageCatalog = ms2PageCatalog,
            productString = productString,
        )
    }
}

/**
 * Minimal MS3 profile used for the first read-only integration.
 */
object Ms3DefinitionProvider : EcuDefinitionProvider {
    override val family: EcuFamily = EcuFamily.MS3

    private val ms3PageCatalog = listOf(
        EcuPageDescriptor(0x04, 1024, "General Settings"),
        EcuPageDescriptor(0x05, 1024, "Auxiliary Settings"),
        EcuPageDescriptor(0x08, 1024, "Trim Tables"),
        EcuPageDescriptor(0x09, 1024, "VE3 / Spark3"),
        EcuPageDescriptor(0x0A, 1024, "AFR Tables"),
        EcuPageDescriptor(0x0B, 1024, "Spark Trims"),
        EcuPageDescriptor(0x0C, 1024, "VE Tables"),
        EcuPageDescriptor(0x0D, 1024, "Ignition Tables"),
        EcuPageDescriptor(0x12, 1024, "Page 18"),
        EcuPageDescriptor(0x13, 1024, "Table Axes"),
        EcuPageDescriptor(0x15, 1024, "Page 21"),
        EcuPageDescriptor(0x16, 1024, "Config 16"),
        EcuPageDescriptor(0x17, 1024, "Config 17"),
        EcuPageDescriptor(0x18, 1024, "Config 18"),
        EcuPageDescriptor(0x19, 1024, "Config 19"),
        EcuPageDescriptor(0x1A, 1024, "Config 1A"),
        EcuPageDescriptor(0x1B, 1024, "Config 1B"),
        EcuPageDescriptor(0x1C, 1024, "Config 1C"),
        EcuPageDescriptor(0x1D, 1024, "Config 1D"),
        // Logger pages are readable on some MS3 builds, but they are not part of the
        // writable calibration set and restoring them makes the ECU return 0x84.
    )

    override fun supports(signature: String, productString: String?): Boolean {
        return signature.trim().startsWith("MS3 Format", ignoreCase = true)
    }

    override fun resolve(signature: String, productString: String?): EcuDefinition {
        val normalized = signature.trim().replace(Regex("\\s+"), " ")
        return EcuDefinition(
            family = family,
            normalizedSignature = normalized,
            runtime = EcuRuntimeSchema(
                blockSize = 509,
                byteOrder = EcuByteOrder.BIG_ENDIAN,
                schemaId = "ms3-0523"
            ),
            capabilities = EcuCapabilities(
                supportsModernProtocol = true,
                supportsLegacyProtocol = true,
                supportsPageRead = true,
                supportsPageWrite = true,
                supportsBurn = true,
                supportsLiveData = true
            ),
            tableDefinitions = Ms3TableDefinitions.TABLE_DEFINITIONS,
            pageCatalog = ms3PageCatalog,
            productString = productString,
        )
    }
}

object RusefiDefinitionProvider : EcuDefinitionProvider {
    override val family: EcuFamily = EcuFamily.RUSEFI

    private val pageCatalog = listOf(
        EcuPageDescriptor(0x0000, 64020, "Main Configuration / Tables"),
        EcuPageDescriptor(0x0100, 256, "Fast Config / Trigger Aux"),
        EcuPageDescriptor(0x0200, 2048, "Logic / Aux Buffers"),
    )

    private val f407DiscoveryPageCatalog = listOf(
        EcuPageDescriptor(0x0000, 22852, "Main Configuration / Tables"),
        EcuPageDescriptor(0x0100, 256, "Fast Config / Trigger Aux"),
        EcuPageDescriptor(0x0200, 2048, "Logic / Aux Buffers"),
    )

    override fun supports(signature: String, productString: String?): Boolean {
        return signature.trim().startsWith("rusEFI", ignoreCase = true)
    }

    override fun resolve(signature: String, productString: String?): EcuDefinition {
        val normalized = signature.trim().replace(Regex("\\s+"), " ")
        val isF407Discovery = normalized.contains("f407-discovery", ignoreCase = true)
        return EcuDefinition(
            family = family,
            normalizedSignature = normalized,
            runtime = EcuRuntimeSchema(
                blockSize = if (isF407Discovery) 2068 else 2084,
                byteOrder = EcuByteOrder.LITTLE_ENDIAN,
                schemaId = if (isF407Discovery) "rusefi-f407-discovery" else "rusefi-main",
            ),
            capabilities = EcuCapabilities(
                supportsModernProtocol = true,
                supportsLegacyProtocol = false,
                supportsPageRead = true,
                supportsPageWrite = true,
                supportsBurn = true,
                supportsLiveData = true,
            ),
            tableDefinitions = if (isF407Discovery) {
                RusefiF407DiscoveryDefinitions.TABLE_DEFINITIONS
            } else {
                RusefiTableDefinitions.TABLE_DEFINITIONS
            },
            pageCatalog = if (isF407Discovery) f407DiscoveryPageCatalog else pageCatalog,
            productString = productString,
        )
    }
}

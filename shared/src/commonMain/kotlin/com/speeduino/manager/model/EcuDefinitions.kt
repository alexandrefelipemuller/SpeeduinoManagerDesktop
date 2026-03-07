package com.speeduino.manager.model

/**
 * ECU family supported by the shared core.
 *
 * This is the main abstraction seam for supporting non-Speeduino controllers.
 */
enum class EcuFamily {
    SPEEDUINO,
    MS2,
    MEGASPEED,
    MS3,
    RUSEFI,
    UNKNOWN
}

/**
 * Byte order used by ECU payloads and page layouts.
 */
enum class EcuByteOrder {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

/**
 * Runtime/output channel schema metadata for a connected ECU.
 */
data class EcuRuntimeSchema(
    val blockSize: Int,
    val byteOrder: EcuByteOrder,
    val schemaId: String
)

/**
 * Readable configuration/log page exposed by an ECU family.
 */
data class EcuPageDescriptor(
    val id: Int,
    val size: Int,
    val label: String,
)

/**
 * High-level feature flags exposed by a given ECU family/profile.
 */
data class EcuCapabilities(
    val supportsModernProtocol: Boolean,
    val supportsLegacyProtocol: Boolean,
    val supportsPageRead: Boolean,
    val supportsPageWrite: Boolean,
    val supportsBurn: Boolean,
    val supportsLiveData: Boolean
)

/**
 * Normalized ECU profile selected for the active connection.
 *
 * In PR1 this is primarily a wrapper for Speeduino metadata so the rest of the
 * codebase can start depending on ECU-agnostic concepts without changing
 * current behavior.
 */
data class EcuDefinition(
    val family: EcuFamily,
    val normalizedSignature: String,
    val runtime: EcuRuntimeSchema,
    val capabilities: EcuCapabilities,
    val tableDefinitions: TableDefinitions?,
    val pageCatalog: List<EcuPageDescriptor> = emptyList(),
    val productString: String? = null,
)

/**
 * Provider responsible for mapping a detected signature to an ECU definition.
 */
interface EcuDefinitionProvider {
    val family: EcuFamily

    fun supports(signature: String, productString: String? = null): Boolean

    fun resolve(signature: String, productString: String? = null): EcuDefinition
}

package com.speeduino.manager.definition

enum class IniFieldKind {
    SCALAR,
    ARRAY,
    BITS,
    STRING,
    UNKNOWN,
}

data class IniShape(
    val rows: Int,
    val columns: Int = 1,
) {
    val isTable: Boolean get() = rows > 1 && columns > 1
    val totalSize: Int get() = rows * columns
}

data class IniPageDefinition(
    val index: Int,
    val identifierRaw: String? = null,
    val resolvedId: Int? = null,
    val size: Int? = null,
)

data class IniFieldDefinition(
    val name: String,
    val kind: IniFieldKind,
    val dataType: String,
    val offset: Int? = null,
    val shape: IniShape? = null,
    val units: String? = null,
    val scale: Double? = null,
    val translate: Double? = null,
    val section: String,
    val page: Int? = null,
    val rawDefinition: String,
)

data class IniTableDefinition(
    val name: String,
    val dataType: String,
    val offset: Int,
    val shape: IniShape,
    val units: String? = null,
    val scale: Double? = null,
    val translate: Double? = null,
    val section: String,
    val page: Int? = null,
    val rawDefinition: String,
)

data class IniOutputChannelDefinition(
    val name: String,
    val kind: IniFieldKind,
    val dataType: String,
    val offset: Int? = null,
    val shape: IniShape? = null,
    val units: String? = null,
    val scale: Double? = null,
    val translate: Double? = null,
    val rawDefinition: String,
)

data class IniDefinition(
    val sourceName: String,
    val signature: String,
    val family: String,
    val queryCommand: String? = null,
    val versionInfoCommand: String? = null,
    val nPages: Int? = null,
    val pageDefinitions: List<IniPageDefinition> = emptyList(),
    val ochBlockSize: Int? = null,
    val tableDefinitions: List<IniTableDefinition> = emptyList(),
    val outputChannels: List<IniOutputChannelDefinition> = emptyList(),
    val fields: List<IniFieldDefinition> = emptyList(),
)

data class IniCatalogEntry(
    val id: String,
    val family: String,
    val board: String,
    val signaturePattern: String,
    val version: String,
    val url: String,
    val sha256: String,
    val priority: Int = 100,
    val bundled: Boolean = false,
    val minAppVersion: Int? = null,
)


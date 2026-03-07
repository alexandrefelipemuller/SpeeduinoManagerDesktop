package com.speeduino.manager.model

import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.definition.IniFieldDefinition
import com.speeduino.manager.definition.IniOutputChannelDefinition
import kotlin.math.max

object SpeeduinoIniDefinitions {

    data class Catalog(
        val tableDefinitions: TableDefinitions,
        val pageCatalog: List<EcuPageDescriptor>,
        val outputFields: List<OutputField>,
    )

    fun fromIni(definition: IniDefinition): Catalog? {
        if (!definition.signature.startsWith("speeduino", ignoreCase = true)) {
            return null
        }

        val veTable = findTable(definition, "veTable") ?: return null
        val ignitionTable = findTable(definition, "advTable1") ?: return null
        val afrTable = findTable(definition, "afrTable") ?: return null

        val veMetadata = buildTableMetadata(
            field = veTable,
            pageFallback = if (definition.signature.contains("201609")) 1 else 2,
            displayName = "Speeduino VE Table",
            valueType = toDataType(veTable.dataType) ?: DataType.U08,
            valueRange = 0.0..255.0,
            unitsFallback = "%",
        )
        val ignMetadata = buildTableMetadata(
            field = ignitionTable,
            pageFallback = 3,
            displayName = "Speeduino Ignition Table",
            valueType = toDataType(ignitionTable.dataType) ?: DataType.U08,
            valueRange = -40.0..90.0,
            unitsFallback = "deg",
        )
        val afrMetadata = buildTableMetadata(
            field = afrTable,
            pageFallback = 5,
            displayName = "Speeduino AFR Table",
            valueType = toDataType(afrTable.dataType) ?: DataType.U08,
            valueRange = 0.0..30.0,
            unitsFallback = "AFR",
        )

        val blockSize = definition.ochBlockSize ?: inferBlockSize(definition.outputChannels)
        val outputFields = buildOutputFields(definition)

        return Catalog(
            tableDefinitions = TableDefinitions(
                veTable = veMetadata,
                ignitionTable = ignMetadata,
                afrTable = afrMetadata,
                boostTable = null,
                ochBlockSize = blockSize,
                nPages = definition.nPages ?: definition.pageDefinitions.size,
                era = SpeeduinoTableDefinitions.detectFirmwareEra(definition.signature),
                family = EcuFamily.SPEEDUINO,
                byteOrder = EcuByteOrder.LITTLE_ENDIAN,
            ),
            pageCatalog = definition.pageDefinitions.map { page ->
                EcuPageDescriptor(
                    id = page.resolvedId ?: (page.index + 1),
                    size = page.size ?: 0,
                    label = "Page ${page.index + 1}",
                )
            },
            outputFields = outputFields,
        )
    }

    private fun findTable(definition: IniDefinition, name: String): IniFieldDefinition? {
        return definition.fields.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    private fun buildTableMetadata(
        field: IniFieldDefinition,
        pageFallback: Int,
        displayName: String,
        valueType: DataType,
        valueRange: ClosedRange<Double>,
        unitsFallback: String,
    ): TableMetadata {
        val shape = field.shape ?: error("Table ${field.name} sem shape")
        val offset = resolveOffset(field) ?: 0
        return TableMetadata(
            name = displayName,
            page = field.page ?: pageFallback,
            offset = offset,
            totalSize = inferTotalSize(field, offset),
            valuesShape = shape.rows to shape.columns,
            valuesOffset = 0,
            rpmBinsOffset = resolveAxisOffset(field.page, field.name, "rpm"),
            loadBinsOffset = resolveAxisOffset(field.page, field.name, "load"),
            valueType = valueType,
            valueRange = valueRange,
            rpmRange = 0..30000,
            loadRange = 0.0..700.0,
            units = field.units?.takeIf { it.isNotBlank() } ?: unitsFallback,
            scale = field.scale ?: 1.0,
            translate = field.translate ?: 0.0,
        )
    }

    private fun resolveAxisOffset(page: Int?, tableName: String, axis: String): Int {
        return when {
            axis == "rpm" && tableName.equals("veTable", ignoreCase = true) -> 256
            axis == "load" && tableName.equals("veTable", ignoreCase = true) -> 272
            axis == "rpm" && tableName.equals("advTable1", ignoreCase = true) -> 256
            axis == "load" && tableName.equals("advTable1", ignoreCase = true) -> 272
            axis == "rpm" && tableName.equals("afrTable", ignoreCase = true) -> 256
            axis == "load" && tableName.equals("afrTable", ignoreCase = true) -> 272
            else -> 0
        }
    }

    private fun inferTotalSize(field: IniFieldDefinition, offset: Int): Int {
        val shape = field.shape ?: return 0
        val valueSize = dataTypeSize(field.dataType)
        return when {
            field.name.equals("veTable", ignoreCase = true) -> 288
            field.name.equals("advTable1", ignoreCase = true) -> 288
            field.name.equals("afrTable", ignoreCase = true) -> 288
            else -> offset + (shape.totalSize * valueSize)
        }
    }

    private fun resolveOffset(field: IniFieldDefinition): Int? {
        field.offset?.let { return it }
        val tokens = field.rawDefinition.split(',').map { it.trim() }
        val rawOffset = tokens.getOrNull(2) ?: return null
        return rawOffset.toIntOrNull()
    }

    private fun inferBlockSize(fields: List<IniOutputChannelDefinition>): Int {
        return fields.maxOfOrNull { field ->
            val offset = field.offset ?: 0
            offset + dataTypeSize(field.dataType) * max(1, field.shape?.totalSize ?: 1)
        } ?: 0
    }

    private fun buildOutputFields(definition: IniDefinition): List<OutputField> {
        val fields = definition.outputChannels.mapNotNull { field ->
            val type = toDataType(field.dataType) ?: return@mapNotNull null
            val offset = field.offset ?: return@mapNotNull null
            OutputField(
                name = field.name,
                offset = offset,
                type = type,
                scale = field.scale ?: 1.0,
                translate = field.translate ?: 0.0,
                units = field.units ?: "",
                byteOrder = EcuByteOrder.LITTLE_ENDIAN,
            )
        }.toMutableList()

        val status2 = fields.firstOrNull { it.name == "status2" }
        if (status2 != null && fields.none { it.name == "spark" }) {
            fields += status2.copy(name = "spark")
        }
        val secl = fields.firstOrNull { it.name == "secl" }
        if (secl != null && fields.none { it.name == "seconds" }) {
            fields += secl.copy(name = "seconds")
        }
        return fields
    }

    private fun toDataType(raw: String): DataType? {
        return when (raw.trim().uppercase()) {
            "U08" -> DataType.U08
            "U16" -> DataType.U16
            "S08" -> DataType.S08
            "S16" -> DataType.S16
            else -> null
        }
    }

    private fun dataTypeSize(raw: String): Int {
        return when (raw.trim().uppercase()) {
            "U16", "S16" -> 2
            else -> 1
        }
    }
}

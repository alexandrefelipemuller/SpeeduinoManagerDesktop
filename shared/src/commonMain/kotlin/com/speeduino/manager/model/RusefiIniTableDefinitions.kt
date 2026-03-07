package com.speeduino.manager.model

import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.definition.IniOutputChannelDefinition
import com.speeduino.manager.definition.IniTableDefinition

object RusefiIniTableDefinitions {
    data class Catalog(
        val tableDefinitions: TableDefinitions,
        val pageCatalog: List<EcuPageDescriptor>,
        val veTable: RusefiTableDefinitions.TableLayout,
        val ignitionTable: RusefiTableDefinitions.TableLayout,
        val afrTable: RusefiTableDefinitions.TableLayout,
        val outputFields: List<OutputField>,
    )

    fun fromIni(definition: IniDefinition): Catalog? {
        if (!definition.signature.startsWith("rusEFI", ignoreCase = true)) {
            return null
        }

        val ignitionTable = buildLayout(
            definition = definition,
            tableName = "ignitionTable",
            rpmAxisName = "ignRpmBins",
            loadAxisName = "ignLoadBins",
            fallbackRpmAxisName = "ignitionRpmBins",
            fallbackLoadAxisName = "ignitionLoadBins",
            valueType = DataType.S16,
            defaultScale = 0.1,
            defaultUnits = "deg",
            valueRange = -20.0..90.0,
        ) ?: return null

        val veTable = buildLayout(
            definition = definition,
            tableName = "veTable",
            rpmAxisName = "veRpmBins",
            loadAxisName = "veLoadBins",
            fallbackRpmAxisName = "veRpmBins1",
            fallbackLoadAxisName = "veLoadBins1",
            valueType = DataType.U16,
            defaultScale = 0.1,
            defaultUnits = "%",
            valueRange = 0.0..999.0,
        ) ?: return null

        val afrTable = buildLayout(
            definition = definition,
            tableName = "lambdaTable",
            rpmAxisName = "lambdaRpmBins",
            loadAxisName = "lambdaLoadBins",
            fallbackRpmAxisName = "afrRpmBins",
            fallbackLoadAxisName = "afrLoadBins",
            valueType = DataType.U08,
            defaultScale = 0.1,
            defaultUnits = "AFR",
            valueRange = 10.0..25.5,
        ) ?: return null

        val blockSize = definition.ochBlockSize ?: 2084
        return Catalog(
            tableDefinitions = TableDefinitions(
                veTable = veTable.metadata,
                ignitionTable = ignitionTable.metadata,
                afrTable = afrTable.metadata,
                boostTable = null,
                ochBlockSize = blockSize,
                nPages = definition.nPages ?: definition.pageDefinitions.size,
                era = FirmwareEra.RUSEFI,
                family = EcuFamily.RUSEFI,
                byteOrder = EcuByteOrder.LITTLE_ENDIAN,
            ),
            pageCatalog = definition.pageDefinitions.mapNotNull { page ->
                val pageId = page.resolvedId ?: return@mapNotNull null
                EcuPageDescriptor(
                    id = pageId,
                    size = page.size ?: 0,
                    label = "Page ${page.index + 1}",
                )
            },
            veTable = veTable,
            ignitionTable = ignitionTable,
            afrTable = afrTable,
            outputFields = buildOutputFields(definition.outputChannels),
        )
    }

    private fun buildLayout(
        definition: IniDefinition,
        tableName: String,
        rpmAxisName: String,
        loadAxisName: String,
        fallbackRpmAxisName: String,
        fallbackLoadAxisName: String,
        valueType: DataType,
        defaultScale: Double,
        defaultUnits: String,
        valueRange: ClosedRange<Double>,
    ): RusefiTableDefinitions.TableLayout? {
        val table = definition.tableDefinitions.firstOrNull { it.name.equals(tableName, ignoreCase = true) } ?: return null
        val rpmAxis = findAxis(definition, rpmAxisName, fallbackRpmAxisName) ?: return null
        val loadAxis = findAxis(definition, loadAxisName, fallbackLoadAxisName) ?: return null
        val pageId = resolvePageId(definition, table.page) ?: 0x0000
        val scale = table.scale ?: defaultScale
        val translate = table.translate ?: 0.0
        val totalSize = table.shape.totalSize * dataTypeSize(valueType)

        return RusefiTableDefinitions.TableLayout(
            metadata = TableMetadata(
                name = "rusEFI ${tableName.removeSuffix("Table").replaceFirstChar { it.uppercase() }}",
                page = pageId,
                offset = table.offset,
                totalSize = totalSize,
                valuesShape = table.shape.rows to table.shape.columns,
                valuesOffset = 0,
                rpmBinsOffset = rpmAxis.offset,
                loadBinsOffset = loadAxis.offset,
                valueType = valueType,
                valueRange = valueRange,
                rpmRange = 0..18000,
                loadRange = 0.0..1000.0,
                units = table.units ?: defaultUnits,
                scale = scale,
                translate = translate,
            ),
            valueByteOrder = EcuByteOrder.LITTLE_ENDIAN,
            rpmAxis = rpmAxis,
            loadAxis = loadAxis,
        )
    }

    private fun findAxis(
        definition: IniDefinition,
        primaryName: String,
        fallbackName: String,
    ): RusefiTableDefinitions.AxisLayout? {
        val field = definition.fields.firstOrNull { it.name.equals(primaryName, ignoreCase = true) }
            ?: definition.fields.firstOrNull { it.name.equals(fallbackName, ignoreCase = true) }
            ?: return null
        val count = field.shape?.rows ?: return null
        val offset = field.offset ?: return null
        val pageId = resolvePageId(definition, field.page) ?: 0x0000
        val dataType = normalizeDataType(field.dataType) ?: return null
        return RusefiTableDefinitions.AxisLayout(
            tableId = pageId,
            offset = offset,
            count = count,
            dataType = dataType,
            scale = field.scale ?: 1.0,
        )
    }

    private fun resolvePageId(definition: IniDefinition, pageNumber: Int?): Int? {
        val page = pageNumber ?: return null
        return definition.pageDefinitions.getOrNull(page - 1)?.resolvedId
            ?: definition.pageDefinitions.firstOrNull { it.index == page }?.resolvedId
    }

    private fun normalizeDataType(rawType: String): DataType? {
        return when (rawType.trim().uppercase()) {
            "U08" -> DataType.U08
            "S08" -> DataType.S08
            "U16" -> DataType.U16
            "S16" -> DataType.S16
            else -> null
        }
    }

    private fun buildOutputFields(outputChannels: List<IniOutputChannelDefinition>): List<OutputField> {
        val byName = linkedMapOf<String, OutputField>()
        outputChannels.forEach { channel ->
            val offset = channel.offset ?: return@forEach
            val dataType = normalizeDataType(channel.dataType) ?: return@forEach
            val field = OutputField(
                name = channel.name,
                offset = offset,
                type = dataType,
                scale = channel.scale ?: 1.0,
                translate = channel.translate ?: 0.0,
                units = channel.units ?: "",
                byteOrder = EcuByteOrder.LITTLE_ENDIAN,
            )
            byName.putIfAbsent(field.name, field)
            aliasesFor(channel).forEach { alias ->
                byName.putIfAbsent(alias, field.copy(name = alias))
            }
        }
        return byName.values.toList()
    }

    private fun aliasesFor(channel: IniOutputChannelDefinition): List<String> {
        return when (channel.name.lowercase()) {
            "rpmvalue" -> listOf("rpm")
            "coolant" -> listOf("coolantRaw")
            "intake" -> listOf("iatRaw")
            "tpsvalue" -> listOf("tps")
            "mapvalue" -> listOf("map")
            "vbatt" -> listOf("batteryVoltage")
            "afrvalue" -> listOf("afr")
            "ignitionadvancecyl1" -> listOf("advance")
            else -> emptyList()
        }
    }

    private fun dataTypeSize(type: DataType): Int {
        return when (type) {
            DataType.U08, DataType.S08 -> 1
            DataType.U16, DataType.S16 -> 2
        }
    }
}

package com.speeduino.manager.model

import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.definition.IniFieldDefinition
import com.speeduino.manager.definition.IniTableDefinition
import kotlin.math.roundToInt

object MegaSpeedIniTableDefinitions {
    data class AxisLayout(
        val pageId: Int,
        val offset: Int,
        val count: Int,
        val dataType: DataType,
        val scale: Double = 1.0,
    )

    data class TableLayout(
        val metadata: TableMetadata,
        val valueByteOrder: EcuByteOrder,
        val rpmAxis: AxisLayout,
        val loadAxis: AxisLayout,
    )

    data class Catalog(
        val tableDefinitions: TableDefinitions,
        val pageCatalog: List<EcuPageDescriptor>,
        val veTable: TableLayout,
        val ignitionTable: TableLayout,
        val afrTable: TableLayout,
    )

    fun fromIni(definition: IniDefinition): Catalog? {
        if (!definition.signature.startsWith("MS2Extra MegaSpeed", ignoreCase = true)) {
            return null
        }

        val veTable = buildLayout(
            definition = definition,
            tableName = "veTable1",
            rpmAxisName = "veRpmBins1",
            loadAxisName = "veLoadBins1",
            fallbackRpmAxisName = "ve1RpmBins",
            fallbackLoadAxisName = "ve1LoadBins",
            pageFallback = 0x09,
            valueType = DataType.U08,
            units = "%",
            valueRange = 0.0..255.0,
            rpmRange = 0..16000,
            loadRange = 0.0..700.0,
            tableDisplayName = "MegaSpeed VE Table 1",
        ) ?: buildLayout(
            definition = definition,
            tableName = "veTable1",
            rpmAxisName = "veBins1",
            loadAxisName = "veLoad1",
            fallbackRpmAxisName = "veBins1",
            fallbackLoadAxisName = "veLoad1",
            pageFallback = 0x09,
            valueType = DataType.U08,
            units = "%",
            valueRange = 0.0..255.0,
            rpmRange = 0..16000,
            loadRange = 0.0..700.0,
            tableDisplayName = "MegaSpeed VE Table 1",
        )
        val ignitionTable = buildLayout(
            definition = definition,
            tableName = "advanceTable1",
            rpmAxisName = "srpm_table1",
            loadAxisName = "smap_table1",
            fallbackRpmAxisName = "sparkRpmBins1",
            fallbackLoadAxisName = "sparkLoadBins1",
            pageFallback = 0x0A,
            valueType = DataType.S16,
            units = "deg",
            valueRange = -10.0..90.0,
            rpmRange = 0..16000,
            loadRange = 0.0..700.0,
            tableDisplayName = "MegaSpeed Ignition Table 1",
        )
        val afrTable = buildLayout(
            definition = definition,
            tableName = "afrTable1",
            rpmAxisName = "arpm_table1",
            loadAxisName = "amap_table1",
            fallbackRpmAxisName = "afrRpmBins1",
            fallbackLoadAxisName = "afrLoadBins1",
            pageFallback = 0x04,
            valueType = DataType.U08,
            units = "AFR",
            valueRange = 0.0..25.5,
            rpmRange = 0..16000,
            loadRange = 0.0..700.0,
            tableDisplayName = "MegaSpeed AFR Table 1",
        )

        if (veTable == null || ignitionTable == null || afrTable == null) {
            return null
        }

        return Catalog(
            tableDefinitions = TableDefinitions(
                veTable = veTable.metadata,
                ignitionTable = ignitionTable.metadata,
                afrTable = afrTable.metadata,
                boostTable = null,
                ochBlockSize = definition.ochBlockSize ?: 219,
                nPages = definition.nPages ?: definition.pageDefinitions.size,
                era = FirmwareEra.MS2,
                family = EcuFamily.MEGASPEED,
                byteOrder = EcuByteOrder.BIG_ENDIAN,
            ),
            pageCatalog = definition.pageDefinitions.mapNotNull { page ->
                val pageId = page.resolvedId ?: return@mapNotNull null
                EcuPageDescriptor(
                    id = pageId,
                    size = page.size ?: 1024,
                    label = "Page ${page.index + 1}",
                )
            },
            veTable = veTable,
            ignitionTable = ignitionTable,
            afrTable = afrTable,
        )
    }

    fun parseVeTable(layout: TableLayout, valuesData: ByteArray, rpmAxisData: ByteArray, loadAxisData: ByteArray): VeTable {
        val rows = layout.metadata.valuesShape.first
        val cols = layout.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                val offset = row * cols + col
                valuesData.getOrNull(offset)?.toInt()?.and(0xFF) ?: 0
            }
        }
        return VeTable(
            rpmBins = parseAxis(layout.rpmAxis, rpmAxisData),
            loadBins = parseAxis(layout.loadAxis, loadAxisData),
            values = values,
            loadType = VeTable.LoadType.MAP,
        )
    }

    fun serializeVeTable(layout: TableLayout, table: VeTable): SerializedMs2Table {
        require(table.values.size == layout.metadata.valuesShape.first) { "Invalid VE row count" }
        require(table.rpmBins.size == layout.rpmAxis.count) { "Invalid VE RPM bins count" }
        require(table.loadBins.size == layout.loadAxis.count) { "Invalid VE load bins count" }

        val valuesData = ByteArray(layout.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == layout.metadata.valuesShape.second) { "Invalid VE column count" }
            row.forEach { value ->
                valuesData[writeOffset] = value.coerceIn(0, 255).toByte()
                writeOffset += 1
            }
        }

        return SerializedMs2Table(
            valuesTableId = layout.metadata.page,
            valuesOffset = layout.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = layout.rpmAxis.pageId,
            rpmAxisOffset = layout.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, layout.rpmAxis, EcuByteOrder.BIG_ENDIAN),
            loadAxisTableId = layout.loadAxis.pageId,
            loadAxisOffset = layout.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, layout.loadAxis, EcuByteOrder.BIG_ENDIAN),
            burnTableIds = setOf(layout.metadata.page),
        )
    }

    fun parseIgnitionTable(layout: TableLayout, valuesData: ByteArray, rpmAxisData: ByteArray, loadAxisData: ByteArray): IgnitionTable {
        val rows = layout.metadata.valuesShape.first
        val cols = layout.metadata.valuesShape.second
        val elementSize = dataTypeSize(layout.metadata.valueType)
        val values = List(rows) { row ->
            List(cols) { col ->
                val raw = readValue(
                    data = valuesData,
                    offset = ((row * cols) + col) * elementSize,
                    type = layout.metadata.valueType,
                    byteOrder = layout.valueByteOrder,
                )
                (raw * layout.metadata.scale + layout.metadata.translate).roundToInt()
            }
        }
        return IgnitionTable(
            rpmBins = parseAxis(layout.rpmAxis, rpmAxisData),
            loadBins = parseAxis(layout.loadAxis, loadAxisData),
            values = values,
            loadType = IgnitionTable.LoadType.MAP,
        )
    }

    fun serializeIgnitionTable(layout: TableLayout, table: IgnitionTable): SerializedMs2Table {
        require(table.values.size == layout.metadata.valuesShape.first) { "Invalid ignition row count" }
        require(table.rpmBins.size == layout.rpmAxis.count) { "Invalid ignition RPM bins count" }
        require(table.loadBins.size == layout.loadAxis.count) { "Invalid ignition load bins count" }

        val elementSize = dataTypeSize(layout.metadata.valueType)
        val valuesData = ByteArray(layout.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == layout.metadata.valuesShape.second) { "Invalid ignition column count" }
            row.forEach { value ->
                val raw = ((value - layout.metadata.translate) / layout.metadata.scale).roundToInt()
                writeValue(valuesData, writeOffset, layout.metadata.valueType, raw, layout.valueByteOrder)
                writeOffset += elementSize
            }
        }

        return SerializedMs2Table(
            valuesTableId = layout.metadata.page,
            valuesOffset = layout.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = layout.rpmAxis.pageId,
            rpmAxisOffset = layout.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, layout.rpmAxis, EcuByteOrder.BIG_ENDIAN),
            loadAxisTableId = layout.loadAxis.pageId,
            loadAxisOffset = layout.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, layout.loadAxis, EcuByteOrder.BIG_ENDIAN),
            burnTableIds = setOf(layout.metadata.page),
        )
    }

    fun parseAfrTable(layout: TableLayout, valuesData: ByteArray, rpmAxisData: ByteArray, loadAxisData: ByteArray): AfrTable {
        val rows = layout.metadata.valuesShape.first
        val cols = layout.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                val offset = row * cols + col
                valuesData.getOrNull(offset)?.toInt()?.and(0xFF) ?: 0
            }
        }
        return AfrTable(
            rpmBins = parseAxis(layout.rpmAxis, rpmAxisData),
            loadBins = parseAxis(layout.loadAxis, loadAxisData),
            values = values,
            loadType = AfrTable.LoadType.MAP,
        )
    }

    fun serializeAfrTable(layout: TableLayout, table: AfrTable): SerializedMs2Table {
        require(table.values.size == layout.metadata.valuesShape.first) { "Invalid AFR row count" }
        require(table.rpmBins.size == layout.rpmAxis.count) { "Invalid AFR RPM bins count" }
        require(table.loadBins.size == layout.loadAxis.count) { "Invalid AFR load bins count" }

        val valuesData = ByteArray(layout.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == layout.metadata.valuesShape.second) { "Invalid AFR column count" }
            row.forEach { value ->
                valuesData[writeOffset] = value.coerceIn(0, 255).toByte()
                writeOffset += 1
            }
        }

        return SerializedMs2Table(
            valuesTableId = layout.metadata.page,
            valuesOffset = layout.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = layout.rpmAxis.pageId,
            rpmAxisOffset = layout.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, layout.rpmAxis, EcuByteOrder.BIG_ENDIAN),
            loadAxisTableId = layout.loadAxis.pageId,
            loadAxisOffset = layout.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, layout.loadAxis, EcuByteOrder.BIG_ENDIAN),
            burnTableIds = setOf(layout.metadata.page),
        )
    }

    private fun buildLayout(
        definition: IniDefinition,
        tableName: String,
        rpmAxisName: String,
        loadAxisName: String,
        fallbackRpmAxisName: String,
        fallbackLoadAxisName: String,
        pageFallback: Int,
        valueType: DataType,
        units: String,
        valueRange: ClosedRange<Double>,
        rpmRange: ClosedRange<Int>,
        loadRange: ClosedRange<Double>,
        tableDisplayName: String,
    ): TableLayout? {
        val table = definition.tableDefinitions.firstOrNull { it.name.equals(tableName, ignoreCase = true) } ?: return null
        val rpmAxis = findAxis(definition, rpmAxisName, fallbackRpmAxisName, pageFallback) ?: return null
        val loadAxis = findAxis(definition, loadAxisName, fallbackLoadAxisName, pageFallback) ?: return null
        val pageId = resolvePageId(definition, table.page) ?: pageFallback
        val valueScale = table.scale ?: 1.0
        val valueTranslate = table.translate ?: 0.0
        val elementSize = dataTypeSize(valueType)
        val expectedBytes = table.shape.totalSize * elementSize

        return TableLayout(
            metadata = TableMetadata(
                name = tableDisplayName,
                page = pageId,
                offset = table.offset,
                totalSize = expectedBytes,
                valuesShape = table.shape.rows to table.shape.columns,
                valuesOffset = 0,
                rpmBinsOffset = rpmAxis.offset,
                loadBinsOffset = loadAxis.offset,
                valueType = valueType,
                valueRange = valueRange,
                rpmRange = rpmRange,
                loadRange = loadRange,
                units = units,
                scale = valueScale,
                translate = valueTranslate,
            ),
            valueByteOrder = EcuByteOrder.BIG_ENDIAN,
            rpmAxis = rpmAxis,
            loadAxis = loadAxis,
        )
    }

    private fun findAxis(definition: IniDefinition, primaryName: String, fallbackName: String, pageFallback: Int): AxisLayout? {
        val field = definition.fields.firstOrNull { it.name.equals(primaryName, ignoreCase = true) }
            ?: definition.fields.firstOrNull { it.name.equals(fallbackName, ignoreCase = true) }
            ?: return null
        val count = field.shape?.rows ?: return null
        val offset = field.offset ?: return null
        val pageId = resolvePageId(definition, field.page) ?: pageFallback
        return AxisLayout(
            pageId = pageId,
            offset = offset,
            count = count,
            dataType = normalizeDataType(field.dataType) ?: return null,
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

    private fun parseAxis(axis: AxisLayout, data: ByteArray): List<Int> {
        val step = dataTypeSize(axis.dataType)
        return List(axis.count) { index ->
            val raw = readValue(data, index * step, axis.dataType, EcuByteOrder.BIG_ENDIAN)
            (raw * axis.scale).roundToInt()
        }
    }

    private fun serializeAxis(values: List<Int>, axis: AxisLayout, byteOrder: EcuByteOrder): ByteArray {
        require(values.size == axis.count) { "Invalid axis count" }
        val elementSize = dataTypeSize(axis.dataType)
        val output = ByteArray(values.size * elementSize)
        values.forEachIndexed { index, value ->
            val raw = (value / axis.scale).roundToInt()
            writeValue(output, index * elementSize, axis.dataType, raw, byteOrder)
        }
        return output
    }

    private fun dataTypeSize(type: DataType): Int = when (type) {
        DataType.U08, DataType.S08 -> 1
        DataType.U16, DataType.S16 -> 2
    }

    private fun readValue(data: ByteArray, offset: Int, type: DataType, byteOrder: EcuByteOrder): Int {
        fun u8(index: Int): Int = data.getOrNull(index)?.toInt()?.and(0xFF) ?: 0
        return when (type) {
            DataType.U08 -> u8(offset)
            DataType.S08 -> data.getOrNull(offset)?.toInt() ?: 0
            DataType.U16 -> {
                val first = u8(offset)
                val second = u8(offset + 1)
                if (byteOrder == EcuByteOrder.BIG_ENDIAN) (first shl 8) or second else first or (second shl 8)
            }
            DataType.S16 -> {
                val raw = readValue(data, offset, DataType.U16, byteOrder)
                if (raw and 0x8000 != 0) raw - 0x10000 else raw
            }
        }
    }

    private fun writeValue(data: ByteArray, offset: Int, type: DataType, value: Int, byteOrder: EcuByteOrder) {
        when (type) {
            DataType.U08 -> if (offset < data.size) data[offset] = value.coerceIn(0, 255).toByte()
            DataType.S08 -> if (offset < data.size) data[offset] = value.coerceIn(-128, 127).toByte()
            DataType.U16 -> writeWord(data, offset, value.coerceIn(0, 0xFFFF), byteOrder)
            DataType.S16 -> writeWord(data, offset, value and 0xFFFF, byteOrder)
        }
    }

    private fun writeWord(data: ByteArray, offset: Int, value: Int, byteOrder: EcuByteOrder) {
        if (offset + 1 >= data.size) return
        if (byteOrder == EcuByteOrder.BIG_ENDIAN) {
            data[offset] = ((value shr 8) and 0xFF).toByte()
            data[offset + 1] = (value and 0xFF).toByte()
        } else {
            data[offset] = (value and 0xFF).toByte()
            data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }
    }
}


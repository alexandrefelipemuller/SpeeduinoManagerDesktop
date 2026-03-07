package com.speeduino.manager.model

import kotlin.math.roundToInt

/**
 * Minimal rusEFI table catalog wired from the generated proteus_f7 INI.
 *
 * The current implementation targets the common 16x16 VE / ignition / lambda
 * tables present on the main configuration page (0x0000).
 */
object RusefiTableDefinitions {
    data class AxisLayout(
        val tableId: Int,
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

    private const val TABLE_PAGE_CONFIG = 0x0000

    val IGNITION_TABLE_1 = TableLayout(
        metadata = TableMetadata(
            name = "rusEFI Ignition Table 1",
            page = TABLE_PAGE_CONFIG,
            offset = 56296,
            totalSize = 512,
            valuesShape = 16 to 16,
            valuesOffset = 0,
            rpmBinsOffset = 56840,
            loadBinsOffset = 56808,
            valueType = DataType.S16,
            valueRange = -20.0..90.0,
            rpmRange = 0..18000,
            loadRange = 0.0..1000.0,
            units = "deg",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.LITTLE_ENDIAN,
        rpmAxis = AxisLayout(
            tableId = TABLE_PAGE_CONFIG,
            offset = 56840,
            count = 16,
            dataType = DataType.U16,
        ),
        loadAxis = AxisLayout(
            tableId = TABLE_PAGE_CONFIG,
            offset = 56808,
            count = 16,
            dataType = DataType.U16,
        ),
    )

    val VE_TABLE_1 = TableLayout(
        metadata = TableMetadata(
            name = "rusEFI VE Table 1",
            page = TABLE_PAGE_CONFIG,
            offset = 56872,
            totalSize = 512,
            valuesShape = 16 to 16,
            valuesOffset = 0,
            rpmBinsOffset = 57416,
            loadBinsOffset = 57384,
            valueType = DataType.U16,
            valueRange = 0.0..999.0,
            rpmRange = 0..18000,
            loadRange = 0.0..1000.0,
            units = "%",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.LITTLE_ENDIAN,
        rpmAxis = AxisLayout(
            tableId = TABLE_PAGE_CONFIG,
            offset = 57416,
            count = 16,
            dataType = DataType.U16,
        ),
        loadAxis = AxisLayout(
            tableId = TABLE_PAGE_CONFIG,
            offset = 57384,
            count = 16,
            dataType = DataType.U16,
        ),
    )

    val AFR_TABLE_1 = TableLayout(
        metadata = TableMetadata(
            name = "rusEFI AFR Table 1",
            page = TABLE_PAGE_CONFIG,
            offset = 57448,
            totalSize = 256,
            valuesShape = 16 to 16,
            valuesOffset = 0,
            rpmBinsOffset = 57736,
            loadBinsOffset = 57704,
            valueType = DataType.U08,
            valueRange = 10.0..25.5,
            rpmRange = 0..18000,
            loadRange = 0.0..1000.0,
            units = "AFR",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.LITTLE_ENDIAN,
        rpmAxis = AxisLayout(
            tableId = TABLE_PAGE_CONFIG,
            offset = 57736,
            count = 16,
            dataType = DataType.U16,
        ),
        loadAxis = AxisLayout(
            tableId = TABLE_PAGE_CONFIG,
            offset = 57704,
            count = 16,
            dataType = DataType.U16,
        ),
    )

    val TABLE_DEFINITIONS = TableDefinitions(
        veTable = VE_TABLE_1.metadata,
        ignitionTable = IGNITION_TABLE_1.metadata,
        afrTable = AFR_TABLE_1.metadata,
        boostTable = null,
        ochBlockSize = 2084,
        nPages = 3,
        era = FirmwareEra.RUSEFI,
        family = EcuFamily.RUSEFI,
        byteOrder = EcuByteOrder.LITTLE_ENDIAN,
    )

    fun parseVeTable(
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: VeTable.LoadType = VeTable.LoadType.MAP,
    ): VeTable {
        return parseVeTableWithLayout(VE_TABLE_1, valuesData, rpmAxisData, loadAxisData, loadType)
    }

    fun parseVeTableWithLayout(
        layout: TableLayout,
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: VeTable.LoadType = VeTable.LoadType.MAP,
    ): VeTable {
        val rows = layout.metadata.valuesShape.first
        val cols = layout.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                val raw = readValue(
                    valuesData,
                    ((row * cols) + col) * 2,
                    layout.metadata.valueType,
                    layout.valueByteOrder,
                )
                raw.toScaledInt(layout.metadata.scale, layout.metadata.translate)
            }
        }
        val rpmBins = parseAxis(layout.rpmAxis, rpmAxisData)
        val loadBins = parseAxis(layout.loadAxis, loadAxisData)
        return VeTable(rpmBins, loadBins, values, loadType)
    }

    fun parseIgnitionTable(
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: IgnitionTable.LoadType = IgnitionTable.LoadType.MAP,
    ): IgnitionTable {
        return parseIgnitionTableWithLayout(IGNITION_TABLE_1, valuesData, rpmAxisData, loadAxisData, loadType)
    }

    fun parseIgnitionTableWithLayout(
        layout: TableLayout,
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: IgnitionTable.LoadType = IgnitionTable.LoadType.MAP,
    ): IgnitionTable {
        val rows = layout.metadata.valuesShape.first
        val cols = layout.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                val raw = readValue(
                    valuesData,
                    ((row * cols) + col) * 2,
                    layout.metadata.valueType,
                    layout.valueByteOrder,
                )
                (raw * layout.metadata.scale).roundToInt()
            }
        }
        val rpmBins = parseAxis(layout.rpmAxis, rpmAxisData)
        val loadBins = parseAxis(layout.loadAxis, loadAxisData)
        return IgnitionTable(rpmBins, loadBins, values, loadType)
    }

    fun parseAfrTable(
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: AfrTable.LoadType = AfrTable.LoadType.MAP,
    ): AfrTable {
        return parseAfrTableWithLayout(AFR_TABLE_1, valuesData, rpmAxisData, loadAxisData, loadType)
    }

    fun parseAfrTableWithLayout(
        layout: TableLayout,
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: AfrTable.LoadType = AfrTable.LoadType.MAP,
    ): AfrTable {
        val rows = layout.metadata.valuesShape.first
        val cols = layout.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                valuesData.getOrNull((row * cols) + col)?.toInt()?.and(0xFF) ?: 0
            }
        }
        val rpmBins = parseAxis(layout.rpmAxis, rpmAxisData)
        val loadBins = parseAxis(layout.loadAxis, loadAxisData)
        return AfrTable(rpmBins, loadBins, values, loadType)
    }

    fun serializeVeTable(table: VeTable): SerializedRusefiTable {
        return serializeVeTableWithLayout(VE_TABLE_1, table, signedValues = false, valueScale = 10)
    }

    fun serializeVeTableWithLayout(
        layout: TableLayout,
        table: VeTable,
        signedValues: Boolean,
        valueScale: Int,
    ): SerializedRusefiTable {
        require(table.values.size == layout.metadata.valuesShape.first) { "Invalid VE row count" }
        require(table.rpmBins.size == layout.rpmAxis.count) { "Invalid VE RPM bins count" }
        require(table.loadBins.size == layout.loadAxis.count) { "Invalid VE load bins count" }

        val valuesData = ByteArray(layout.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == layout.metadata.valuesShape.second) { "Invalid VE column count" }
            row.forEach { value ->
                writeValue(valuesData, writeOffset, value * valueScale, signed = signedValues)
                writeOffset += 2
            }
        }

        return SerializedRusefiTable(
            valuesTableId = layout.metadata.page,
            valuesOffset = layout.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = layout.rpmAxis.tableId,
            rpmAxisOffset = layout.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, layout.rpmAxis),
            loadAxisTableId = layout.loadAxis.tableId,
            loadAxisOffset = layout.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, layout.loadAxis),
            burnTableIds = setOf(TABLE_PAGE_CONFIG),
        )
    }

    fun serializeIgnitionTable(table: IgnitionTable): SerializedRusefiTable {
        return serializeIgnitionTableWithLayout(IGNITION_TABLE_1, table)
    }

    fun serializeIgnitionTableWithLayout(layout: TableLayout, table: IgnitionTable): SerializedRusefiTable {
        require(table.values.size == layout.metadata.valuesShape.first) { "Invalid ignition row count" }
        require(table.rpmBins.size == layout.rpmAxis.count) { "Invalid ignition RPM bins count" }
        require(table.loadBins.size == layout.loadAxis.count) { "Invalid ignition load bins count" }

        val valuesData = ByteArray(layout.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == layout.metadata.valuesShape.second) { "Invalid ignition column count" }
            row.forEach { value ->
                writeValue(valuesData, writeOffset, value * 10, signed = true)
                writeOffset += 2
            }
        }

        return SerializedRusefiTable(
            valuesTableId = layout.metadata.page,
            valuesOffset = layout.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = layout.rpmAxis.tableId,
            rpmAxisOffset = layout.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, layout.rpmAxis),
            loadAxisTableId = layout.loadAxis.tableId,
            loadAxisOffset = layout.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, layout.loadAxis),
            burnTableIds = setOf(TABLE_PAGE_CONFIG),
        )
    }

    fun serializeAfrTable(table: AfrTable): SerializedRusefiTable {
        return serializeAfrTableWithLayout(AFR_TABLE_1, table)
    }

    fun serializeAfrTableWithLayout(layout: TableLayout, table: AfrTable): SerializedRusefiTable {
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

        return SerializedRusefiTable(
            valuesTableId = layout.metadata.page,
            valuesOffset = layout.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = layout.rpmAxis.tableId,
            rpmAxisOffset = layout.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, layout.rpmAxis),
            loadAxisTableId = layout.loadAxis.tableId,
            loadAxisOffset = layout.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, layout.loadAxis),
            burnTableIds = setOf(TABLE_PAGE_CONFIG),
        )
    }

    private fun parseAxis(layout: AxisLayout, data: ByteArray): List<Int> {
        return List(layout.count) { index ->
            val raw = readValue(
                data = data,
                offset = index * 2,
                type = layout.dataType,
                byteOrder = EcuByteOrder.LITTLE_ENDIAN,
            )
            (raw * layout.scale).roundToInt()
        }
    }

    private fun serializeAxis(values: List<Int>, layout: AxisLayout): ByteArray {
        require(values.size == layout.count) { "Invalid axis count" }
        val data = ByteArray(layout.count * 2)
        values.forEachIndexed { index, value ->
            val raw = when {
                layout.scale == 0.0 -> value
                else -> (value / layout.scale).roundToInt()
            }
            writeValue(
                target = data,
                offset = index * 2,
                rawValue = raw,
                signed = layout.dataType == DataType.S16,
            )
        }
        return data
    }

    private fun readValue(
        data: ByteArray,
        offset: Int,
        type: DataType,
        byteOrder: EcuByteOrder,
    ): Int {
        val first = data.getOrNull(offset)?.toInt()?.and(0xFF) ?: 0
        val second = data.getOrNull(offset + 1)?.toInt()?.and(0xFF) ?: 0
        return when (type) {
            DataType.U08 -> first
            DataType.S08 -> data.getOrNull(offset)?.toInt() ?: 0
            DataType.U16 -> when (byteOrder) {
                EcuByteOrder.LITTLE_ENDIAN -> first or (second shl 8)
                EcuByteOrder.BIG_ENDIAN -> (first shl 8) or second
            }
            DataType.S16 -> {
                val raw = when (byteOrder) {
                    EcuByteOrder.LITTLE_ENDIAN -> first or (second shl 8)
                    EcuByteOrder.BIG_ENDIAN -> (first shl 8) or second
                }
                if (raw >= 0x8000) raw - 0x10000 else raw
            }
        }
    }

    private fun writeValue(target: ByteArray, offset: Int, rawValue: Int, signed: Boolean) {
        val clamped = if (signed) rawValue.coerceIn(-32768, 32767) else rawValue.coerceIn(0, 0xFFFF)
        target[offset] = (clamped and 0xFF).toByte()
        target[offset + 1] = ((clamped shr 8) and 0xFF).toByte()
    }

    private fun Int.toScaledInt(scale: Double, translate: Double): Int {
        return ((this + translate) * scale).roundToInt()
    }
}

data class SerializedRusefiTable(
    val valuesTableId: Int,
    val valuesOffset: Int,
    val valuesData: ByteArray,
    val rpmAxisTableId: Int,
    val rpmAxisOffset: Int,
    val rpmAxisData: ByteArray,
    val loadAxisTableId: Int,
    val loadAxisOffset: Int,
    val loadAxisData: ByteArray,
    val burnTableIds: Set<Int>,
)

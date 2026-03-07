package com.speeduino.manager.model

import kotlin.math.roundToInt

/**
 * Minimal MS2/Extra table catalog used by the first map-reading integration.
 *
 * References:
 * - speeduino_ini_analysis/megasquirt2.ini
 * - simulator/ms2_tcp_simulator.py
 */
object Ms2TableDefinitions {
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

    private const val TABLE_PAGE_AFR = 0x04
    private const val TABLE_PAGE_IGNITION = 0x0A
    private const val TABLE_PAGE_VE = 0x09

    val VE_TABLE_1 = TableLayout(
        metadata = TableMetadata(
            name = "MS2 VE Table 1",
            page = TABLE_PAGE_VE,
            offset = 0,
            totalSize = 256,
            valuesShape = 16 to 16,
            valuesOffset = 0,
            rpmBinsOffset = 768,
            loadBinsOffset = 864,
            valueType = DataType.U08,
            valueRange = 0.0..255.0,
            rpmRange = 0..16000,
            loadRange = 0.0..400.0,
            units = "%",
            scale = 1.0,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.BIG_ENDIAN,
        rpmAxis = AxisLayout(
            tableId = TABLE_PAGE_VE,
            offset = 768,
            count = 16,
            dataType = DataType.U16,
        ),
        loadAxis = AxisLayout(
            tableId = TABLE_PAGE_VE,
            offset = 864,
            count = 16,
            dataType = DataType.S16,
            scale = 0.1,
        ),
    )

    val IGNITION_TABLE_1 = TableLayout(
        metadata = TableMetadata(
            name = "MS2 Ignition Table 1",
            page = TABLE_PAGE_IGNITION,
            offset = 0,
            totalSize = 288,
            valuesShape = 12 to 12,
            valuesOffset = 0,
            rpmBinsOffset = 576,
            loadBinsOffset = 624,
            valueType = DataType.S16,
            valueRange = -40.0..90.0,
            rpmRange = 0..16000,
            loadRange = 0.0..400.0,
            units = "deg",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.BIG_ENDIAN,
        rpmAxis = AxisLayout(
            tableId = TABLE_PAGE_IGNITION,
            offset = 576,
            count = 12,
            dataType = DataType.U16,
        ),
        loadAxis = AxisLayout(
            tableId = TABLE_PAGE_IGNITION,
            offset = 624,
            count = 12,
            dataType = DataType.S16,
            scale = 0.1,
        ),
    )

    val AFR_TABLE_1 = TableLayout(
        metadata = TableMetadata(
            name = "MS2 AFR Table 1",
            page = TABLE_PAGE_AFR,
            offset = 48,
            totalSize = 144,
            valuesShape = 12 to 12,
            valuesOffset = 0,
            rpmBinsOffset = 374,
            loadBinsOffset = 422,
            valueType = DataType.U08,
            valueRange = 10.0..200.0,
            rpmRange = 0..16000,
            loadRange = 0.0..400.0,
            units = "AFR",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.BIG_ENDIAN,
        rpmAxis = AxisLayout(
            tableId = TABLE_PAGE_AFR,
            offset = 374,
            count = 12,
            dataType = DataType.U16,
        ),
        loadAxis = AxisLayout(
            tableId = TABLE_PAGE_AFR,
            offset = 422,
            count = 12,
            dataType = DataType.S16,
            scale = 0.1,
        ),
    )

    val TABLE_DEFINITIONS = TableDefinitions(
        veTable = VE_TABLE_1.metadata,
        ignitionTable = IGNITION_TABLE_1.metadata,
        afrTable = AFR_TABLE_1.metadata,
        boostTable = null,
        ochBlockSize = 212,
        nPages = 7,
        era = FirmwareEra.MS2,
        family = EcuFamily.MS2,
        byteOrder = EcuByteOrder.BIG_ENDIAN,
    )

    fun parseVeTable(
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: VeTable.LoadType = VeTable.LoadType.MAP,
    ): VeTable {
        val rows = VE_TABLE_1.metadata.valuesShape.first
        val cols = VE_TABLE_1.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                valuesData.getOrNull((row * cols) + col)?.toInt()?.and(0xFF) ?: 0
            }
        }
        val rpmBins = parseAxis(VE_TABLE_1.rpmAxis, rpmAxisData)
        val loadBins = parseAxis(VE_TABLE_1.loadAxis, loadAxisData)
        return VeTable(rpmBins, loadBins, values, loadType)
    }

    fun serializeVeTable(table: VeTable): SerializedMs2Table {
        require(table.values.size == VE_TABLE_1.metadata.valuesShape.first) { "Invalid VE row count" }
        require(table.rpmBins.size == VE_TABLE_1.rpmAxis.count) { "Invalid VE RPM bins count" }
        require(table.loadBins.size == VE_TABLE_1.loadAxis.count) { "Invalid VE load bins count" }

        val valuesData = ByteArray(VE_TABLE_1.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == VE_TABLE_1.metadata.valuesShape.second) { "Invalid VE column count" }
            row.forEach { value ->
                valuesData[writeOffset] = value.coerceIn(0, 255).toByte()
                writeOffset += 1
            }
        }

        return SerializedMs2Table(
            valuesTableId = VE_TABLE_1.metadata.page,
            valuesOffset = VE_TABLE_1.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = VE_TABLE_1.rpmAxis.tableId,
            rpmAxisOffset = VE_TABLE_1.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, VE_TABLE_1.rpmAxis),
            loadAxisTableId = VE_TABLE_1.loadAxis.tableId,
            loadAxisOffset = VE_TABLE_1.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, VE_TABLE_1.loadAxis),
            burnTableIds = setOf(VE_TABLE_1.metadata.page),
        )
    }

    fun parseIgnitionTable(
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: IgnitionTable.LoadType = IgnitionTable.LoadType.MAP,
    ): IgnitionTable {
        val rows = IGNITION_TABLE_1.metadata.valuesShape.first
        val cols = IGNITION_TABLE_1.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                val raw = readValue(
                    valuesData,
                    ((row * cols) + col) * 2,
                    IGNITION_TABLE_1.metadata.valueType,
                    IGNITION_TABLE_1.valueByteOrder,
                )
                (raw * IGNITION_TABLE_1.metadata.scale).roundToInt()
            }
        }
        val rpmBins = parseAxis(IGNITION_TABLE_1.rpmAxis, rpmAxisData)
        val loadBins = parseAxis(IGNITION_TABLE_1.loadAxis, loadAxisData)
        return IgnitionTable(rpmBins, loadBins, values, loadType)
    }

    fun serializeIgnitionTable(table: IgnitionTable): SerializedMs2Table {
        require(table.values.size == IGNITION_TABLE_1.metadata.valuesShape.first) { "Invalid ignition row count" }
        require(table.rpmBins.size == IGNITION_TABLE_1.rpmAxis.count) { "Invalid ignition RPM bins count" }
        require(table.loadBins.size == IGNITION_TABLE_1.loadAxis.count) { "Invalid ignition load bins count" }

        val valuesData = ByteArray(IGNITION_TABLE_1.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == IGNITION_TABLE_1.metadata.valuesShape.second) { "Invalid ignition column count" }
            row.forEach { value ->
                writeS16(valuesData, writeOffset, value * 10, signed = true)
                writeOffset += 2
            }
        }

        return SerializedMs2Table(
            valuesTableId = IGNITION_TABLE_1.metadata.page,
            valuesOffset = IGNITION_TABLE_1.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = IGNITION_TABLE_1.rpmAxis.tableId,
            rpmAxisOffset = IGNITION_TABLE_1.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, IGNITION_TABLE_1.rpmAxis),
            loadAxisTableId = IGNITION_TABLE_1.loadAxis.tableId,
            loadAxisOffset = IGNITION_TABLE_1.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, IGNITION_TABLE_1.loadAxis),
            burnTableIds = setOf(IGNITION_TABLE_1.metadata.page),
        )
    }

    fun parseAfrTable(
        valuesData: ByteArray,
        rpmAxisData: ByteArray,
        loadAxisData: ByteArray,
        loadType: AfrTable.LoadType = AfrTable.LoadType.MAP,
    ): AfrTable {
        val rows = AFR_TABLE_1.metadata.valuesShape.first
        val cols = AFR_TABLE_1.metadata.valuesShape.second
        val values = List(rows) { row ->
            List(cols) { col ->
                valuesData.getOrNull((row * cols) + col)?.toInt()?.and(0xFF) ?: 0
            }
        }
        val rpmBins = parseAxis(AFR_TABLE_1.rpmAxis, rpmAxisData)
        val loadBins = parseAxis(AFR_TABLE_1.loadAxis, loadAxisData)
        return AfrTable(rpmBins, loadBins, values, loadType)
    }

    fun serializeAfrTable(table: AfrTable): SerializedMs2Table {
        require(table.values.size == AFR_TABLE_1.metadata.valuesShape.first) { "Invalid AFR row count" }
        require(table.rpmBins.size == AFR_TABLE_1.rpmAxis.count) { "Invalid AFR RPM bins count" }
        require(table.loadBins.size == AFR_TABLE_1.loadAxis.count) { "Invalid AFR load bins count" }

        val valuesData = ByteArray(AFR_TABLE_1.metadata.totalSize)
        var writeOffset = 0
        table.values.forEach { row ->
            require(row.size == AFR_TABLE_1.metadata.valuesShape.second) { "Invalid AFR column count" }
            row.forEach { value ->
                valuesData[writeOffset] = value.coerceIn(0, 255).toByte()
                writeOffset += 1
            }
        }

        return SerializedMs2Table(
            valuesTableId = AFR_TABLE_1.metadata.page,
            valuesOffset = AFR_TABLE_1.metadata.offset,
            valuesData = valuesData,
            rpmAxisTableId = AFR_TABLE_1.rpmAxis.tableId,
            rpmAxisOffset = AFR_TABLE_1.rpmAxis.offset,
            rpmAxisData = serializeAxis(table.rpmBins, AFR_TABLE_1.rpmAxis),
            loadAxisTableId = AFR_TABLE_1.loadAxis.tableId,
            loadAxisOffset = AFR_TABLE_1.loadAxis.offset,
            loadAxisData = serializeAxis(table.loadBins, AFR_TABLE_1.loadAxis),
            burnTableIds = setOf(AFR_TABLE_1.metadata.page),
        )
    }

    private fun parseAxis(axis: AxisLayout, data: ByteArray): List<Int> {
        return List(axis.count) { index ->
            val raw = readValue(
                data = data,
                offset = index * when (axis.dataType) {
                    DataType.U08, DataType.S08 -> 1
                    DataType.U16, DataType.S16 -> 2
                },
                type = axis.dataType,
                byteOrder = EcuByteOrder.BIG_ENDIAN,
            )
            (raw * axis.scale).roundToInt()
        }
    }

    private fun serializeAxis(values: List<Int>, layout: AxisLayout): ByteArray {
        require(values.size == layout.count) { "Invalid axis count" }
        val elementSize = when (layout.dataType) {
            DataType.U08, DataType.S08 -> 1
            DataType.U16, DataType.S16 -> 2
        }
        val output = ByteArray(values.size * elementSize)
        values.forEachIndexed { index, value ->
            val raw = (value / layout.scale).roundToInt()
            val offset = index * elementSize
            when (layout.dataType) {
                DataType.U08 -> output[offset] = raw.coerceIn(0, 255).toByte()
                DataType.S08 -> output[offset] = raw.coerceIn(-128, 127).toByte()
                DataType.U16 -> writeS16(output, offset, raw.coerceIn(0, 0xFFFF), signed = false)
                DataType.S16 -> writeS16(output, offset, raw.coerceIn(-32768, 32767), signed = true)
            }
        }
        return output
    }

    private fun readValue(
        data: ByteArray,
        offset: Int,
        type: DataType,
        byteOrder: EcuByteOrder,
    ): Int {
        fun u8(index: Int): Int = data.getOrNull(index)?.toInt()?.and(0xFF) ?: 0
        return when (type) {
            DataType.U08 -> u8(offset)
            DataType.S08 -> data.getOrNull(offset)?.toInt() ?: 0
            DataType.U16 -> {
                val first = u8(offset)
                val second = u8(offset + 1)
                if (byteOrder == EcuByteOrder.BIG_ENDIAN) {
                    (first shl 8) or second
                } else {
                    first or (second shl 8)
                }
            }
            DataType.S16 -> {
                val raw = readValue(data, offset, DataType.U16, byteOrder)
                if (raw and 0x8000 != 0) raw - 0x10000 else raw
            }
        }
    }

    private fun writeS16(data: ByteArray, offset: Int, value: Int, signed: Boolean) {
        if (offset + 1 >= data.size) return
        val raw = if (signed) value and 0xFFFF else value.coerceIn(0, 0xFFFF)
        data[offset] = ((raw shr 8) and 0xFF).toByte()
        data[offset + 1] = (raw and 0xFF).toByte()
    }
}

data class SerializedMs2Table(
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

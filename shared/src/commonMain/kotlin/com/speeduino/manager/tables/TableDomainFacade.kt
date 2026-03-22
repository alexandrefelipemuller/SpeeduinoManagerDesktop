package com.speeduino.manager.tables

import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.Algorithm
import com.speeduino.manager.model.DataType
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.TableMetadata
import com.speeduino.manager.model.TableValidator
import com.speeduino.manager.model.ValidationException
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.model.afrTableLoadType
import com.speeduino.manager.model.fuelTableLoadType
import com.speeduino.manager.model.ignitionTableLoadType
import java.util.Locale

data class TableWritePayload(
    val metadata: TableMetadata,
    val data: ByteArray,
)

object TableDomainFacade {
    fun resolveVeLoadType(engineConstants: EngineConstants?, algorithmBits: Int? = null): VeTable.LoadType {
        algorithmBits?.let {
            return if (Algorithm.fromBits(it and 0x07) == Algorithm.ALPHA_N) {
                VeTable.LoadType.TPS
            } else {
                VeTable.LoadType.MAP
            }
        }
        return engineConstants?.fuelTableLoadType() ?: VeTable.LoadType.MAP
    }

    fun resolveIgnitionLoadType(
        engineConstants: EngineConstants?,
        algorithmBits: Int? = null,
    ): IgnitionTable.LoadType {
        algorithmBits?.let {
            return if (Algorithm.fromBits(it and 0x07) == Algorithm.ALPHA_N) {
                IgnitionTable.LoadType.TPS
            } else {
                IgnitionTable.LoadType.MAP
            }
        }
        return engineConstants?.ignitionTableLoadType() ?: IgnitionTable.LoadType.MAP
    }

    fun resolveAfrLoadType(
        engineConstants: EngineConstants?,
        isLegacyFormat: Boolean = false,
    ): AfrTable.LoadType {
        return engineConstants?.afrTableLoadType(isLegacyFormat) ?: AfrTable.LoadType.MAP
    }

    fun isMapLoad(engineConstants: EngineConstants?): Boolean {
        return engineConstants?.algorithm != Algorithm.ALPHA_N
    }

    fun resolveSpeeduinoTableMetadata(
        fieldDataType: String,
        fieldPage: Int?,
        fieldOffset: Int?,
        fieldRows: Int?,
        fieldColumns: Int?,
        fallback: TableMetadata,
        displayName: String,
    ): TableMetadata {
        val rows = fieldRows ?: fallback.valuesShape.first
        val cols = fieldColumns ?: fallback.valuesShape.second
        val valueSize = when (fieldDataType.trim().uppercase(Locale.US)) {
            "U16", "S16" -> 2
            else -> 1
        }
        val valuesBytes = rows * cols * valueSize
        val axisBytes = if (rows == 16 && cols == 16) 32 else (rows + cols) * valueSize
        return fallback.copy(
            name = displayName,
            page = fieldPage ?: fallback.page,
            offset = fieldOffset ?: fallback.offset,
            totalSize = valuesBytes + axisBytes,
            valuesShape = rows to cols,
            valuesOffset = 0,
            rpmBinsOffset = valuesBytes,
            loadBinsOffset = valuesBytes + (cols * valueSize),
            valueType = when (fieldDataType.trim().uppercase(Locale.US)) {
                "S08" -> DataType.S08
                "U16" -> DataType.U16
                "S16" -> DataType.S16
                else -> DataType.U08
            },
        )
    }

    fun prepareVeWrite(metadata: TableMetadata, table: VeTable): TableWritePayload {
        validateTable(metadata, table)
        val storageFormat = VeTable.StorageFormat.fromTotalSize(metadata.totalSize)
        val data = storageFormat?.let(table::toByteArray) ?: table.toByteArray()
        return TableWritePayload(metadata = metadata, data = data)
    }

    fun prepareIgnitionWrite(metadata: TableMetadata, table: IgnitionTable): TableWritePayload {
        validateTable(metadata, table)
        val storageFormat = IgnitionTable.StorageFormat.fromTotalSize(metadata.totalSize)
        val data = storageFormat?.let(table::toByteArray) ?: table.toByteArray()
        return TableWritePayload(metadata = metadata, data = data)
    }

    fun prepareAfrWrite(metadata: TableMetadata, table: AfrTable): TableWritePayload {
        val storageFormat = AfrTable.StorageFormat.fromTotalSize(metadata.totalSize)
        val data = storageFormat?.let(table::toByteArray) ?: table.toByteArray()
        return TableWritePayload(metadata = metadata, data = data)
    }

    private fun validateTable(metadata: TableMetadata, table: Any) {
        val validationResult = TableValidator(metadata).validateBeforeWrite(table)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult)
        }
    }
}

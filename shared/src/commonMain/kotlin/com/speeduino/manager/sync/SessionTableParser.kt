package com.speeduino.manager.sync

import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.EcuFamily
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.Ms2TableDefinitions
import com.speeduino.manager.model.Ms3TableDefinitions
import com.speeduino.manager.model.RusefiTableDefinitions
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.tables.TableDomainFacade

object SessionTableParser {
    fun parseEngineConstants(
        ecuFamily: EcuFamily,
        pages: Map<Int, ByteArray>,
    ): EngineConstants? {
        return when (ecuFamily) {
            EcuFamily.MS2,
            EcuFamily.MEGASPEED -> pages[0x04]?.let(EngineConstants::fromMs2Page1)
            EcuFamily.RUSEFI -> pages[0x0000]?.let(EngineConstants::fromRusefiMainPage)
            else -> pages[1]?.let(EngineConstants::fromPage1)
        }
    }

    fun parseVeTable(
        ecuFamily: EcuFamily,
        pages: Map<Int, ByteArray>,
        engineConstants: EngineConstants? = parseEngineConstants(ecuFamily, pages),
    ): VeTable? {
        return when (ecuFamily) {
            EcuFamily.MS2,
            EcuFamily.MEGASPEED -> {
                val blob = pages[Ms2TableDefinitions.VE_TABLE_1.metadata.page] ?: return null
                Ms2TableDefinitions.parseVeTable(
                    valuesData = blob.sliceRange(
                        Ms2TableDefinitions.VE_TABLE_1.metadata.offset,
                        Ms2TableDefinitions.VE_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = blob.sliceRange(
                        Ms2TableDefinitions.VE_TABLE_1.rpmAxis.offset,
                        Ms2TableDefinitions.VE_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = blob.sliceRange(
                        Ms2TableDefinitions.VE_TABLE_1.loadAxis.offset,
                        Ms2TableDefinitions.VE_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            EcuFamily.MS3 -> {
                val valuesBlob = pages[Ms3TableDefinitions.VE_TABLE_1.metadata.page] ?: return null
                val axesBlob = pages[Ms3TableDefinitions.VE_TABLE_1.rpmAxis.tableId] ?: return null
                Ms3TableDefinitions.parseVeTable(
                    valuesData = valuesBlob.sliceRange(
                        Ms3TableDefinitions.VE_TABLE_1.metadata.offset,
                        Ms3TableDefinitions.VE_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = axesBlob.sliceRange(
                        Ms3TableDefinitions.VE_TABLE_1.rpmAxis.offset,
                        Ms3TableDefinitions.VE_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = axesBlob.sliceRange(
                        Ms3TableDefinitions.VE_TABLE_1.loadAxis.offset,
                        Ms3TableDefinitions.VE_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            EcuFamily.RUSEFI -> {
                val blob = pages[RusefiTableDefinitions.VE_TABLE_1.metadata.page] ?: return null
                RusefiTableDefinitions.parseVeTable(
                    valuesData = blob.sliceRange(
                        RusefiTableDefinitions.VE_TABLE_1.metadata.offset,
                        RusefiTableDefinitions.VE_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = blob.sliceRange(
                        RusefiTableDefinitions.VE_TABLE_1.rpmAxis.offset,
                        RusefiTableDefinitions.VE_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = blob.sliceRange(
                        RusefiTableDefinitions.VE_TABLE_1.loadAxis.offset,
                        RusefiTableDefinitions.VE_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            else -> {
                val data = pages[2] ?: pages[1] ?: return null
                val loadType = TableDomainFacade.resolveVeLoadType(engineConstants)
                VeTable.fromPageData(data, loadType = loadType)
            }
        }
    }

    fun parseIgnitionTable(
        ecuFamily: EcuFamily,
        pages: Map<Int, ByteArray>,
        engineConstants: EngineConstants? = parseEngineConstants(ecuFamily, pages),
    ): IgnitionTable? {
        return when (ecuFamily) {
            EcuFamily.MS2,
            EcuFamily.MEGASPEED -> {
                val blob = pages[Ms2TableDefinitions.IGNITION_TABLE_1.metadata.page] ?: return null
                Ms2TableDefinitions.parseIgnitionTable(
                    valuesData = blob.sliceRange(
                        Ms2TableDefinitions.IGNITION_TABLE_1.metadata.offset,
                        Ms2TableDefinitions.IGNITION_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = blob.sliceRange(
                        Ms2TableDefinitions.IGNITION_TABLE_1.rpmAxis.offset,
                        Ms2TableDefinitions.IGNITION_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = blob.sliceRange(
                        Ms2TableDefinitions.IGNITION_TABLE_1.loadAxis.offset,
                        Ms2TableDefinitions.IGNITION_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            EcuFamily.MS3 -> {
                val valuesBlob = pages[Ms3TableDefinitions.IGNITION_TABLE_1.metadata.page] ?: return null
                val axesBlob = pages[Ms3TableDefinitions.IGNITION_TABLE_1.rpmAxis.tableId] ?: return null
                Ms3TableDefinitions.parseIgnitionTable(
                    valuesData = valuesBlob.sliceRange(
                        Ms3TableDefinitions.IGNITION_TABLE_1.metadata.offset,
                        Ms3TableDefinitions.IGNITION_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = axesBlob.sliceRange(
                        Ms3TableDefinitions.IGNITION_TABLE_1.rpmAxis.offset,
                        Ms3TableDefinitions.IGNITION_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = axesBlob.sliceRange(
                        Ms3TableDefinitions.IGNITION_TABLE_1.loadAxis.offset,
                        Ms3TableDefinitions.IGNITION_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            EcuFamily.RUSEFI -> {
                val blob = pages[RusefiTableDefinitions.IGNITION_TABLE_1.metadata.page] ?: return null
                RusefiTableDefinitions.parseIgnitionTable(
                    valuesData = blob.sliceRange(
                        RusefiTableDefinitions.IGNITION_TABLE_1.metadata.offset,
                        RusefiTableDefinitions.IGNITION_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = blob.sliceRange(
                        RusefiTableDefinitions.IGNITION_TABLE_1.rpmAxis.offset,
                        RusefiTableDefinitions.IGNITION_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = blob.sliceRange(
                        RusefiTableDefinitions.IGNITION_TABLE_1.loadAxis.offset,
                        RusefiTableDefinitions.IGNITION_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            else -> {
                val data = pages[3] ?: return null
                val loadType = TableDomainFacade.resolveIgnitionLoadType(engineConstants)
                IgnitionTable.fromPageData(data, loadType = loadType)
            }
        }
    }

    fun parseAfrTable(
        ecuFamily: EcuFamily,
        pages: Map<Int, ByteArray>,
        engineConstants: EngineConstants? = parseEngineConstants(ecuFamily, pages),
    ): AfrTable? {
        return when (ecuFamily) {
            EcuFamily.MS2,
            EcuFamily.MEGASPEED -> {
                val blob = pages[Ms2TableDefinitions.AFR_TABLE_1.metadata.page] ?: return null
                Ms2TableDefinitions.parseAfrTable(
                    valuesData = blob.sliceRange(
                        Ms2TableDefinitions.AFR_TABLE_1.metadata.offset,
                        Ms2TableDefinitions.AFR_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = blob.sliceRange(
                        Ms2TableDefinitions.AFR_TABLE_1.rpmAxis.offset,
                        Ms2TableDefinitions.AFR_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = blob.sliceRange(
                        Ms2TableDefinitions.AFR_TABLE_1.loadAxis.offset,
                        Ms2TableDefinitions.AFR_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            EcuFamily.MS3 -> {
                val blob = pages[Ms3TableDefinitions.AFR_TABLE_1.metadata.page] ?: return null
                Ms3TableDefinitions.parseAfrTable(
                    valuesData = blob.sliceRange(
                        Ms3TableDefinitions.AFR_TABLE_1.metadata.offset,
                        Ms3TableDefinitions.AFR_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = blob.sliceRange(
                        Ms3TableDefinitions.AFR_TABLE_1.rpmAxis.offset,
                        Ms3TableDefinitions.AFR_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = blob.sliceRange(
                        Ms3TableDefinitions.AFR_TABLE_1.loadAxis.offset,
                        Ms3TableDefinitions.AFR_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            EcuFamily.RUSEFI -> {
                val blob = pages[RusefiTableDefinitions.AFR_TABLE_1.metadata.page] ?: return null
                RusefiTableDefinitions.parseAfrTable(
                    valuesData = blob.sliceRange(
                        RusefiTableDefinitions.AFR_TABLE_1.metadata.offset,
                        RusefiTableDefinitions.AFR_TABLE_1.metadata.totalSize,
                    ),
                    rpmAxisData = blob.sliceRange(
                        RusefiTableDefinitions.AFR_TABLE_1.rpmAxis.offset,
                        RusefiTableDefinitions.AFR_TABLE_1.rpmAxis.count * 2,
                    ),
                    loadAxisData = blob.sliceRange(
                        RusefiTableDefinitions.AFR_TABLE_1.loadAxis.offset,
                        RusefiTableDefinitions.AFR_TABLE_1.loadAxis.count * 2,
                    ),
                )
            }

            else -> {
                val data = pages[5] ?: return null
                val format = AfrTable.StorageFormat.fromTotalSize(data.size)
                val loadType = TableDomainFacade.resolveAfrLoadType(
                    engineConstants = engineConstants,
                    isLegacyFormat = format == AfrTable.StorageFormat.LEGACY_304,
                )
                AfrTable.fromPageData(data, loadType = loadType)
            }
        }
    }

    private fun ByteArray.sliceRange(offset: Int, length: Int): ByteArray {
        val endIndex = (offset + length).coerceAtMost(size)
        if (offset >= endIndex) return ByteArray(0)
        return copyOfRange(offset, endIndex)
    }
}

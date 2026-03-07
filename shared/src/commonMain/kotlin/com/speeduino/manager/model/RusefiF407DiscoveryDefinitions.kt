package com.speeduino.manager.model

/**
 * rusEFI f407-discovery layout from rusefi_f407-discovery.ini.
 */
object RusefiF407DiscoveryDefinitions {
    private const val TABLE_PAGE_CONFIG = 0x0000

    val IGNITION_TABLE_1 = RusefiTableDefinitions.TableLayout(
        metadata = TableMetadata(
            name = "rusEFI f407 Ignition Table 1",
            page = TABLE_PAGE_CONFIG,
            offset = 15136,
            totalSize = 512,
            valuesShape = 16 to 16,
            valuesOffset = 0,
            rpmBinsOffset = 15680,
            loadBinsOffset = 15648,
            valueType = DataType.S16,
            valueRange = -20.0..90.0,
            rpmRange = 0..18000,
            loadRange = 0.0..1000.0,
            units = "deg",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.LITTLE_ENDIAN,
        rpmAxis = RusefiTableDefinitions.AxisLayout(TABLE_PAGE_CONFIG, 15680, 16, DataType.U16),
        loadAxis = RusefiTableDefinitions.AxisLayout(TABLE_PAGE_CONFIG, 15648, 16, DataType.U16),
    )

    val VE_TABLE_1 = RusefiTableDefinitions.TableLayout(
        metadata = TableMetadata(
            name = "rusEFI f407 VE Table 1",
            page = TABLE_PAGE_CONFIG,
            offset = 15712,
            totalSize = 512,
            valuesShape = 16 to 16,
            valuesOffset = 0,
            rpmBinsOffset = 16256,
            loadBinsOffset = 16224,
            valueType = DataType.U16,
            valueRange = 0.0..999.0,
            rpmRange = 0..18000,
            loadRange = 0.0..1000.0,
            units = "%",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.LITTLE_ENDIAN,
        rpmAxis = RusefiTableDefinitions.AxisLayout(TABLE_PAGE_CONFIG, 16256, 16, DataType.U16),
        loadAxis = RusefiTableDefinitions.AxisLayout(TABLE_PAGE_CONFIG, 16224, 16, DataType.U16),
    )

    val AFR_TABLE_1 = RusefiTableDefinitions.TableLayout(
        metadata = TableMetadata(
            name = "rusEFI f407 AFR Table 1",
            page = TABLE_PAGE_CONFIG,
            offset = 16288,
            totalSize = 256,
            valuesShape = 16 to 16,
            valuesOffset = 0,
            rpmBinsOffset = 16576,
            loadBinsOffset = 16544,
            valueType = DataType.U08,
            valueRange = 0.0..25.0,
            rpmRange = 0..18000,
            loadRange = 0.0..1000.0,
            units = "AFR",
            scale = 0.1,
            translate = 0.0,
        ),
        valueByteOrder = EcuByteOrder.LITTLE_ENDIAN,
        rpmAxis = RusefiTableDefinitions.AxisLayout(TABLE_PAGE_CONFIG, 16576, 16, DataType.U16),
        loadAxis = RusefiTableDefinitions.AxisLayout(TABLE_PAGE_CONFIG, 16544, 16, DataType.U16),
    )

    val TABLE_DEFINITIONS = TableDefinitions(
        veTable = VE_TABLE_1.metadata,
        ignitionTable = IGNITION_TABLE_1.metadata,
        afrTable = AFR_TABLE_1.metadata,
        boostTable = null,
        ochBlockSize = 2068,
        nPages = 3,
        era = FirmwareEra.RUSEFI,
        family = EcuFamily.RUSEFI,
        byteOrder = EcuByteOrder.LITTLE_ENDIAN,
    )

    fun parseVeTable(valuesData: ByteArray, rpmAxisData: ByteArray, loadAxisData: ByteArray): VeTable {
        return RusefiTableDefinitions.parseVeTableWithLayout(VE_TABLE_1, valuesData, rpmAxisData, loadAxisData)
    }

    fun parseIgnitionTable(valuesData: ByteArray, rpmAxisData: ByteArray, loadAxisData: ByteArray): IgnitionTable {
        return RusefiTableDefinitions.parseIgnitionTableWithLayout(IGNITION_TABLE_1, valuesData, rpmAxisData, loadAxisData)
    }

    fun parseAfrTable(valuesData: ByteArray, rpmAxisData: ByteArray, loadAxisData: ByteArray): AfrTable {
        return RusefiTableDefinitions.parseAfrTableWithLayout(AFR_TABLE_1, valuesData, rpmAxisData, loadAxisData)
    }

    fun serializeVeTable(table: VeTable): SerializedRusefiTable =
        RusefiTableDefinitions.serializeVeTableWithLayout(VE_TABLE_1, table, signedValues = false, valueScale = 10)

    fun serializeIgnitionTable(table: IgnitionTable): SerializedRusefiTable =
        RusefiTableDefinitions.serializeIgnitionTableWithLayout(IGNITION_TABLE_1, table)

    fun serializeAfrTable(table: AfrTable): SerializedRusefiTable =
        RusefiTableDefinitions.serializeAfrTableWithLayout(AFR_TABLE_1, table)
}

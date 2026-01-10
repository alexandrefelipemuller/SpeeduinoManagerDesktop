package com.speeduino.manager.model


/**
 * VE (Volumetric Efficiency) Table
 * 3D interpolated table that uses RPM and fuel load to lookup the desired VE value
 */
data class VeTable(
    val rpmBins: List<Int>,        // RPM values (columns) - e.g., [500, 1000, 1500, ...]
    val loadBins: List<Int>,       // Load values (rows) - MAP kPa or TPS %
    val values: List<List<Int>>,   // VE values (percentage of Required Fuel)
    val loadType: LoadType = LoadType.MAP
) {
    enum class LoadType {
        MAP,    // Speed Density (MAP kPa)
        TPS     // Alpha-N (TPS %)
    }

    /**
     * Persistent layout used by Speeduino pages.
     *
     * Modern firmwares (2020+) pack the VE values first (256 bytes) and keep
     * axis bins as single-byte entries at the end of the page (total 288 bytes).
     * Very old firmwares used a 304-byte layout with 16-bit RPM bins first.
     */
    enum class StorageFormat(val totalSize: Int) {
        MODERN_288(288),
        LEGACY_304(304);

        companion object {
            fun fromTotalSize(size: Int): StorageFormat? =
                values().firstOrNull { it.totalSize == size }
        }
    }

    companion object {
        private const val MAP_LOAD_SCALE = 2

        /**
         * Create a default 16x16 VE table for testing
         */
        fun createDefault(): VeTable {
            val rpmBins = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000,
                                4500, 5000, 5500, 6000, 6500, 7000, 7500, 8000)
            val loadBins = listOf(10, 20, 30, 40, 50, 60, 70, 80,
                                 90, 100, 110, 120, 130, 140, 150, 160)

            // Create sample VE values (realistic curve)
            val values = loadBins.mapIndexed { rowIndex, load ->
                rpmBins.mapIndexed { colIndex, rpm ->
                    // Simulate realistic VE curve (peaks around mid-range)
                    val baseVE = 75
                    val rpmFactor = when {
                        rpm < 2000 -> -10
                        rpm in 2000..4000 -> 10
                        rpm in 4000..6000 -> 5
                        else -> -5
                    }
                    val loadFactor = (load / 10)
                    (baseVE + rpmFactor + loadFactor).coerceIn(40, 110)
                }
            }

            return VeTable(rpmBins, loadBins, values, LoadType.MAP)
        }

        fun fromPageData(
            data: ByteArray,
            formatHint: StorageFormat? = null,
            loadType: LoadType = LoadType.MAP
        ): VeTable {
            val format = detectFormat(data, formatHint) ?: return createDefault()

            return when (format) {
                StorageFormat.MODERN_288 -> parseModernFormat(data, loadType)
                StorageFormat.LEGACY_304 -> parseLegacyFormat(data, loadType)
            }
        }

        private fun detectFormat(
            data: ByteArray,
            formatHint: StorageFormat?
        ): StorageFormat? {
            formatHint?.let { hint ->
                if (data.size >= hint.totalSize) {
                    return hint
                }
            }

            return when {
                data.size >= StorageFormat.LEGACY_304.totalSize -> StorageFormat.LEGACY_304
                data.size >= StorageFormat.MODERN_288.totalSize -> StorageFormat.MODERN_288
                else -> null
            }
        }

        private fun parseModernFormat(data: ByteArray, loadType: LoadType): VeTable {
            if (data.size < StorageFormat.MODERN_288.totalSize) {
                return createDefault()
            }

            val values = MutableList(16) { row ->
                MutableList(16) { col ->
                    val offset = (row * 16) + col
                    data[offset].toInt() and 0xFF
                }
            }

            val rpmBins = List(16) { i ->
                val raw = data[256 + i].toInt() and 0xFF
                raw * 100
            }

            val loadBins = List(16) { i ->
                rawLoadToUser(data[272 + i].toInt() and 0xFF, loadType)
            }

            return VeTable(rpmBins, loadBins, values, loadType)
        }

        private fun parseLegacyFormat(data: ByteArray, loadType: LoadType): VeTable {
            if (data.size < StorageFormat.LEGACY_304.totalSize) {
                return createDefault()
            }

            val rpmBins = List(16) { i ->
                val offset = i * 2
                val rpmHundreds = ((data[offset].toInt() and 0xFF) shl 8) or
                    (data[offset + 1].toInt() and 0xFF)
                rpmHundreds * 100
            }

            val loadBins = List(16) { i ->
                rawLoadToUser(data[32 + i].toInt() and 0xFF, loadType)
            }

            val values = MutableList(16) { row ->
                MutableList(16) { col ->
                    val offset = 48 + (row * 16) + col
                    data[offset].toInt() and 0xFF
                }
            }

            return VeTable(rpmBins, loadBins, values, loadType)
        }

        private fun rawLoadToUser(rawValue: Int, loadType: LoadType): Int {
            return if (loadType == LoadType.MAP) rawValue * MAP_LOAD_SCALE else rawValue
        }

        private fun userLoadToRaw(userValue: Int, loadType: LoadType): Int {
            return if (loadType == LoadType.MAP) userValue / MAP_LOAD_SCALE else userValue
        }

        /**
         * Get color for VE value (heatmap)
         */
        fun getColorForValue(value: Int): Color {
            return when {
                value < 50 -> Color(0xFF0000FF.toInt()) // Blue - lean
                value < 60 -> Color(0xFF00FFFF.toInt()) // Cyan
                value < 70 -> Color(0xFF00FF00.toInt()) // Green
                value < 80 -> Color(0xFFFFFF00.toInt()) // Yellow
                value < 90 -> Color(0xFFFFA500.toInt()) // Orange
                value < 100 -> Color(0xFFFF4500.toInt()) // Orange-Red
                else -> Color(0xFFFF0000.toInt())        // Red - rich
            }
        }
    }

    /**
     * Get value at specific row/column
     */
    fun getValue(row: Int, col: Int): Int {
        return values.getOrNull(row)?.getOrNull(col) ?: 0
    }

    /**
     * Update value at specific row/column
     */
    fun setValue(row: Int, col: Int, newValue: Int): VeTable {
        val newValues = values.mapIndexed { r, rowValues ->
            if (r == row) {
                rowValues.mapIndexed { c, value ->
                    if (c == col) newValue.coerceIn(0, 255) else value
                }
            } else {
                rowValues
            }
        }
        return copy(values = newValues)
    }

    /**
     * Update RPM bin at specific index
     * @param index 0-15
     * @param newRpm RPM value (100-25500 range, stored as hundredths)
     */
    fun setRpmBin(index: Int, newRpm: Int): VeTable {
        if (index !in 0..15) return this
        val newRpmBins = rpmBins.toMutableList()
        newRpmBins[index] = newRpm.coerceIn(100, 25500)
        return copy(rpmBins = newRpmBins)
    }

    /**
     * Update Load bin at specific index
     * @param index 0-15
     * @param newLoad Load value (0-510 range for MAP kPa, 0-255 for TPS %)
     */
    fun setLoadBin(index: Int, newLoad: Int): VeTable {
        if (index !in 0..15) return this
        val newLoadBins = loadBins.toMutableList()
        newLoadBins[index] = newLoad.coerceIn(0, maxLoadForType())
        return copy(loadBins = newLoadBins)
    }

    /**
     * Convert to byte array for writing to ECU (Page 1 format)
     *
     * Format (304 bytes):
     * - Offset 0-31: 16 RPM bins (U16 big-endian, value in hundreds)
     * - Offset 32-47: 16 Load bins (U08, MAP kPa or TPS %)
     * - Offset 48-303: 16x16 VE values (U08, percentage 0-255)
     */
    fun toByteArray(format: StorageFormat = StorageFormat.MODERN_288): ByteArray {
        return when (format) {
            StorageFormat.MODERN_288 -> serializeModernFormat()
            StorageFormat.LEGACY_304 -> serializeLegacyFormat()
        }
    }

    private fun serializeModernFormat(): ByteArray {
        val data = ByteArray(StorageFormat.MODERN_288.totalSize)

        for (row in 0 until 16) {
            for (col in 0 until 16) {
                val offset = (row * 16) + col
                data[offset] = values[row][col].coerceIn(0, 255).toByte()
            }
        }

        for (i in 0 until 16) {
            val rpmHundreds = (rpmBins[i] / 100).coerceIn(0, 255)
            data[256 + i] = rpmHundreds.toByte()
        }

        for (i in 0 until 16) {
            val rawLoad = userLoadToRaw(loadBins[i], loadType).coerceIn(0, 255)
            data[272 + i] = rawLoad.toByte()
        }

        return data
    }

    private fun serializeLegacyFormat(): ByteArray {
        val data = ByteArray(StorageFormat.LEGACY_304.totalSize)

        for (i in 0 until 16) {
            val rpmHundreds = rpmBins[i] / 100
            val offset = i * 2
            data[offset] = ((rpmHundreds shr 8) and 0xFF).toByte()
            data[offset + 1] = (rpmHundreds and 0xFF).toByte()
        }

        for (i in 0 until 16) {
            val rawLoad = userLoadToRaw(loadBins[i], loadType).coerceIn(0, 255)
            data[32 + i] = rawLoad.toByte()
        }

        for (row in 0 until 16) {
            for (col in 0 until 16) {
                val offset = 48 + (row * 16) + col
                data[offset] = values[row][col].coerceIn(0, 255).toByte()
            }
        }

        return data
    }

    private fun maxLoadForType(): Int {
        return if (loadType == LoadType.MAP) MAP_LOAD_SCALE * 255 else 255
    }
}

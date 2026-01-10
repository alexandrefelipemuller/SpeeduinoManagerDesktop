package com.speeduino.manager.model


/**
 * Ignition Table (Spark Advance)
 * 3D interpolated table that uses RPM and engine load to lookup the desired ignition timing
 */
data class IgnitionTable(
    val rpmBins: List<Int>,        // RPM values (columns) - e.g., [500, 1000, 1500, ...]
    val loadBins: List<Int>,       // Load values (rows) - MAP kPa or TPS %
    val values: List<List<Int>>,   // Ignition advance values (degrees BTDC)
    val loadType: LoadType = LoadType.MAP
) {
    enum class LoadType {
        MAP,    // Speed Density (MAP kPa)
        TPS     // Alpha-N (TPS %)
    }

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
         * Create a default 16x16 Ignition table for testing
         */
        fun createDefault(): IgnitionTable {
            val rpmBins = listOf(500, 650, 790, 930, 1100, 1270, 1440, 1610,
                                1780, 1950, 2360, 2770, 3530, 4290, 5050, 6000)
            val loadBins = listOf(20, 27, 34, 41, 48, 55, 62, 69,
                                 76, 83, 90, 96, 100, 105, 110, 120)

            // Create sample Ignition advance values (realistic spark curve)
            val values = loadBins.mapIndexed { rowIndex, load ->
                rpmBins.mapIndexed { colIndex, rpm ->
                    // Simulate realistic ignition curve
                    // Lower RPM and load = more advance
                    // Higher load = less advance (avoid knock)
                    val baseAdvance = when {
                        rpm < 1000 -> 15   // Idle timing
                        rpm < 2000 -> 25   // Low RPM
                        rpm < 4000 -> 32   // Mid-range
                        else -> 35         // High RPM
                    }

                    val loadReduction = when {
                        load < 40 -> 0      // Light load - no reduction
                        load < 60 -> 2      // Medium load
                        load < 80 -> 5      // Heavy load
                        else -> 10          // Full load - significant reduction
                    }

                    (baseAdvance - loadReduction).coerceIn(5, 45)
                }
            }

            return IgnitionTable(rpmBins, loadBins, values, LoadType.MAP)
        }

        fun fromPageData(
            data: ByteArray,
            formatHint: StorageFormat? = null,
            loadType: LoadType = LoadType.MAP
        ): IgnitionTable {
            val format = detectFormat(data, formatHint) ?: return createDefault()

            return when (format) {
                StorageFormat.MODERN_288 -> parseModern(data, loadType)
                StorageFormat.LEGACY_304 -> parseLegacy(data, loadType)
            }
        }

        private fun detectFormat(
            data: ByteArray,
            hint: StorageFormat?
        ): StorageFormat? {
            hint?.let { expected ->
                if (data.size >= expected.totalSize) return expected
            }

            return when {
                data.size >= StorageFormat.LEGACY_304.totalSize -> StorageFormat.LEGACY_304
                data.size >= StorageFormat.MODERN_288.totalSize -> StorageFormat.MODERN_288
                else -> null
            }
        }

        private fun parseModern(data: ByteArray, loadType: LoadType): IgnitionTable {
            if (data.size < StorageFormat.MODERN_288.totalSize) {
                return createDefault()
            }

            val values = MutableList(16) { row ->
                MutableList(16) { col ->
                    val offset = (row * 16) + col
                    val rawValue = data[offset].toInt() and 0xFF
                    rawValue - 40
                }
            }

            val rpmBins = List(16) { i ->
                val raw = data[256 + i].toInt() and 0xFF
                raw * 100
            }

            val loadBins = List(16) { i ->
                rawLoadToUser(data[272 + i].toInt() and 0xFF, loadType)
            }

            return IgnitionTable(rpmBins, loadBins, values, loadType)
        }

        private fun parseLegacy(data: ByteArray, loadType: LoadType): IgnitionTable {
            if (data.size < StorageFormat.LEGACY_304.totalSize) {
                return createDefault()
            }

            val rpmBins = List(16) { i ->
                val offset = i * 2
                val raw = ((data[offset].toInt() and 0xFF) shl 8) or
                    (data[offset + 1].toInt() and 0xFF)
                raw * 100
            }

            val loadBins = List(16) { i ->
                rawLoadToUser(data[32 + i].toInt() and 0xFF, loadType)
            }

            val values = MutableList(16) { row ->
                MutableList(16) { col ->
                    val offset = 48 + (row * 16) + col
                    val rawValue = data[offset].toInt() and 0xFF
                    rawValue - 40
                }
            }

            return IgnitionTable(rpmBins, loadBins, values, loadType)
        }

        private fun rawLoadToUser(rawValue: Int, loadType: LoadType): Int {
            return if (loadType == LoadType.MAP) rawValue * MAP_LOAD_SCALE else rawValue
        }

        private fun userLoadToRaw(userValue: Int, loadType: LoadType): Int {
            return if (loadType == LoadType.MAP) userValue / MAP_LOAD_SCALE else userValue
        }

        /**
         * Get color for ignition advance value (heatmap)
         */
        fun getColorForValue(value: Int): Color {
            return when {
                value < 10 -> Color(0xFF4A148C.toInt())  // Deep Purple - very little advance
                value < 15 -> Color(0xFF6A1B9A.toInt())  // Purple
                value < 20 -> Color(0xFF1976D2.toInt())  // Blue
                value < 25 -> Color(0xFF0288D1.toInt())  // Light Blue
                value < 30 -> Color(0xFF00ACC1.toInt())  // Cyan
                value < 35 -> Color(0xFF00897B.toInt())  // Teal
                value < 40 -> Color(0xFFFBC02D.toInt())  // Yellow
                else -> Color(0xFFE65100.toInt())        // Deep Orange - high advance
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
    fun setValue(row: Int, col: Int, newValue: Int): IgnitionTable {
        val newValues = values.mapIndexed { r, rowValues ->
            if (r == row) {
                rowValues.mapIndexed { c, value ->
                    // CRITICAL: Range is -40 to 70 degrees (not -10 to 60)
                    if (c == col) newValue.coerceIn(-40, 70) else value
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
    fun setRpmBin(index: Int, newRpm: Int): IgnitionTable {
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
    fun setLoadBin(index: Int, newLoad: Int): IgnitionTable {
        if (index !in 0..15) return this
        val newLoadBins = loadBins.toMutableList()
        newLoadBins[index] = newLoad.coerceIn(0, maxLoadForType())
        return copy(loadBins = newLoadBins)
    }

    /**
     * Convert to byte array for writing to ECU (Page 3 - 304 bytes)
     *
     * Format (same as fromPageData):
     * - Offset 0-31: 16 RPM bins (U16 big-endian, value / 100)
     * - Offset 32-47: 16 Load bins (U08)
     * - Offset 48-303: 16x16 Ignition values (U08, degrees + 40)
     *
     * CRITICAL: Must apply translate offset (+40) when converting to raw bytes
     */
    fun toByteArray(format: StorageFormat = StorageFormat.MODERN_288): ByteArray {
        return when (format) {
            StorageFormat.MODERN_288 -> serializeModern()
            StorageFormat.LEGACY_304 -> serializeLegacy()
        }
    }

    private fun serializeModern(): ByteArray {
        val data = ByteArray(StorageFormat.MODERN_288.totalSize)

        for (row in 0 until 16) {
            for (col in 0 until 16) {
                val offset = (row * 16) + col
                val rawValue = (values[row][col] + 40).coerceIn(0, 255)
                data[offset] = rawValue.toByte()
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

    private fun serializeLegacy(): ByteArray {
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
                val rawValue = (values[row][col] + 40).coerceIn(0, 255)
                data[offset] = rawValue.toByte()
            }
        }

        return data
    }

    private fun maxLoadForType(): Int {
        return if (loadType == LoadType.MAP) MAP_LOAD_SCALE * 255 else 255
    }
}

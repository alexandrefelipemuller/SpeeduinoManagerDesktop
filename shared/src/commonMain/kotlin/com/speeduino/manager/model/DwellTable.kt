package com.speeduino.manager.model

/**
 * Dwell table for ignition coil charging time.
 *
 * This keeps the shared core ready for the Android feature set without
 * forcing an implementation of a specific ECU layout yet.
 */
data class DwellTable(
    val rpmBins: List<Int>,
    val loadBins: List<Int>,
    val values: List<List<Int>>,
    val loadType: LoadType = LoadType.MAP
) {
    enum class LoadType {
        MAP,
        TPS
    }

    companion object {
        private const val DEFAULT_MIN_DWELL = 0
        private const val DEFAULT_MAX_DWELL = 10

        fun createDefault(): DwellTable {
            val rpmBins = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000,
                4500, 5000, 5500, 6000, 6500, 7000, 7500, 8000)
            val loadBins = listOf(20, 30, 40, 50, 60, 70, 80, 90,
                100, 110, 120, 130, 140, 150, 160, 170)
            val values = loadBins.map { load ->
                rpmBins.map { rpm ->
                    when {
                        rpm < 1500 -> 3
                        rpm < 3500 -> 2
                        load > 120 -> 4
                        else -> 2
                    }
                }
            }
            return DwellTable(rpmBins, loadBins, values)
        }
    }

    fun getValue(row: Int, col: Int): Int {
        return values.getOrNull(row)?.getOrNull(col) ?: 0
    }

    fun setValue(row: Int, col: Int, newValue: Int): DwellTable {
        val newValues = values.mapIndexed { r, rowValues ->
            if (r == row) {
                rowValues.mapIndexed { c, value ->
                    if (c == col) newValue.coerceIn(DEFAULT_MIN_DWELL, DEFAULT_MAX_DWELL) else value
                }
            } else {
                rowValues
            }
        }
        return copy(values = newValues)
    }

    fun setRpmBin(index: Int, newRpm: Int): DwellTable {
        if (index !in rpmBins.indices) return this
        val newRpmBins = rpmBins.toMutableList()
        newRpmBins[index] = newRpm.coerceAtLeast(0)
        return copy(rpmBins = newRpmBins)
    }

    fun setLoadBin(index: Int, newLoad: Int): DwellTable {
        if (index !in loadBins.indices) return this
        val newLoadBins = loadBins.toMutableList()
        newLoadBins[index] = newLoad.coerceAtLeast(0)
        return copy(loadBins = newLoadBins)
    }
}

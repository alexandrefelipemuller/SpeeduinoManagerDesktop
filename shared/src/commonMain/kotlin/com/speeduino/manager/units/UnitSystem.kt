package com.speeduino.manager.units

import java.util.Locale

enum class UnitSystem(val storageValue: String) {
    AUTO("auto"),
    METRIC("metric"),
    IMPERIAL("imperial");

    companion object {
        fun fromStorage(value: String?): UnitSystem {
            return entries.firstOrNull { it.storageValue == value } ?: AUTO
        }
    }
}

fun defaultUnitSystemForLocale(locale: Locale = Locale.getDefault()): UnitSystem {
    return when (locale.country.uppercase(Locale.US)) {
        "US", "LR", "MM" -> UnitSystem.IMPERIAL
        else -> UnitSystem.METRIC
    }
}

fun resolveEffectiveUnitSystem(selected: UnitSystem, locale: Locale = Locale.getDefault()): UnitSystem {
    return if (selected == UnitSystem.AUTO) {
        defaultUnitSystemForLocale(locale)
    } else {
        selected
    }
}

object UnitConverter {
    private const val KPA_TO_PSI = 0.1450377377
    private const val KMH_TO_MPH = 0.6213711922

    fun convertValue(value: Double, unit: String, system: UnitSystem): Pair<Double, String> {
        if (system != UnitSystem.IMPERIAL) {
            return value to unit
        }

        return when (unit.trim().lowercase(Locale.US)) {
            "kpa" -> value * KPA_TO_PSI to "psi"
            "km/h", "kph" -> value * KMH_TO_MPH to "mph"
            "°c", "c" -> (value * 9.0 / 5.0 + 32.0) to "°F"
            else -> value to unit
        }
    }
}

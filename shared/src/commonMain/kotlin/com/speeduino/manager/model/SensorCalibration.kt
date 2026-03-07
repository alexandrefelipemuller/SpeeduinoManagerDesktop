package com.speeduino.manager.model

data class PressureCalibration(
    val mapMin: Int,
    val mapMax: Int,
    val baroMin: Int,
    val baroMax: Int,
    val emapMin: Int,
    val emapMax: Int
)

data class TpsCalibration(
    val tpsMin: Int,
    val tpsMax: Int
)

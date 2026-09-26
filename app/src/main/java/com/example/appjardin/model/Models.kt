package com.example.appjardin.model

data class Telemetry(
    val humedad: Float,
    val bomba: Boolean,
    val conectado: Boolean
)

data class Config(
    val inicioRiego: Int,
    val finRiego: Int,
    val recomendadaMax: Int,
    val exceso: Int
)

enum class MoistureState {
    NO_PLANT,
    LOW_MOISTURE,
    MEDIUM_MOISTURE,
    GOOD_MOISTURE,
    EXCESS_MOISTURE
}

package com.example.appjardin.model

data class Telemetry(
    val humedad: Float = 0f,
    val bomba: Boolean = false,
    val conectado: Boolean = true,
    val exceso: Boolean = false
)

data class Config(
    val humedadMinima: Int,
    val humedadBuena: Int,
    val humedadExceso: Int
)

data class ActionCommand(
    val accion: String
)

enum class MoistureState {
    NO_PLANT,
    LOW_MOISTURE,
    MEDIUM_MOISTURE,
    GOOD_MOISTURE,
    EXCESS_MOISTURE
}

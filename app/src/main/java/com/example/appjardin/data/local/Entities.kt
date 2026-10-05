package com.example.appjardin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val humedadMinima: Int,
    val humedadBuena: Int,
    val humedadExceso: Int,
    val imagePath: String? = null,
    val defaultKey: String? = null,
    val isUserCreated: Boolean = false
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val plantName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val humidities: String, // Comma separated values or JSON
    val humedadMasBaja: Float = 0f,
    val humedadMasAlta: Float = 100f
)

@Entity(tableName = "diagnostics")
data class DiagnosisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int?,
    val plantName: String,
    val imagePath: String,
    val result: String,
    val confidence: Float,
    val createdAt: Long,
    val modelVersion: String
)

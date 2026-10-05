package com.example.appjardin.domain.ai

import android.graphics.Bitmap

enum class DiagnosisResult {
    HEALTHY, UNHEALTHY
}

data class DiagnosisPrediction(
    val result: DiagnosisResult,
    val confidence: Float
)

interface DiagnosisClassifier {
    suspend fun classify(bitmap: Bitmap): DiagnosisPrediction
    fun close()
}

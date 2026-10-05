package com.example.appjardin.domain.ai

import android.graphics.Bitmap
import kotlinx.coroutines.delay

class FakeDiagnosisClassifier : DiagnosisClassifier {
    private var toggle = false

    override suspend fun classify(bitmap: Bitmap): DiagnosisPrediction {
        delay(1500) // Simulate processing time
        toggle = !toggle
        return if (toggle) {
            DiagnosisPrediction(DiagnosisResult.HEALTHY, 0.95f)
        } else {
            DiagnosisPrediction(DiagnosisResult.UNHEALTHY, 0.88f)
        }
    }

    override fun close() {
        // No-op
    }
}

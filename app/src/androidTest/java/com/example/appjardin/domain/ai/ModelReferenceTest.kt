package com.example.appjardin.domain.ai

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelReferenceTest {

    // Expected probability values provided by user
    private val EXPECTED_HEALTHY_PROB = 0.950f
    private val EXPECTED_UNHEALTHY_PROB = 0.050f
    private val TOLERANCE = 1e-3f

    @Test
    @Ignore("Ignorado por defecto: requiere que el usuario aporte la imagen de prueba 'test_leaf.jpg' en src/androidTest/assets/ y configure las probabilidades esperadas.")
    fun testModelReferenceInference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val classifier = LiteRtDiagnosisClassifier(context)

        // Load reference image from assets
        val inputStream = context.assets.open("test_leaf.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)

        runBlocking {
            val prediction = classifier.classify(bitmap)

            if (prediction.result == DiagnosisResult.HEALTHY) {
                assertEquals(EXPECTED_HEALTHY_PROB, prediction.confidence, TOLERANCE)
            } else {
                assertEquals(EXPECTED_UNHEALTHY_PROB, prediction.confidence, TOLERANCE)
            }
        }

        classifier.close()
    }
}

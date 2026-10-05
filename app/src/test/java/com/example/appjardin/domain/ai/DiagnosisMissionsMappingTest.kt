package com.example.appjardin.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisMissionsMappingTest {

    private fun calculateProgress(count: Int, target: Int): Float {
        if (target <= 0) return 0f
        return (minOf(count, target).toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }

    private fun isCompleted(progress: Float): Boolean {
        return progress >= 1.0f
    }

    @Test
    fun testM4DailyDiagnosisMapping() {
        val target = 1

        // Count = 0
        var count = 0
        var progress = calculateProgress(count, target)
        assertEquals(0.0f, progress, 1e-4f)
        assertFalse(isCompleted(progress))

        // Count >= 1 (e.g. 1)
        count = 1
        progress = calculateProgress(count, target)
        assertEquals(1.0f, progress, 1e-4f)
        assertTrue(isCompleted(progress))

        // Count = 5
        count = 5
        progress = calculateProgress(count, target)
        assertEquals(1.0f, progress, 1e-4f)
        assertTrue(isCompleted(progress))
    }

    @Test
    fun testM20ToM24CumulativeDiagnosticsMapping() {
        val testCounts = listOf(0, 1, 4, 5, 9, 10, 24, 25, 49, 50)
        
        // m20: target 1
        val m20Target = 1
        val m20Expected = listOf(
            0.0f to false,  // 0
            1.0f to true,   // 1
            1.0f to true,   // 4
            1.0f to true,   // 5
            1.0f to true,   // 9
            1.0f to true,   // 10
            1.0f to true,   // 24
            1.0f to true,   // 25
            1.0f to true,   // 49
            1.0f to true    // 50
        )
        testCounts.forEachIndexed { idx, c ->
            val p = calculateProgress(c, m20Target)
            assertEquals("m20 count $c", m20Expected[idx].first, p, 1e-4f)
            assertEquals("m20 completed $c", m20Expected[idx].second, isCompleted(p))
        }

        // m21: target 5
        val m21Target = 5
        val m21Expected = listOf(
            0.0f to false,  // 0
            0.2f to false,  // 1
            0.8f to false,  // 4
            1.0f to true,   // 5
            1.0f to true,   // 9
            1.0f to true,   // 10
            1.0f to true,   // 24
            1.0f to true,   // 25
            1.0f to true,   // 49
            1.0f to true    // 50
        )
        testCounts.forEachIndexed { idx, c ->
            val p = calculateProgress(c, m21Target)
            assertEquals("m21 count $c", m21Expected[idx].first, p, 1e-4f)
            assertEquals("m21 completed $c", m21Expected[idx].second, isCompleted(p))
        }

        // m22: target 10
        val m22Target = 10
        val m22Expected = listOf(
            0.0f to false,  // 0
            0.1f to false,  // 1
            0.4f to false,  // 4
            0.5f to false,  // 5
            0.9f to false,  // 9
            1.0f to true,   // 10
            1.0f to true,   // 24
            1.0f to true,   // 25
            1.0f to true,   // 49
            1.0f to true    // 50
        )
        testCounts.forEachIndexed { idx, c ->
            val p = calculateProgress(c, m22Target)
            assertEquals("m22 count $c", m22Expected[idx].first, p, 1e-4f)
            assertEquals("m22 completed $c", m22Expected[idx].second, isCompleted(p))
        }

        // m23: target 25
        val m23Target = 25
        val m23Expected = listOf(
            0.0f to false,   // 0
            0.04f to false,  // 1
            0.16f to false,  // 4
            0.20f to false,  // 5
            0.36f to false,  // 9
            0.40f to false,  // 10
            0.96f to false,  // 24
            1.00f to true,   // 25
            1.00f to true,   // 49
            1.00f to true    // 50
        )
        testCounts.forEachIndexed { idx, c ->
            val p = calculateProgress(c, m23Target)
            assertEquals("m23 count $c", m23Expected[idx].first, p, 1e-4f)
            assertEquals("m23 completed $c", m23Expected[idx].second, isCompleted(p))
        }

        // m24: target 50
        val m24Target = 50
        val m24Expected = listOf(
            0.0f to false,   // 0
            0.02f to false,  // 1
            0.08f to false,  // 4
            0.10f to false,  // 5
            0.18f to false,  // 9
            0.20f to false,  // 10
            0.48f to false,  // 24
            0.50f to false,  // 25
            0.98f to false,  // 49
            1.00f to true    // 50
        )
        testCounts.forEachIndexed { idx, c ->
            val p = calculateProgress(c, m24Target)
            assertEquals("m24 count $c", m24Expected[idx].first, p, 1e-4f)
            assertEquals("m24 completed $c", m24Expected[idx].second, isCompleted(p))
        }
    }
}

package com.example.appjardin.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class PostprocessorTest {

    @Test
    fun testSoftmax() {
        val logits = floatArrayOf(2.0f, 1.0f, 0.1f)
        val probs = Postprocessor.softmax(logits)

        var sum = 0f
        for (p in probs) {
            sum += p
        }

        assertEquals(1.0f, sum, 1e-4f)
        assert(probs[0] > probs[1])
        assert(probs[1] > probs[2])
    }

    @Test
    fun testProcessOutputsAlreadyProbabilities() {
        val input = floatArrayOf(0.85f, 0.15f)
        val (maxIdx, maxProb) = Postprocessor.processOutputs(input)

        assertEquals(0, maxIdx)
        assertEquals(0.85f, maxProb, 1e-4f)
    }

    @Test
    fun testProcessOutputsLogits() {
        val input = floatArrayOf(3.0f, 1.0f)
        val (maxIdx, maxProb) = Postprocessor.processOutputs(input)

        assertEquals(0, maxIdx)
        assert(maxProb > 0.8f)
    }
}

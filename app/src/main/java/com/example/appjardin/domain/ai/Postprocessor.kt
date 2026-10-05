package com.example.appjardin.domain.ai

import kotlin.math.abs
import kotlin.math.exp

object Postprocessor {
    fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = logits[0]
        for (i in 1 until logits.size) {
            if (logits[i] > maxLogit) maxLogit = logits[i]
        }
        var sum = 0f
        val expArr = FloatArray(logits.size)
        for (i in logits.indices) {
            expArr[i] = exp(logits[i] - maxLogit)
            sum += expArr[i]
        }
        for (i in expArr.indices) {
            expArr[i] /= sum
        }
        return expArr
    }

    fun processOutputs(outputValues: FloatArray): Pair<Int, Float> {
        val sum = outputValues.sum()
        val probs = if (abs(sum - 1.0f) < 0.05f) {
            outputValues
        } else {
            softmax(outputValues)
        }

        var maxIdx = 0
        var maxProb = probs[0]
        for (i in 1 until probs.size) {
            if (probs[i] > maxProb) {
                maxProb = probs[i]
                maxIdx = i
            }
        }
        return Pair(maxIdx, maxProb)
    }
}

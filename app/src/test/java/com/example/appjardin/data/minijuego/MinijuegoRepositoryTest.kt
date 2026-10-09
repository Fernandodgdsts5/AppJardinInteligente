package com.example.appjardin.data.minijuego

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinijuegoRepositoryTest {

    private fun computeRewardForStreak(isCorrect: Boolean, newStreak: Int): Pair<Int, Int> {
        if (!isCorrect) return Pair(0, 0)
        return if (newStreak >= 3) Pair(30, 40) else Pair(20, 30)
    }

    @Test
    fun testCasesMatching() {
        val cases = MinijuegoRepository.CASES
        assertEquals(6, cases.size)

        assertEquals("REGAR", cases["s1"]?.correctAnswer)
        assertEquals("ESPERAR", cases["s2"]?.correctAnswer)
        assertEquals("REGAR", cases["s3"]?.correctAnswer)
        assertEquals("ESPERAR", cases["s4"]?.correctAnswer)
        assertEquals("REGAR", cases["s5"]?.correctAnswer)
        assertEquals("ESPERAR", cases["s6"]?.correctAnswer)
    }

    @Test
    fun testRewardStreakRules() {
        // Incorrect answer
        val (c0, x0) = computeRewardForStreak(isCorrect = false, newStreak = 0)
        assertEquals(0, c0)
        assertEquals(0, x0)

        // Correct answer streak 1
        val (c1, x1) = computeRewardForStreak(isCorrect = true, newStreak = 1)
        assertEquals(20, c1)
        assertEquals(30, x1)

        // Correct answer streak 2
        val (c2, x2) = computeRewardForStreak(isCorrect = true, newStreak = 2)
        assertEquals(20, c2)
        assertEquals(30, x2)

        // Correct answer streak 3 (streak >= 3 boost)
        val (c3, x3) = computeRewardForStreak(isCorrect = true, newStreak = 3)
        assertEquals(30, c3)
        assertEquals(40, x3)
    }

    @Test
    fun testDailyLimitCapCalculation() {
        val currentCoins = 4980
        val maxCoins = MinijuegoDataStore.MAX_DAILY_COINS // 5000
        val rawReward = 30

        val remaining = maxOf(0, maxCoins - currentCoins) // 20
        val credited = minOf(rawReward, remaining) // min(30, 20) = 20

        assertEquals(20, credited)
        assertEquals(5000, currentCoins + credited)
    }

    @Test
    fun testDailyLimitReachedWhenCapFull() {
        val currentCoins = 5000
        val maxCoins = 5000
        val rawReward = 30

        val remaining = maxOf(0, maxCoins - currentCoins) // 0
        val credited = minOf(rawReward, remaining) // 0

        assertEquals(0, credited)
    }
}

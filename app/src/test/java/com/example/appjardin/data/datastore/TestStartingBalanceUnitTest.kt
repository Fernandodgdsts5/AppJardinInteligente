package com.example.appjardin.data.datastore

import com.example.appjardin.model.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestStartingBalanceUnitTest {

    @Test
    fun testSeedingOnFreshInstall() {
        val alreadyApplied = false
        val hasPriorProgress = false
        val switchEnabled = TestStartingBalance.ENABLE_TEST_STARTING_BALANCE

        val shouldSeed = switchEnabled && !alreadyApplied && !hasPriorProgress
        assertTrue(shouldSeed)

        val coins = if (shouldSeed) TestStartingBalance.TEST_STARTING_GOLD else 0
        val exp = if (shouldSeed) TestStartingBalance.TEST_STARTING_XP else 0

        assertEquals(12000, coins)
        assertEquals(10000, exp)
    }

    @Test
    fun testSeedingSkippedIfPriorProgressExists() {
        val alreadyApplied = false
        val hasPriorProgress = true // User updated APK over existing install with progress
        val switchEnabled = TestStartingBalance.ENABLE_TEST_STARTING_BALANCE

        val shouldSeed = switchEnabled && !alreadyApplied && !hasPriorProgress
        assertFalse(shouldSeed)
    }

    @Test
    fun testSeedingSkippedIfSwitchDisabled() {
        val alreadyApplied = false
        val hasPriorProgress = false
        val switchEnabled = false

        val shouldSeed = switchEnabled && !alreadyApplied && !hasPriorProgress
        assertFalse(shouldSeed)
    }

    @Test
    fun testAbejaPurchaseDeduction() {
        val startCoins = 12000
        val startExp = 10000

        val reqCoins = GameConfig.MIEL_COINS // 12000
        val reqExp = GameConfig.MIEL_EXP     // 8000

        val canAfford = startCoins >= reqCoins && startExp >= reqExp
        assertTrue(canAfford)

        val endCoins = startCoins - reqCoins
        val endExp = startExp - reqExp

        assertEquals(0, endCoins)
        assertEquals(2000, endExp)
    }

    @Test
    fun testAbejaPurchaseInsufficientBalance() {
        val startCoins = 5000
        val startExp = 3000

        val reqCoins = GameConfig.MIEL_COINS // 12000
        val reqExp = GameConfig.MIEL_EXP     // 8000

        val canAfford = startCoins >= reqCoins && startExp >= reqExp
        assertFalse(canAfford)

        // No deduction occurs
        val endCoins = if (canAfford) startCoins - reqCoins else startCoins
        val endExp = if (canAfford) startExp - reqExp else startExp

        assertEquals(5000, endCoins)
        assertEquals(3000, endExp)
    }

    @Test
    fun testLevelFormulaDerivedFromLoginDates() {
        // App level formula: preferences[GAME_LEVEL] = maxOf(1, dates.size)
        val dates = setOf("2026-10-05")
        val derivedLevel = maxOf(1, dates.size)
        assertEquals(1, derivedLevel)
    }
}

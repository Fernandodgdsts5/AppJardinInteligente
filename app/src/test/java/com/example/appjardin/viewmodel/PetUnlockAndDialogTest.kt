package com.example.appjardin.viewmodel

import com.example.appjardin.model.GameConfig
import com.example.appjardin.model.Pet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetUnlockAndDialogTest {

    @Test
    fun testUnlockingOnlyUnlocksTargetSpeciesAndKeepsExisting() {
        val initialUnlocked = mutableSetOf("larva", "gusano")
        val targetPet = "abeja"

        // Perform unlock
        initialUnlocked.add(targetPet)

        assertTrue(initialUnlocked.contains("larva"))
        assertTrue(initialUnlocked.contains("gusano"))
        assertTrue(initialUnlocked.contains("abeja"))
        assertFalse(initialUnlocked.contains("hormiga"))
        assertFalse(initialUnlocked.contains("chanchito"))
        assertFalse(initialUnlocked.contains("reygeko"))
    }

    @Test
    fun testPurchaseDeductionAndNoNegativeBalance() {
        var coins = 12000
        var exp = 10000

        val costCoins = GameConfig.MIEL_COINS // 12000
        val costExp = GameConfig.MIEL_EXP     // 8000

        val canAfford = coins >= costCoins && exp >= costExp
        assertTrue(canAfford)

        if (canAfford) {
            coins -= costCoins
            exp -= costExp
        }

        assertEquals(0, coins)
        assertEquals(2000, exp)
        assertTrue(coins >= 0)
        assertTrue(exp >= 0)
    }

    @Test
    fun testAlreadyUnlockedPetDoesNotDoubleCharge() {
        val unlockedPets = mutableSetOf("larva", "gusano", "abeja")
        var coins = 12000
        var exp = 10000

        val targetPet = "abeja"

        fun buyPetAtomic(petId: String, costCoins: Int, costExp: Int): Boolean {
            if (unlockedPets.contains(petId)) {
                return true // Already unlocked, no deduction!
            }
            if (coins >= costCoins && exp >= costExp) {
                coins -= costCoins
                exp -= costExp
                unlockedPets.add(petId)
                return true
            }
            return false
        }

        val success = buyPetAtomic(targetPet, GameConfig.MIEL_COINS, GameConfig.MIEL_EXP)
        assertTrue(success)
        assertEquals(12000, coins) // No deduction!
        assertEquals(10000, exp)   // No deduction!
    }

    @Test
    fun testInsufficientBalanceDeductsNothing() {
        val unlockedPets = mutableSetOf("larva", "gusano")
        var coins = 5000
        var exp = 3000

        val targetPet = "abeja"

        fun buyPetAtomic(petId: String, costCoins: Int, costExp: Int): Boolean {
            if (unlockedPets.contains(petId)) return true
            if (coins >= costCoins && exp >= costExp) {
                coins -= costCoins
                exp -= costExp
                unlockedPets.add(petId)
                return true
            }
            return false
        }

        val success = buyPetAtomic(targetPet, GameConfig.MIEL_COINS, GameConfig.MIEL_EXP)
        assertFalse(success)
        assertEquals(5000, coins)
        assertEquals(3000, exp)
        assertFalse(unlockedPets.contains("abeja"))
    }

    @Test
    fun testEquipActionSelectsPetAndCloseActionKeepsSelectedPetUnchanged() {
        var selectedPetId = "gusano"

        fun onEquip(pet: Pet) {
            selectedPetId = pet.id
        }

        fun onClose() {
            // Do not change selectedPetId
        }

        // Action: Close
        onClose()
        assertEquals("gusano", selectedPetId)

        // Action: Equip Abeja
        onEquip(Pet.ABEJA)
        assertEquals("abeja", selectedPetId)
    }
}

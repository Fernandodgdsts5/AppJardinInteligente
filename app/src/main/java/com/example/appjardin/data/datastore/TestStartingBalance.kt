package com.example.appjardin.data.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/*
 * SOLO PRUEBAS: Carga de saldo inicial de pruebas (12.000 oro, 10.000 exp) en la primera instalación.
 * Eliminar o cambiar ENABLE_TEST_STARTING_BALANCE = false antes de publicar.
 */
object TestStartingBalance {
    const val ENABLE_TEST_STARTING_BALANCE = true
    const val TEST_STARTING_GOLD = 12000
    const val TEST_STARTING_XP = 10000

    private val STARTING_BALANCE_APPLIED = booleanPreferencesKey("starting_balance_applied")
    private val GAME_COINS = intPreferencesKey("game_coins")
    private val GAME_EXP = intPreferencesKey("game_exp")
    private val COINS_EARNED_TOTAL = intPreferencesKey("coins_earned_total")
    private val CHESTS_OPENED_COUNT = intPreferencesKey("chests_opened_count")
    private val GAME_UNLOCKED_PETS = stringSetPreferencesKey("game_unlocked_pets")

    suspend fun applyIfFirstInstall(context: Context) {
        if (!ENABLE_TEST_STARTING_BALANCE) return

        try {
            context.dataStore.edit { preferences ->
                val alreadyApplied = preferences[STARTING_BALANCE_APPLIED] ?: false
                if (alreadyApplied) return@edit

                // Check for existing economic progress
                val currentCoins = preferences[GAME_COINS] ?: 0
                val currentExp = preferences[GAME_EXP] ?: 0
                val coinsEarned = preferences[COINS_EARNED_TOTAL] ?: 0
                val chestsOpened = preferences[CHESTS_OPENED_COUNT] ?: 0
                val unlockedPets = preferences[GAME_UNLOCKED_PETS] ?: setOf("larva", "gusano")

                val hasPriorProgress = currentCoins > 0 || currentExp > 0 || coinsEarned > 0 ||
                        chestsOpened > 0 || unlockedPets.size > 2

                if (!hasPriorProgress) {
                    // Fresh install: apply test starting balance
                    preferences[GAME_COINS] = TEST_STARTING_GOLD
                    preferences[GAME_EXP] = TEST_STARTING_XP
                    preferences[COINS_EARNED_TOTAL] = TEST_STARTING_GOLD
                    Log.d("TestStartingBalance", "Seeding starting balance: $TEST_STARTING_GOLD coins, $TEST_STARTING_XP xp")
                } else {
                    Log.d("TestStartingBalance", "Prior progress detected. Skipping seeding.")
                }

                // Mark flag as applied so it never runs again
                preferences[STARTING_BALANCE_APPLIED] = true
            }
        } catch (e: Exception) {
            Log.e("TestStartingBalance", "Error applying test starting balance", e)
        }
    }
}

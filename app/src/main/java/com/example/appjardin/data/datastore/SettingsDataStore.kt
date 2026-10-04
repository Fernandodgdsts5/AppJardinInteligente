package com.example.appjardin.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val SELECTED_PLANT_ID = intPreferencesKey("selected_plant_id")
        val LAST_DISCONNECT_TIME = longPreferencesKey("last_disconnect_time")
        val DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")
        val SELECTED_PET_ID = stringPreferencesKey("selected_pet_id")
        val LEGACY_PET_NAME = stringPreferencesKey("pet_name")

        val GAME_COINS = intPreferencesKey("game_coins")
        val GAME_EXP = intPreferencesKey("game_exp")
        val GAME_LEVEL = intPreferencesKey("game_level")
        val GAME_LAST_DATE = stringPreferencesKey("game_last_date")
        val GAME_UNLOCKED_PETS = stringSetPreferencesKey("game_unlocked_pets")
        val DIAGNOSTICS_COUNT = intPreferencesKey("diagnostics_count")
        val PLANT_PHOTO_SET = booleanPreferencesKey("plant_photo_set")
        val PET_RENAMED = booleanPreferencesKey("pet_renamed")

        fun getPetNameKey(petId: String): Preferences.Key<String> {
            return stringPreferencesKey("pet_name_$petId")
        }

        fun getMissionClaimedKey(missionId: String): Preferences.Key<Boolean> {
            return booleanPreferencesKey("mission_claimed_$missionId")
        }
    }

    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME] ?: "Usuario"
        }

    val selectedPlantIdFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_PLANT_ID] ?: -1
        }

    val lastDisconnectTimeFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_DISCONNECT_TIME] ?: 0L
        }

    val defaultsSeededFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULTS_SEEDED] ?: false
        }

    val selectedPetIdFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_PET_ID] ?: "gusano"
        }

    val coinsFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[GAME_COINS] ?: 0
        }

    val expFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[GAME_EXP] ?: 0
        }

    val levelFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[GAME_LEVEL] ?: 1
        }

    val lastDateFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GAME_LAST_DATE] ?: ""
        }

    val unlockedPetsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[GAME_UNLOCKED_PETS] ?: setOf("larva", "gusano")
        }

    val diagnosticsCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DIAGNOSTICS_COUNT] ?: 0
        }

    val plantPhotoSetFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PLANT_PHOTO_SET] ?: false
        }

    val petRenamedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PET_RENAMED] ?: false
        }

    fun getMissionClaimedFlow(missionId: String): Flow<Boolean> {
        val key = getMissionClaimedKey(missionId)
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    fun getPetNameFlow(petId: String, defaultName: String): Flow<String> {
        val key = getPetNameKey(petId)
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultName
        }
    }

    suspend fun checkAndMigrateLegacyPetName() {
        context.dataStore.edit { preferences ->
            val legacyName = preferences[LEGACY_PET_NAME]
            if (!legacyName.isNullOrBlank() && legacyName != "Menta") {
                val currentPetId = preferences[SELECTED_PET_ID] ?: "gusano"
                preferences[getPetNameKey(currentPetId)] = legacyName
            }
            preferences.remove(LEGACY_PET_NAME)
        }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun saveSelectedPlantId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PLANT_ID] = id
        }
    }

    suspend fun saveLastDisconnectTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_DISCONNECT_TIME] = time
        }
    }

    suspend fun setDefaultsSeeded(seeded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULTS_SEEDED] = seeded
        }
    }

    suspend fun saveSelectedPetId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PET_ID] = id
        }
    }

    suspend fun savePetName(petId: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[getPetNameKey(petId)] = name
        }
    }

    suspend fun resetPetName(petId: String) {
        context.dataStore.edit { preferences ->
            preferences.remove(getPetNameKey(petId))
        }
    }

    suspend fun addRewards(coins: Int, exp: Int) {
        context.dataStore.edit { preferences ->
            val currentCoins = preferences[GAME_COINS] ?: 0
            val currentExp = preferences[GAME_EXP] ?: 0
            preferences[GAME_COINS] = currentCoins + coins
            preferences[GAME_EXP] = currentExp + exp
        }
    }

    suspend fun deductResources(coins: Int, exp: Int) {
        context.dataStore.edit { preferences ->
            val currentCoins = preferences[GAME_COINS] ?: 0
            val currentExp = preferences[GAME_EXP] ?: 0
            preferences[GAME_COINS] = maxOf(0, currentCoins - coins)
            preferences[GAME_EXP] = maxOf(0, currentExp - exp)
        }
    }

    suspend fun unlockPet(petId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[GAME_UNLOCKED_PETS] ?: setOf("larva", "gusano")
            preferences[GAME_UNLOCKED_PETS] = current + petId
        }
    }

    suspend fun setMissionClaimed(missionId: String, claimed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[getMissionClaimedKey(missionId)] = claimed
        }
    }

    suspend fun updateLoginStreak(todayStr: String) {
        context.dataStore.edit { preferences ->
            val lastDate = preferences[GAME_LAST_DATE] ?: ""
            if (lastDate != todayStr) {
                // If system date is not older than last date
                if (lastDate.isEmpty() || todayStr > lastDate) {
                    val currentLevel = preferences[GAME_LEVEL] ?: 1
                    preferences[GAME_LEVEL] = currentLevel + 1
                    preferences[GAME_LAST_DATE] = todayStr
                }
            }
        }
    }

    suspend fun incrementDiagnostics() {
        context.dataStore.edit { preferences ->
            val current = preferences[DIAGNOSTICS_COUNT] ?: 0
            preferences[DIAGNOSTICS_COUNT] = current + 1
        }
    }

    suspend fun setPlantPhotoSet() {
        context.dataStore.edit { preferences ->
            preferences[PLANT_PHOTO_SET] = true
        }
    }

    suspend fun setPetRenamed() {
        context.dataStore.edit { preferences ->
            preferences[PET_RENAMED] = true
        }
    }
}

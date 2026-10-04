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
import com.example.appjardin.model.GameConfig
import com.example.appjardin.model.RewardType
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
        
        val GAME_CATALOG_VERSION = intPreferencesKey("game_catalog_version")
        val GAME_LOGIN_DATES = stringSetPreferencesKey("game_login_dates")
        val DIAGNOSTICS_COUNT = intPreferencesKey("diagnostics_count")
        val MANUAL_WATERINGS_COUNT = intPreferencesKey("manual_waterings_count")
        val THRESHOLD_EDITS_COUNT = intPreferencesKey("threshold_edits_count")
        val PET_SELECTION_CHANGES_COUNT = intPreferencesKey("pet_selection_changes_count")
        val COINS_EARNED_TOTAL = intPreferencesKey("coins_earned_total")
        val COINS_SPENT_PET_TOTAL = intPreferencesKey("coins_spent_pet_total")
        val CHESTS_OPENED_COUNT = intPreferencesKey("chests_opened_count")
        val BLE_CONNECTED_ONCE = booleanPreferencesKey("ble_connected_once")
        val PLANT_PHOTO_SET = booleanPreferencesKey("plant_photo_set")
        val PET_RENAMED = booleanPreferencesKey("pet_renamed")

        fun getPetNameKey(petId: String): Preferences.Key<String> {
            return stringPreferencesKey("pet_name_$petId")
        }

        fun getMissionClaimedKey(missionId: String, isDaily: Boolean, dateStr: String): Preferences.Key<Boolean> {
            val name = if (isDaily) "mission_claimed_${missionId}_$dateStr" else "mission_claimed_$missionId"
            return booleanPreferencesKey(name)
        }

        fun getDailyActionKey(actionName: String, dateStr: String): Preferences.Key<Boolean> {
            return booleanPreferencesKey("action_${actionName}_$dateStr")
        }
    }

    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME] ?: "Usuario" }

    val selectedPlantIdFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_PLANT_ID] ?: -1 }

    val lastDisconnectTimeFlow: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_DISCONNECT_TIME] ?: 0L }

    val defaultsSeededFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DEFAULTS_SEEDED] ?: false }

    val selectedPetIdFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_PET_ID] ?: "gusano" }

    val coinsFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[GAME_COINS] ?: 0 }

    val expFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[GAME_EXP] ?: 0 }

    val levelFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[GAME_LEVEL] ?: 1 }

    val loginDatesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[GAME_LOGIN_DATES] ?: emptySet() }

    val unlockedPetsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[GAME_UNLOCKED_PETS] ?: setOf("larva", "gusano") }

    val diagnosticsCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[DIAGNOSTICS_COUNT] ?: 0 }

    val manualWateringsCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[MANUAL_WATERINGS_COUNT] ?: 0 }

    val thresholdEditsCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[THRESHOLD_EDITS_COUNT] ?: 0 }

    val petSelectionChangesCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PET_SELECTION_CHANGES_COUNT] ?: 0 }

    val coinsEarnedTotalFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[COINS_EARNED_TOTAL] ?: 0 }

    val coinsSpentPetTotalFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[COINS_SPENT_PET_TOTAL] ?: 0 }

    val chestsOpenedCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[CHESTS_OPENED_COUNT] ?: 0 }

    val bleConnectedOnceFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BLE_CONNECTED_ONCE] ?: false }

    val plantPhotoSetFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PLANT_PHOTO_SET] ?: false }

    val petRenamedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PET_RENAMED] ?: false }

    fun getMissionClaimedFlow(missionId: String, isDaily: Boolean, dateStr: String): Flow<Boolean> {
        val key = getMissionClaimedKey(missionId, isDaily, dateStr)
        return context.dataStore.data.map { preferences -> preferences[key] ?: false }
    }

    fun getDailyActionFlow(actionName: String, dateStr: String): Flow<Boolean> {
        val key = getDailyActionKey(actionName, dateStr)
        return context.dataStore.data.map { preferences -> preferences[key] ?: false }
    }

    fun getPetNameFlow(petId: String, defaultName: String): Flow<String> {
        val key = getPetNameKey(petId)
        return context.dataStore.data.map { preferences -> preferences[key] ?: defaultName }
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

    suspend fun checkCatalogVersionAndSanitize(): Int {
        var corrected = 0
        context.dataStore.edit { preferences ->
            val savedVersion = preferences[GAME_CATALOG_VERSION] ?: 1
            if (savedVersion < GameConfig.GAME_CATALOG_VERSION) {
                preferences[DIAGNOSTICS_COUNT] = 0
                preferences[MANUAL_WATERINGS_COUNT] = 0
                preferences[THRESHOLD_EDITS_COUNT] = 0
                preferences[PET_SELECTION_CHANGES_COUNT] = 0
                corrected++
                preferences[GAME_CATALOG_VERSION] = GameConfig.GAME_CATALOG_VERSION
            }
        }
        return corrected
    }

    suspend fun updateLoginDatesAndLevel(todayStr: String) {
        context.dataStore.edit { preferences ->
            val dates = (preferences[GAME_LOGIN_DATES] ?: emptySet()).toMutableSet()
            val maxDate = dates.maxOrNull()
            if (maxDate != null && todayStr < maxDate) {
                // System date retrocedes, do not add
            } else {
                dates.add(todayStr)
                preferences[GAME_LOGIN_DATES] = dates
            }
            preferences[GAME_LEVEL] = maxOf(1, dates.size)
        }
    }

    suspend fun claimMissionAtomic(
        missionId: String,
        rewardType: RewardType,
        rewardAmount: Int,
        rewardExp: Int,
        isDaily: Boolean,
        dateStr: String
    ): Boolean {
        var success = false
        context.dataStore.edit { preferences ->
            val key = getMissionClaimedKey(missionId, isDaily, dateStr)
            val alreadyClaimed = preferences[key] ?: false
            if (!alreadyClaimed) {
                preferences[key] = true
                val currentCoins = preferences[GAME_COINS] ?: 0
                val currentExp = preferences[GAME_EXP] ?: 0

                var earnedCoins = rewardAmount
                if (rewardType == RewardType.CHEST) {
                    earnedCoins = when (missionId) {
                        "m1" -> (GameConfig.CHEST_C1_COINS).random()
                        "m5" -> (GameConfig.CHEST_C2_COINS).random()
                        "m12" -> (GameConfig.CHEST_C3_COINS).random()
                        "m14", "m24", "m45", "m47", "m72", "m81" -> (GameConfig.CHEST_C5_COINS).random()
                        else -> 100
                    }
                    val chestExp = when (missionId) {
                        "m5" -> (GameConfig.CHEST_C2_EXP).random()
                        "m14", "m24", "m45", "m47", "m72", "m81" -> (GameConfig.CHEST_C5_EXP).random()
                        else -> 0
                    }
                    preferences[GAME_COINS] = currentCoins + earnedCoins
                    preferences[GAME_EXP] = currentExp + chestExp
                    val chests = preferences[CHESTS_OPENED_COUNT] ?: 0
                    preferences[CHESTS_OPENED_COUNT] = chests + 1
                } else {
                    preferences[GAME_COINS] = currentCoins + rewardAmount
                    preferences[GAME_EXP] = currentExp + rewardExp
                }

                val totalEarned = preferences[COINS_EARNED_TOTAL] ?: 0
                preferences[COINS_EARNED_TOTAL] = totalEarned + earnedCoins
                success = true
            }
        }
        return success
    }

    suspend fun buyPetAtomic(petId: String, costCoins: Int, costExp: Int): Boolean {
        var success = false
        context.dataStore.edit { preferences ->
            val coins = preferences[GAME_COINS] ?: 0
            val exp = preferences[GAME_EXP] ?: 0
            if (coins >= costCoins && exp >= costExp) {
                preferences[GAME_COINS] = coins - costCoins
                preferences[GAME_EXP] = exp - costExp
                val unlocked = (preferences[GAME_UNLOCKED_PETS] ?: setOf("larva", "gusano")) + petId
                preferences[GAME_UNLOCKED_PETS] = unlocked
                val spent = preferences[COINS_SPENT_PET_TOTAL] ?: 0
                preferences[COINS_SPENT_PET_TOTAL] = spent + costCoins
                success = true
            }
        }
        return success
    }

    suspend fun recordDailyAction(actionName: String, dateStr: String) {
        context.dataStore.edit { preferences ->
            preferences[getDailyActionKey(actionName, dateStr)] = true
        }
    }

    suspend fun incrementDiagnostics() {
        context.dataStore.edit { preferences ->
            val current = preferences[DIAGNOSTICS_COUNT] ?: 0
            preferences[DIAGNOSTICS_COUNT] = current + 1
        }
    }

    suspend fun incrementManualWaterings() {
        context.dataStore.edit { preferences ->
            val current = preferences[MANUAL_WATERINGS_COUNT] ?: 0
            preferences[MANUAL_WATERINGS_COUNT] = current + 1
        }
    }

    suspend fun incrementThresholdEdits() {
        context.dataStore.edit { preferences ->
            val current = preferences[THRESHOLD_EDITS_COUNT] ?: 0
            preferences[THRESHOLD_EDITS_COUNT] = current + 1
        }
    }

    suspend fun incrementPetSelectionChanges() {
        context.dataStore.edit { preferences ->
            val current = preferences[PET_SELECTION_CHANGES_COUNT] ?: 0
            preferences[PET_SELECTION_CHANGES_COUNT] = current + 1
        }
    }

    suspend fun setBleConnectedOnce() {
        context.dataStore.edit { preferences ->
            preferences[BLE_CONNECTED_ONCE] = true
        }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences -> preferences[USER_NAME] = name }
    }

    suspend fun saveSelectedPlantId(id: Int) {
        context.dataStore.edit { preferences -> preferences[SELECTED_PLANT_ID] = id }
    }

    suspend fun saveLastDisconnectTime(time: Long) {
        context.dataStore.edit { preferences -> preferences[LAST_DISCONNECT_TIME] = time }
    }

    suspend fun setDefaultsSeeded(seeded: Boolean) {
        context.dataStore.edit { preferences -> preferences[DEFAULTS_SEEDED] = seeded }
    }

    suspend fun saveSelectedPetId(id: String) {
        context.dataStore.edit { preferences -> preferences[SELECTED_PET_ID] = id }
    }

    suspend fun savePetName(petId: String, name: String) {
        context.dataStore.edit { preferences -> preferences[getPetNameKey(petId)] = name }
    }

    suspend fun resetPetName(petId: String) {
        context.dataStore.edit { preferences -> preferences.remove(getPetNameKey(petId)) }
    }

    suspend fun addRewards(coins: Int, exp: Int) {
        context.dataStore.edit { preferences ->
            val currentCoins = preferences[GAME_COINS] ?: 0
            val currentExp = preferences[GAME_EXP] ?: 0
            preferences[GAME_COINS] = currentCoins + coins
            preferences[GAME_EXP] = currentExp + exp
            val totalEarned = preferences[COINS_EARNED_TOTAL] ?: 0
            preferences[COINS_EARNED_TOTAL] = totalEarned + coins
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

    suspend fun setPlantPhotoSet() {
        context.dataStore.edit { preferences -> preferences[PLANT_PHOTO_SET] = true }
    }

    suspend fun setPetRenamed() {
        context.dataStore.edit { preferences -> preferences[PET_RENAMED] = true }
    }
}

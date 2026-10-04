package com.example.appjardin.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

        fun getPetNameKey(petId: String): Preferences.Key<String> {
            return stringPreferencesKey("pet_name_$petId")
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
}

package com.example.appjardin.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val SELECTED_PLANT_ID = intPreferencesKey("selected_plant_id")
    }

    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME] ?: "Usuario"
        }

    val selectedPlantIdFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_PLANT_ID] ?: -1
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
}

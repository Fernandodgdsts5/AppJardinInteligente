package com.example.appjardin.data

import android.content.Context
import com.example.appjardin.data.datastore.SettingsDataStore
import com.example.appjardin.data.local.AppDatabase
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.data.local.SessionEntity
import kotlinx.coroutines.flow.Flow

class Repository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val plantDao = database.plantDao()
    private val sessionDao = database.sessionDao()
    private val settingsDataStore = SettingsDataStore(context)

    val allPlants: Flow<List<PlantEntity>> = plantDao.getAllPlants()
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    
    val userNameFlow: Flow<String> = settingsDataStore.userNameFlow
    val selectedPlantIdFlow: Flow<Int> = settingsDataStore.selectedPlantIdFlow

    suspend fun insertDefaultPlantsIfEmpty() {
        if (plantDao.getCount() == 0) {
            val defaults = listOf(
                PlantEntity(name = "Tomate", inicioRiego = 50, finRiego = 65, recomendadaMax = 75, exceso = 85),
                PlantEntity(name = "Geranio", inicioRiego = 40, finRiego = 60, recomendadaMax = 70, exceso = 80),
                PlantEntity(name = "Rosa", inicioRiego = 45, finRiego = 60, recomendadaMax = 75, exceso = 85),
                PlantEntity(name = "Helecho", inicioRiego = 60, finRiego = 80, recomendadaMax = 90, exceso = 95)
            )
            plantDao.insertPlants(defaults)
        }
    }

    suspend fun getPlantById(id: Int): PlantEntity? {
        return plantDao.getPlantById(id)
    }

    suspend fun insertPlant(plant: PlantEntity) {
        plantDao.insertPlant(plant)
    }

    suspend fun saveUserName(name: String) {
        settingsDataStore.saveUserName(name)
    }

    suspend fun saveSelectedPlantId(id: Int) {
        settingsDataStore.saveSelectedPlantId(id)
    }
    
    suspend fun addOrUpdateSession(plantId: Int, plantName: String, humidity: Float) {
        val lastSession = sessionDao.getLastSession()
        val currentTime = System.currentTimeMillis()
        
        if (lastSession != null && (currentTime - lastSession.endTimeMs) < 3600000L) {
            // Check if continuous session exceeds 5 hours
            if ((currentTime - lastSession.startTimeMs) > 5 * 3600000L) {
                // Too long, create a new session
                val newSession = SessionEntity(
                    plantId = plantId,
                    plantName = plantName,
                    startTimeMs = currentTime,
                    endTimeMs = currentTime,
                    humidities = "$humidity"
                )
                sessionDao.insertSession(newSession)
            } else {
                // Update existing
                val newHumidities = if (lastSession.humidities.isEmpty()) "$humidity" else "${lastSession.humidities},$humidity"
                val updated = lastSession.copy(endTimeMs = currentTime, humidities = newHumidities)
                sessionDao.updateSession(updated)
            }
        } else {
            // Create new
            val newSession = SessionEntity(
                plantId = plantId,
                plantName = plantName,
                startTimeMs = currentTime,
                endTimeMs = currentTime,
                humidities = "$humidity"
            )
            sessionDao.insertSession(newSession)
        }
    }
}

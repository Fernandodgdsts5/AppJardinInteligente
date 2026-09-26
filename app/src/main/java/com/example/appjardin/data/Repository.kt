package com.example.appjardin.data

import android.content.Context
import android.util.Log
import com.example.appjardin.data.datastore.SettingsDataStore
import com.example.appjardin.data.local.AppDatabase
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.data.local.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class Repository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val plantDao = database.plantDao()
    private val sessionDao = database.sessionDao()
    private val settingsDataStore = SettingsDataStore(context)

    val allPlants: Flow<List<PlantEntity>> = plantDao.getAllPlants()
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    
    val userNameFlow: Flow<String> = settingsDataStore.userNameFlow
    val selectedPlantIdFlow: Flow<Int> = settingsDataStore.selectedPlantIdFlow

    suspend fun insertDefaultPlantsIfEmpty() = withContext(Dispatchers.IO) {
        try {
            if (plantDao.getCount() == 0) {
                val defaults = listOf(
                    PlantEntity(name = "Tomate", inicioRiego = 50, finRiego = 65, recomendadaMax = 75, exceso = 85),
                    PlantEntity(name = "Geranio", inicioRiego = 40, finRiego = 60, recomendadaMax = 70, exceso = 80),
                    PlantEntity(name = "Rosa", inicioRiego = 45, finRiego = 60, recomendadaMax = 75, exceso = 85),
                    PlantEntity(name = "Helecho", inicioRiego = 60, finRiego = 80, recomendadaMax = 90, exceso = 95)
                )
                plantDao.insertPlants(defaults)
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error inserting default plants", e)
        }
    }

    suspend fun getPlantById(id: Int): PlantEntity? = withContext(Dispatchers.IO) {
        try {
            plantDao.getPlantById(id)
        } catch (e: Exception) {
            Log.e("Repository", "Error getting plant by id", e)
            null
        }
    }

    suspend fun insertPlant(plant: PlantEntity) = withContext(Dispatchers.IO) {
        try {
            plantDao.insertPlant(plant)
        } catch (e: Exception) {
            Log.e("Repository", "Error inserting plant", e)
        }
    }

    suspend fun saveUserName(name: String) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.saveUserName(name)
        } catch (e: Exception) {
            Log.e("Repository", "Error saving username", e)
        }
    }

    suspend fun saveSelectedPlantId(id: Int) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.saveSelectedPlantId(id)
        } catch (e: Exception) {
            Log.e("Repository", "Error saving selected plant id", e)
        }
    }
    
    suspend fun addOrUpdateSession(plantId: Int, plantName: String, humidity: Float) = withContext(Dispatchers.IO) {
        try {
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
                    // To avoid endless string growth, we only keep the first few, or we update min/max,
                    // but since the schema is string, let's keep it simple: we don't append if it's too long,
                    // or we just update the endTimeMs without appending everything.
                    // Better approach: just store average or append if time difference > 5 minutes.
                    val lastRecordTime = lastSession.endTimeMs
                    val shouldAppendData = (currentTime - lastRecordTime) > 5 * 60000L // every 5 mins
                    
                    val newHumidities = if (shouldAppendData) {
                        "${lastSession.humidities},$humidity"
                    } else {
                        lastSession.humidities
                    }
                    
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
        } catch (e: Exception) {
            Log.e("Repository", "Error adding/updating session", e)
        }
    }
}

package com.example.appjardin.data

import android.content.Context
import android.util.Log
import com.example.appjardin.data.datastore.SettingsDataStore
import com.example.appjardin.data.local.AppDatabase
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.data.local.SessionEntity
import com.example.appjardin.util.PlantImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.first

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
                    PlantEntity(name = "Tomate", humedadMinima = 50, humedadBuena = 65, humedadExceso = 80, defaultKey = "tomate"),
                    PlantEntity(name = "Geranio", humedadMinima = 40, humedadBuena = 55, humedadExceso = 70, defaultKey = "geranio"),
                    PlantEntity(name = "Rosa", humedadMinima = 45, humedadBuena = 60, humedadExceso = 75, defaultKey = "rosa"),
                    PlantEntity(name = "Helecho", humedadMinima = 60, humedadBuena = 75, humedadExceso = 90, defaultKey = "helecho")
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

    suspend fun updatePlant(plant: PlantEntity, newImagePath: String?) = withContext(Dispatchers.IO) {
        try {
            val existing = plantDao.getPlantById(plant.id)
            if (existing != null) {
                if (!existing.imagePath.isNullOrBlank() && existing.imagePath != newImagePath) {
                    PlantImageStorage.deleteImageFile(existing.imagePath)
                }
                val updated = plant.copy(
                    name = plant.name,
                    humedadMinima = plant.humedadMinima,
                    humedadBuena = plant.humedadBuena,
                    humedadExceso = plant.humedadExceso,
                    imagePath = newImagePath
                )
                plantDao.updatePlant(updated)
                Log.d("Repository", "Updated plant ID: ${plant.id}")
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error updating plant", e)
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
    
    suspend fun recordSessionStart(plantId: Int, plantName: String, humidity: Float, minHumidity: Float, maxHumidity: Float) = withContext(Dispatchers.IO) {
        try {
            val lastDisconnectTime = settingsDataStore.lastDisconnectTimeFlow.first()
            val currentTime = System.currentTimeMillis()
            val timeSinceLastDisconnect = currentTime - lastDisconnectTime
            
            Log.d("Repository", "Evaluando sesión: tiempoActual=$currentTime, ultimaDesconexion=$lastDisconnectTime, diff=$timeSinceLastDisconnect ms")
            
            val lastSession = sessionDao.getLastSession()
            
            // Check if last disconnect was within 1 hour AND the last session belongs to the same plant
            if (lastSession != null && lastDisconnectTime > 0L && timeSinceLastDisconnect <= 3600000L && lastSession.plantId == plantId) {
                val newHumidities = "${lastSession.humidities},$humidity"
                val updated = lastSession.copy(
                    endTimeMs = currentTime, 
                    humidities = newHumidities,
                    humedadMasBaja = minOf(lastSession.humedadMasBaja, minHumidity),
                    humedadMasAlta = maxOf(lastSession.humedadMasAlta, maxHumidity)
                )
                sessionDao.updateSession(updated)
                Log.d("Repository", "-> Continuar sesión anterior: UPDATE sobre ID=${updated.id}")
            } else {
                val newSession = SessionEntity(
                    plantId = plantId,
                    plantName = plantName,
                    startTimeMs = currentTime,
                    endTimeMs = currentTime,
                    humidities = "$humidity",
                    humedadMasBaja = minHumidity,
                    humedadMasAlta = maxHumidity
                )
                sessionDao.insertSession(newSession)
                Log.d("Repository", "-> Nueva sesión: CREAR nuevo registro para $plantName (Pasó más de 1 hora o planta distinta)")
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error starting session", e)
        }
    }

    suspend fun recordSessionEnd(humidity: Float, minHumidity: Float, maxHumidity: Float) = withContext(Dispatchers.IO) {
        try {
            Log.d("Repository", "END RECORDING: humidity=$humidity")
            val currentTime = System.currentTimeMillis()
            settingsDataStore.saveLastDisconnectTime(currentTime) // Save exact disconnect time
            
            val lastSession = sessionDao.getLastSession()
            if (lastSession != null) {
                val newHumidities = "${lastSession.humidities},$humidity"
                val updated = lastSession.copy(
                    endTimeMs = currentTime, 
                    humidities = newHumidities,
                    humedadMasBaja = minOf(lastSession.humedadMasBaja, minHumidity),
                    humedadMasAlta = maxOf(lastSession.humedadMasAlta, maxHumidity)
                )
                sessionDao.updateSession(updated)
                Log.d("Repository", "Completed existing session: ${updated.id}, disconnect time saved.")
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error ending session", e)
        }
    }
}

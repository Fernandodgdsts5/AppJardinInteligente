package com.example.appjardin.data

import android.content.Context
import android.util.Log
import com.example.appjardin.data.datastore.SettingsDataStore
import com.example.appjardin.data.local.AppDatabase
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.data.local.SessionEntity
import com.example.appjardin.model.RewardType
import com.example.appjardin.util.PlantImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class Repository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val plantDao = database.plantDao()
    private val sessionDao = database.sessionDao()
    private val settingsDataStore = SettingsDataStore(context)

    val allPlants: Flow<List<PlantEntity>> = plantDao.getAllPlants()
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    
    val userNameFlow: Flow<String> = settingsDataStore.userNameFlow
    val selectedPlantIdFlow: Flow<Int> = settingsDataStore.selectedPlantIdFlow
    val selectedPetIdFlow: Flow<String> = settingsDataStore.selectedPetIdFlow

    val coinsFlow: Flow<Int> = settingsDataStore.coinsFlow
    val expFlow: Flow<Int> = settingsDataStore.expFlow
    val levelFlow: Flow<Int> = settingsDataStore.levelFlow
    val unlockedPetsFlow: Flow<Set<String>> = settingsDataStore.unlockedPetsFlow
    val diagnosticsCountFlow: Flow<Int> = settingsDataStore.diagnosticsCountFlow
    val manualWateringsCountFlow: Flow<Int> = settingsDataStore.manualWateringsCountFlow
    val thresholdEditsCountFlow: Flow<Int> = settingsDataStore.thresholdEditsCountFlow
    val petSelectionChangesCountFlow: Flow<Int> = settingsDataStore.petSelectionChangesCountFlow
    val coinsEarnedTotalFlow: Flow<Int> = settingsDataStore.coinsEarnedTotalFlow
    val coinsSpentPetTotalFlow: Flow<Int> = settingsDataStore.coinsSpentPetTotalFlow
    val plantPhotoSetFlow: Flow<Boolean> = settingsDataStore.plantPhotoSetFlow
    val chestsOpenedCountFlow: Flow<Int> = settingsDataStore.chestsOpenedCountFlow
    val bleConnectedOnceFlow: Flow<Boolean> = settingsDataStore.bleConnectedOnceFlow

    fun getPetNameFlow(petId: String, defaultName: String): Flow<String> {
        return settingsDataStore.getPetNameFlow(petId, defaultName)
    }

    fun getMissionClaimedFlow(missionId: String, isDaily: Boolean, dateStr: String): Flow<Boolean> {
        return settingsDataStore.getMissionClaimedFlow(missionId, isDaily, dateStr)
    }

    fun getDailyActionFlow(actionName: String, dateStr: String): Flow<Boolean> {
        return settingsDataStore.getDailyActionFlow(actionName, dateStr)
    }

    suspend fun initializeGameOnStartup(): Int = withContext(Dispatchers.IO) {
        var correctedCount = 0
        try {
            settingsDataStore.checkAndMigrateLegacyPetName()
            correctedCount = settingsDataStore.checkCatalogVersionAndSanitize()
            Log.d("Repository", "Catalog sanitization correction count: $correctedCount")

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            settingsDataStore.updateLoginDatesAndLevel(todayStr)

            val defaultsSeeded = settingsDataStore.defaultsSeededFlow.first()
            val count = plantDao.getCount()
            if (!defaultsSeeded) {
                if (count == 0) {
                    val defaults = listOf(
                        PlantEntity(name = "Tomate", humedadMinima = 50, humedadBuena = 65, humedadExceso = 80, defaultKey = "tomate", isUserCreated = false),
                        PlantEntity(name = "Geranio", humedadMinima = 40, humedadBuena = 55, humedadExceso = 70, defaultKey = "geranio", isUserCreated = false),
                        PlantEntity(name = "Rosa", humedadMinima = 45, humedadBuena = 60, humedadExceso = 75, defaultKey = "rosa", isUserCreated = false),
                        PlantEntity(name = "Helecho", humedadMinima = 60, humedadBuena = 75, humedadExceso = 90, defaultKey = "helecho", isUserCreated = false)
                    )
                    plantDao.insertPlants(defaults)
                }
                settingsDataStore.setDefaultsSeeded(true)
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error initializing game on startup", e)
        }
        correctedCount
    }

    suspend fun claimMissionAtomic(
        missionId: String,
        rewardType: RewardType,
        rewardAmount: Int,
        rewardExp: Int,
        isDaily: Boolean,
        dateStr: String
    ): Boolean = withContext(Dispatchers.IO) {
        settingsDataStore.claimMissionAtomic(missionId, rewardType, rewardAmount, rewardExp, isDaily, dateStr)
    }

    suspend fun buyPetAtomic(petId: String, costCoins: Int, costExp: Int): Boolean = withContext(Dispatchers.IO) {
        settingsDataStore.buyPetAtomic(petId, costCoins, costExp)
    }

    suspend fun recordDailyAction(actionName: String, dateStr: String) = withContext(Dispatchers.IO) {
        settingsDataStore.recordDailyAction(actionName, dateStr)
    }

    suspend fun incrementDiagnostics() = withContext(Dispatchers.IO) {
        settingsDataStore.incrementDiagnostics()
    }

    suspend fun incrementManualWaterings() = withContext(Dispatchers.IO) {
        settingsDataStore.incrementManualWaterings()
    }

    suspend fun incrementThresholdEdits() = withContext(Dispatchers.IO) {
        settingsDataStore.incrementThresholdEdits()
    }

    suspend fun incrementPetSelectionChanges() = withContext(Dispatchers.IO) {
        settingsDataStore.incrementPetSelectionChanges()
    }

    suspend fun setBleConnectedOnce() = withContext(Dispatchers.IO) {
        settingsDataStore.setBleConnectedOnce()
    }

    suspend fun deletePlant(plant: PlantEntity) = withContext(Dispatchers.IO) {
        try {
            plantDao.deletePlant(plant)
            if (!plant.imagePath.isNullOrBlank()) {
                PlantImageStorage.deleteImageFile(plant.imagePath)
            }
            val currentSelectedId = settingsDataStore.selectedPlantIdFlow.first()
            if (currentSelectedId == plant.id) {
                settingsDataStore.saveSelectedPlantId(-1)
            }
            Log.d("PlantDelete", "Successfully deleted plant: ${plant.name} (id=${plant.id})")
        } catch (e: Exception) {
            Log.e("PlantDelete", "Error deleting plant: ${plant.name}", e)
            throw e
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

    suspend fun saveSelectedPetId(id: String) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.saveSelectedPetId(id)
        } catch (e: Exception) {
            Log.e("Pet", "Error saving selected pet id", e)
        }
    }

    suspend fun savePetName(petId: String, name: String) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.savePetName(petId, name)
        } catch (e: Exception) {
            Log.e("Pet", "Error saving pet name", e)
        }
    }

    suspend fun resetPetName(petId: String) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.resetPetName(petId)
        } catch (e: Exception) {
            Log.e("Pet", "Error resetting pet name", e)
        }
    }

    suspend fun addRewards(coins: Int, exp: Int) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.addRewards(coins, exp)
        } catch (e: Exception) {
            Log.e("Repository", "Error adding rewards", e)
        }
    }

    suspend fun deductResources(coins: Int, exp: Int) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.deductResources(coins, exp)
        } catch (e: Exception) {
            Log.e("Repository", "Error deducting resources", e)
        }
    }

    suspend fun unlockPet(petId: String) = withContext(Dispatchers.IO) {
        try {
            settingsDataStore.unlockPet(petId)
        } catch (e: Exception) {
            Log.e("Repository", "Error unlocking pet", e)
        }
    }

    suspend fun recordSessionStart(plantId: Int, plantName: String, humidity: Float, minHumidity: Float, maxHumidity: Float) = withContext(Dispatchers.IO) {
        try {
            val lastDisconnectTime = settingsDataStore.lastDisconnectTimeFlow.first()
            val currentTime = System.currentTimeMillis()
            val timeSinceLastDisconnect = currentTime - lastDisconnectTime
            
            val lastSession = sessionDao.getLastSession()
            
            if (lastSession != null && lastDisconnectTime > 0L && timeSinceLastDisconnect <= 3600000L && lastSession.plantId == plantId) {
                val newHumidities = "${lastSession.humidities},$humidity"
                val updated = lastSession.copy(
                    endTimeMs = currentTime, 
                    humidities = newHumidities,
                    humedadMasBaja = minOf(lastSession.humedadMasBaja, minHumidity),
                    humedadMasAlta = maxOf(lastSession.humedadMasAlta, maxHumidity)
                )
                sessionDao.updateSession(updated)
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
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error starting session", e)
        }
    }

    suspend fun recordSessionEnd(humidity: Float, minHumidity: Float, maxHumidity: Float) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            settingsDataStore.saveLastDisconnectTime(currentTime)
            
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
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error ending session", e)
        }
    }

    suspend fun deleteSession(sessionId: Int) = withContext(Dispatchers.IO) {
        try {
            sessionDao.deleteSessionById(sessionId)
        } catch (e: Exception) {
            Log.e("Repository", "Error deleting session", e)
        }
    }

    suspend fun deleteSessions(sessionIds: List<Int>) = withContext(Dispatchers.IO) {
        try {
            sessionDao.deleteSessionsByIds(sessionIds)
        } catch (e: Exception) {
            Log.e("Repository", "Error deleting sessions", e)
        }
    }
}

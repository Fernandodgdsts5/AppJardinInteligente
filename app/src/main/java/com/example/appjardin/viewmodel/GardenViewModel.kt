package com.example.appjardin.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appjardin.BuildConfig
import com.example.appjardin.ble.BleManager
import com.example.appjardin.data.Repository
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.data.local.SessionEntity
import com.example.appjardin.model.AppTheme
import com.example.appjardin.model.Config
import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.Pet
import com.example.appjardin.model.RewardType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class ConnectionMode {
    CONNECTED,
    OFFLINE
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class GardenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    val bleManager = BleManager(application)

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("GardenViewModel", "Coroutine failed: ${exception.message}", exception)
    }

    val userName = repository.userNameFlow
        .catch { Log.e("GardenViewModel", "Error fetching username", it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, "Guardián de las Plantas")
        
    val allPlants = repository.allPlants
        .catch { Log.e("GardenViewModel", "Error fetching plants", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val allSessions = repository.allSessions
        .catch { Log.e("GardenViewModel", "Error fetching sessions", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allDiagnostics = repository.allDiagnostics
        .catch { Log.e("GardenViewModel", "Error fetching diagnostics", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedPetId = repository.selectedPetIdFlow
        .catch { Log.e("GardenViewModel", "Error fetching selected pet id", it); emit("gusano") }
        .stateIn(viewModelScope, SharingStarted.Lazily, "gusano")

    val petNames: StateFlow<Map<String, String>> = selectedPetId
        .flatMapLatest { _ ->
            combine(
                Pet.entries.map { pet ->
                    repository.getPetNameFlow(pet.id, pet.defaultName)
                        .map { name -> pet.id to name }
                }
            ) { pairs ->
                pairs.toMap()
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            Pet.entries.associate { it.id to it.defaultName }
        )

    val selectedPetName: StateFlow<String> = combine(selectedPetId, petNames) { id, names ->
        names[id] ?: Pet.fromId(id).defaultName
    }.stateIn(viewModelScope, SharingStarted.Lazily, "Coco")

    val selectedPet: StateFlow<Pet> = selectedPetId
        .map { Pet.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, Pet.GUSANO)

    val selectedTheme: StateFlow<AppTheme> = repository.selectedThemeIdFlow
        .map { AppTheme.fromId(it) }
        .catch { Log.e("GardenViewModel", "Error fetching selected theme", it); emit(AppTheme.SELVA) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SELVA)

    private val _selectedPlant = MutableStateFlow<PlantEntity?>(null)
    val selectedPlant: StateFlow<PlantEntity?> = _selectedPlant

    val coins: StateFlow<Int> = repository.coinsFlow
        .catch { Log.e("GardenViewModel", "Error fetching coins", it); emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val exp: StateFlow<Int> = repository.expFlow
        .catch { Log.e("GardenViewModel", "Error fetching exp", it); emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val level: StateFlow<Int> = repository.levelFlow
        .catch { Log.e("GardenViewModel", "Error fetching level", it); emit(1) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    val unlockedPets: StateFlow<Set<String>> = repository.unlockedPetsFlow
        .catch { Log.e("GardenViewModel", "Error fetching unlocked pets", it); emit(setOf("larva", "gusano")) }
        .stateIn(viewModelScope, SharingStarted.Lazily, setOf("larva", "gusano"))

    val diagnosticsCount: StateFlow<Int> = repository.totalDiagnosticsCountFlow
        .catch { Log.e("GardenViewModel", "Error fetching diagnostics count", it); emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val todayDiagnosticsCount: StateFlow<Int> = repository.allDiagnostics.map { list ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endMs = cal.timeInMillis

        list.count { it.modelVersion != "debug-fake" && it.createdAt >= startMs && it.createdAt < endMs }
    }.catch { Log.e("GardenViewModel", "Error fetching today diagnostics count", it); emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val manualWateringsCount: StateFlow<Int> = repository.manualWateringsCountFlow
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val thresholdEditsCount: StateFlow<Int> = repository.thresholdEditsCountFlow
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val petSelectionChangesCount: StateFlow<Int> = repository.petSelectionChangesCountFlow
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val coinsEarnedTotal: StateFlow<Int> = repository.coinsEarnedTotalFlow
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val coinsSpentPetTotal: StateFlow<Int> = repository.coinsSpentPetTotalFlow
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val plantPhotoSet: StateFlow<Boolean> = repository.plantPhotoSetFlow
        .catch { Log.e("GardenViewModel", "Error fetching plant photo flag", it); emit(false) }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val chestsOpenedCount: StateFlow<Int> = repository.chestsOpenedCountFlow
        .catch { Log.e("GardenViewModel", "Error fetching chests count", it); emit(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val bleConnectedOnce: StateFlow<Boolean> = repository.bleConnectedOnceFlow
        .catch { Log.e("GardenViewModel", "Error fetching ble connected flag", it); emit(false) }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _selectedSessionIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedSessionIds: StateFlow<Set<Int>> = _selectedSessionIds

    private val _isSessionSelectionMode = MutableStateFlow<Boolean>(false)
    val isSessionSelectionMode: StateFlow<Boolean> = _isSessionSelectionMode

    private val _selectedDiagnosisIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedDiagnosisIds: StateFlow<Set<Int>> = _selectedDiagnosisIds

    private val _isDiagnosisSelectionMode = MutableStateFlow<Boolean>(false)
    val isDiagnosisSelectionMode: StateFlow<Boolean> = _isDiagnosisSelectionMode

    val telemetry = bleManager.telemetry
    val connectionState = bleManager.connectionState
    val discoveredDevices = bleManager.discoveredDevices
    val isScanning = bleManager.isScanning

    private val _connectionMode = MutableStateFlow(ConnectionMode.CONNECTED)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode

    @OptIn(ExperimentalCoroutinesApi::class)
    val lastSessionForSelectedPlant: StateFlow<SessionEntity?> = _selectedPlant
        .flatMapLatest { plant ->
            if (plant != null) repository.getLastSessionForPlant(plant.id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun skipToOfflineMode() {
        bleManager.stopScan()
        bleManager.disconnect()
        _connectionMode.value = ConnectionMode.OFFLINE
        isRecordingSession = false
        waitingForFirstTelemetry = false
    }

    fun setConnectionMode(mode: ConnectionMode) {
        _connectionMode.value = mode
    }

    val activeSessionId: StateFlow<Int?> = combine(connectionState, allSessions) { state, sessions ->
        if (state == BluetoothProfile.STATE_CONNECTED && isRecordingSession) {
            sessions.firstOrNull()?.id
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _pumpOn = MutableStateFlow(false)
    val pumpOn: StateFlow<Boolean> = _pumpOn

    private var isRecordingSession = false
    private var waitingForFirstTelemetry = false
    private var lastKnownHumidity: Float = 0f
    
    private var appStartMinHumidity: Float = 100f
    private var sessionMaxHumidity: Float = 0f

    init {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val corrected = repository.initializeGameOnStartup()
            Log.d("GardenViewModel", "Startup sanitization correction count: $corrected")
            if (BuildConfig.DEBUG) {
                val deleted = repository.cleanupFakeDiagnostics()
                if (deleted > 0) {
                    Log.d("GardenViewModel", "Deleted $deleted debug-fake diagnostics on startup")
                }
            }
        }
        
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.selectedPlantIdFlow
                .catch { Log.e("GardenViewModel", "Error reading plant id", it); emit(-1) }
                .collect { id ->
                    if (id != -1) {
                        val plant = repository.getPlantById(id)
                        _selectedPlant.value = plant
                        plant?.let {
                            val config = Config(it.humedadMinima, it.humedadBuena, it.humedadExceso)
                            bleManager.writeConfig(config)
                        }
                    } else {
                        _selectedPlant.value = null
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            connectionState.collect { state ->
                val plant = _selectedPlant.value
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    repository.setBleConnectedOnce()
                    _pumpOn.value = false
                    plant?.let {
                        val config = Config(it.humedadMinima, it.humedadBuena, it.humedadExceso)
                        delay(1000L)
                        bleManager.writeConfig(config)
                        waitingForFirstTelemetry = true
                    }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    _pumpOn.value = false
                    if (isRecordingSession) {
                        repository.recordSessionEnd(lastKnownHumidity, appStartMinHumidity, sessionMaxHumidity)
                        isRecordingSession = false
                        waitingForFirstTelemetry = false
                    }
                }
            }
        }
        
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            telemetry.filterNotNull().collect { tele ->
                if (_connectionMode.value == ConnectionMode.OFFLINE) {
                    return@collect
                }
                val prevOptimistic = _pumpOn.value
                _pumpOn.value = tele.bomba
                lastKnownHumidity = tele.humedad
                
                val h = tele.humedad
                val plant = _selectedPlant.value
                if (h in 0f..100f) {
                    if (h < appStartMinHumidity) {
                        appStartMinHumidity = h
                    }
                    if (connectionState.value == BluetoothProfile.STATE_CONNECTED && plant != null) {
                        val moistureState = getMoistureState(h, plant)
                        if (moistureState == MoistureState.GOOD_MOISTURE) {
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            repository.recordDailyAction("happy_plant", todayStr)
                        }
                    }
                }
                sessionMaxHumidity = maxOf(sessionMaxHumidity, h)
                
                Log.d("JardinBLE", "bomba optimista=$prevOptimistic vs bomba real telemetría=${tele.bomba}, humedad=${tele.humedad}")
                
                if (plant != null && waitingForFirstTelemetry) {
                    repository.recordSessionStart(plant.id, plant.name, tele.humedad, appStartMinHumidity, sessionMaxHumidity)
                    waitingForFirstTelemetry = false
                    isRecordingSession = true
                }
            }
        }
    }

    fun getTodayStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun getDiagnosisById(id: Int) = repository.getDiagnosisById(id)
    
    fun getMissionClaimedFlow(missionId: String, isDaily: Boolean): Flow<Boolean> {
        return repository.getMissionClaimedFlow(missionId, isDaily, getTodayStr())
    }

    fun getDailyActionFlow(actionName: String): Flow<Boolean> {
        return repository.getDailyActionFlow(actionName, getTodayStr())
    }

    fun claimMission(
        missionId: String,
        rewardType: RewardType,
        rewardAmount: Int,
        rewardExp: Int,
        isDaily: Boolean,
        chosenPetId: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val success = repository.claimMissionAtomic(missionId, rewardType, rewardAmount, rewardExp, isDaily, getTodayStr(), chosenPetId)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun buyPetAtomic(petId: String, costCoins: Int, costExp: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val success = repository.buyPetAtomic(petId, costCoins, costExp)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun onDiagnosisConfirmed() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.incrementDiagnostics()
            repository.recordDailyAction("diagnosis", getTodayStr())
        }
    }

    fun onAdviceShown() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.recordDailyAction("advice_shown", getTodayStr())
        }
    }

    fun onHistoryEntered() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.recordDailyAction("history_opened", getTodayStr())
        }
    }

    fun isBluetoothEnabled(): Boolean = bleManager.isBluetoothEnabled()

    fun selectPlant(plantId: Int) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.saveSelectedPlantId(plantId)
        }
    }

    fun saveUserName(name: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.saveUserName(name)
        }
    }

    fun selectPet(id: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.saveSelectedPetId(id)
            repository.incrementPetSelectionChanges()
        }
    }

    fun savePetName(petId: String, name: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.savePetName(petId, name)
        }
    }

    fun resetPetName(petId: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.resetPetName(petId)
        }
    }

    suspend fun addPlant(plant: PlantEntity): Boolean {
        return repository.insertPlant(plant)
    }

    fun deletePlant(plant: PlantEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                repository.deletePlant(plant)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Error al eliminar planta")
                }
            }
        }
    }

    suspend fun updatePlant(plant: PlantEntity, newImagePath: String?): Boolean {
        val success = repository.updatePlant(plant, newImagePath)
        if (success) {
            repository.incrementThresholdEdits()
        }
        return success
    }

    fun addRewards(coins: Int, exp: Int) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.addRewards(coins, exp)
        }
    }

    fun deductResources(coins: Int, exp: Int) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.deductResources(coins, exp)
        }
    }

    fun unlockPet(petId: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.unlockPet(petId)
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.saveSelectedTheme(themeId)
        }
    }

    fun toggleSessionSelection(sessionId: Int) {
        val current = _selectedSessionIds.value.toMutableSet()
        if (current.contains(sessionId)) {
            current.remove(sessionId)
        } else {
            current.add(sessionId)
        }
        _selectedSessionIds.value = current
        _isSessionSelectionMode.value = current.isNotEmpty()
    }

    fun selectAllSessions(allIds: List<Int>) {
        _selectedSessionIds.value = allIds.toSet()
        _isSessionSelectionMode.value = allIds.isNotEmpty()
    }

    fun clearSessionSelection() {
        _selectedSessionIds.value = emptySet()
        _isSessionSelectionMode.value = false
    }

    fun toggleDiagnosisSelection(diagnosisId: Int) {
        val current = _selectedDiagnosisIds.value.toMutableSet()
        if (current.contains(diagnosisId)) {
            current.remove(diagnosisId)
        } else {
            current.add(diagnosisId)
        }
        _selectedDiagnosisIds.value = current
        _isDiagnosisSelectionMode.value = current.isNotEmpty()
    }

    fun selectAllDiagnostics(allIds: List<Int>) {
        _selectedDiagnosisIds.value = allIds.toSet()
        _isDiagnosisSelectionMode.value = allIds.isNotEmpty()
    }

    fun clearDiagnosisSelection() {
        _selectedDiagnosisIds.value = emptySet()
        _isDiagnosisSelectionMode.value = false
    }

    fun deleteSelectedDiagnostics() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val idsToDelete = _selectedDiagnosisIds.value.toList()
            if (idsToDelete.isNotEmpty()) {
                repository.deleteDiagnostics(idsToDelete)
            }
            withContext(Dispatchers.Main) {
                clearDiagnosisSelection()
            }
        }
    }

    fun deleteSelectedSessions(activeSessionId: Int?, onActiveExcluded: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val idsToDelete = _selectedSessionIds.value.toMutableSet()
            if (activeSessionId != null && idsToDelete.contains(activeSessionId)) {
                idsToDelete.remove(activeSessionId)
                withContext(Dispatchers.Main) {
                    onActiveExcluded()
                }
            }
            if (idsToDelete.isNotEmpty()) {
                repository.deleteSessions(idsToDelete.toList())
            }
            withContext(Dispatchers.Main) {
                clearSessionSelection()
            }
        }
    }

    fun deleteSingleSession(sessionId: Int, activeSessionId: Int?, onActiveExcluded: () -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            if (activeSessionId != null && sessionId == activeSessionId) {
                withContext(Dispatchers.Main) {
                    onActiveExcluded()
                }
                return@launch
            }
            repository.deleteSession(sessionId)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun startScan() {
        _connectionMode.value = ConnectionMode.CONNECTED
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        _connectionMode.value = ConnectionMode.CONNECTED
        bleManager.connectToDevice(device)
    }
    
    fun disconnectBle() {
        bleManager.disconnect()
    }
    
    fun togglePump(turnOn: Boolean) {
        if (_connectionMode.value == ConnectionMode.OFFLINE) return
        val isConnected = connectionState.value == BluetoothProfile.STATE_CONNECTED
        val plant = selectedPlant.value
        val isExcess = plant != null && lastKnownHumidity > plant.humedadExceso

        if (turnOn) {
            if (!isConnected || isExcess) {
                return
            }
            _pumpOn.value = true
            Log.d("JardinBLE", "Enviando comando: regar")
            viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
                repository.recordDailyAction("manual_watering", getTodayStr())
                repository.incrementManualWaterings()
            }
            bleManager.sendWateringAction("regar")
        } else {
            _pumpOn.value = false
            Log.d("JardinBLE", "Enviando comando: detener")
            bleManager.sendWateringAction("detener")
        }
    }

    fun getMoistureState(humidity: Float, plant: PlantEntity?): MoistureState {
        if (plant == null) return MoistureState.NO_PLANT
        return when {
            humidity < plant.humedadMinima -> MoistureState.LOW_MOISTURE
            humidity >= plant.humedadMinima && humidity < plant.humedadBuena -> MoistureState.MEDIUM_MOISTURE
            humidity >= plant.humedadBuena && humidity <= plant.humedadExceso -> MoistureState.GOOD_MOISTURE
            else -> MoistureState.EXCESS_MOISTURE
        }
    }

    fun onAppMinimized() {
        if (isRecordingSession) {
            viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
                repository.recordSessionEnd(lastKnownHumidity, appStartMinHumidity, sessionMaxHumidity)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isRecordingSession) {
            runBlocking(Dispatchers.IO) {
                repository.recordSessionEnd(lastKnownHumidity, appStartMinHumidity, sessionMaxHumidity)
            }
        }
        bleManager.closeGatt()
    }
}

package com.example.appjardin.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appjardin.ble.BleManager
import com.example.appjardin.data.Repository
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.model.Config
import com.example.appjardin.model.MoistureState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
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
    
    private val _selectedPlant = MutableStateFlow<PlantEntity?>(null)
    val selectedPlant: StateFlow<PlantEntity?> = _selectedPlant

    val telemetry = bleManager.telemetry
    val connectionState = bleManager.connectionState
    val discoveredDevices = bleManager.discoveredDevices
    val isScanning = bleManager.isScanning

    private val _pumpOn = MutableStateFlow(false)
    val pumpOn: StateFlow<Boolean> = _pumpOn

    private var isRecordingSession = false
    private var waitingForFirstTelemetry = false
    private var lastKnownHumidity: Float = 0f
    
    private var sessionMinHumidity: Float = 100f
    private var sessionMaxHumidity: Float = 0f

    init {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.insertDefaultPlantsIfEmpty()
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
                    _pumpOn.value = false // Reset optimistic state on fresh connection
                    plant?.let {
                        val config = Config(it.humedadMinima, it.humedadBuena, it.humedadExceso)
                        delay(1000L)
                        bleManager.writeConfig(config)
                        waitingForFirstTelemetry = true
                    }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    _pumpOn.value = false // Reset optimistic state on disconnect
                    if (isRecordingSession) {
                        repository.recordSessionEnd(lastKnownHumidity, sessionMinHumidity, sessionMaxHumidity)
                        isRecordingSession = false
                        waitingForFirstTelemetry = false
                    }
                }
            }
        }
        
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            telemetry.filterNotNull().collect { tele ->
                val prevOptimistic = _pumpOn.value
                _pumpOn.value = tele.bomba
                lastKnownHumidity = tele.humedad
                
                if (isRecordingSession) {
                    sessionMinHumidity = minOf(sessionMinHumidity, tele.humedad)
                    sessionMaxHumidity = maxOf(sessionMaxHumidity, tele.humedad)
                }
                
                // Add Log to verify optimistic vs real state
                Log.d("JardinBLE", "bomba optimista=$prevOptimistic vs bomba real telemetría=${tele.bomba}, humedad=${tele.humedad}")
                
                val plant = _selectedPlant.value
                if (plant != null && waitingForFirstTelemetry) {
                    sessionMinHumidity = tele.humedad
                    sessionMaxHumidity = tele.humedad
                    repository.recordSessionStart(plant.id, plant.name, tele.humedad, sessionMinHumidity, sessionMaxHumidity)
                    waitingForFirstTelemetry = false
                    isRecordingSession = true
                }
            }
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

    fun addPlant(plant: PlantEntity) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.insertPlant(plant)
        }
    }

    fun updatePlant(plant: PlantEntity, newImagePath: String?) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.updatePlant(plant, newImagePath)
        }
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        bleManager.connectToDevice(device)
    }
    
    fun disconnectBle() {
        bleManager.disconnect()
    }
    
    fun togglePump(turnOn: Boolean) {
        _pumpOn.value = turnOn // Actualización INSTANTÁNEA (optimista)
        val action = if (turnOn) "regar" else "detener"
        Log.d("JardinBLE", "Enviando comando: $action")
        bleManager.sendWateringAction(action)
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

    override fun onCleared() {
        super.onCleared()
        if (isRecordingSession) {
            runBlocking(Dispatchers.IO) {
                repository.recordSessionEnd(lastKnownHumidity, sessionMinHumidity, sessionMaxHumidity)
            }
        }
        bleManager.closeGatt()
    }
}

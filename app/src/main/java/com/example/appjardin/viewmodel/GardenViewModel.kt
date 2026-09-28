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
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    _selectedPlant.value?.let { plant ->
                        val config = Config(plant.humedadMinima, plant.humedadBuena, plant.humedadExceso)
                        delay(1000L)
                        bleManager.writeConfig(config)
                    }
                }
            }
        }
        
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            telemetry
                .filterNotNull()
                .debounce(5.seconds)
                .collect { tele ->
                    val plant = _selectedPlant.value
                    if (plant != null) {
                        repository.addOrUpdateSession(plant.id, plant.name, tele.humedad)
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            telemetry.filterNotNull().collect { tele ->
                _pumpOn.value = tele.bomba
                Log.d("JardinBLE", "Telemetría recibida -> bomba=${tele.bomba}")
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
        bleManager.closeGatt()
    }
}

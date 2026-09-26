package com.example.appjardin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appjardin.ble.BleManager
import com.example.appjardin.data.Repository
import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.model.Config
import com.example.appjardin.model.MoistureState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GardenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    val bleManager = BleManager(application)

    val userName = repository.userNameFlow.stateIn(viewModelScope, SharingStarted.Lazily, "Usuario")
    val allPlants = repository.allPlants.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allSessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _selectedPlant = MutableStateFlow<PlantEntity?>(null)
    val selectedPlant: StateFlow<PlantEntity?> = _selectedPlant

    val telemetry = bleManager.telemetry
    val connectionState = bleManager.connectionState

    init {
        viewModelScope.launch {
            repository.insertDefaultPlantsIfEmpty()
        }
        
        viewModelScope.launch {
            repository.selectedPlantIdFlow.collect { id ->
                if (id != -1) {
                    val plant = repository.getPlantById(id)
                    _selectedPlant.value = plant
                    plant?.let {
                        val config = Config(it.inicioRiego, it.finRiego, it.recomendadaMax, it.exceso)
                        bleManager.writeConfig(config)
                    }
                } else {
                    _selectedPlant.value = null
                }
            }
        }
        
        // Listen to telemetry to log sessions
        viewModelScope.launch {
            telemetry.collect { tele ->
                if (tele != null) {
                    val plant = _selectedPlant.value
                    if (plant != null) {
                        repository.addOrUpdateSession(plant.id, plant.name, tele.humedad)
                    }
                }
            }
        }
    }

    fun selectPlant(plantId: Int) {
        viewModelScope.launch {
            repository.saveSelectedPlantId(plantId)
        }
    }

    fun saveUserName(name: String) {
        viewModelScope.launch {
            repository.saveUserName(name)
        }
    }

    fun addPlant(plant: PlantEntity) {
        viewModelScope.launch {
            repository.insertPlant(plant)
        }
    }

    fun startScan() {
        bleManager.startScan()
    }
    
    fun disconnectBle() {
        bleManager.disconnect()
    }

    fun getMoistureState(humidity: Float, plant: PlantEntity?): MoistureState {
        if (plant == null) return MoistureState.NO_PLANT
        return when {
            humidity < plant.inicioRiego -> MoistureState.LOW_MOISTURE
            humidity >= plant.inicioRiego && humidity < plant.finRiego -> MoistureState.MEDIUM_MOISTURE
            humidity >= plant.finRiego && humidity <= plant.exceso -> MoistureState.GOOD_MOISTURE
            else -> MoistureState.EXCESS_MOISTURE
        }
    }
}

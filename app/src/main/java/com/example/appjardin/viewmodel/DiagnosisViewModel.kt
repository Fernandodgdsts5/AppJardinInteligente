package com.example.appjardin.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appjardin.BuildConfig
import com.example.appjardin.data.Repository
import com.example.appjardin.domain.ai.*
import com.example.appjardin.model.GameConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

sealed class DiagnosisUiState {
    object Camera : DiagnosisUiState()
    data class Captured(val bitmap: Bitmap) : DiagnosisUiState()
    object Analyzing : DiagnosisUiState()
    data class Saved(val id: Int) : DiagnosisUiState()
    object LowConfidence : DiagnosisUiState()
    object ModelUnavailable : DiagnosisUiState()
    data class Error(val message: String) : DiagnosisUiState()
}

class DiagnosisViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    private var classifier: DiagnosisClassifier? = null

    private val _uiState = MutableStateFlow<DiagnosisUiState>(DiagnosisUiState.Camera)
    val uiState: StateFlow<DiagnosisUiState> = _uiState

    var currentPlantId: Int? = null
    var currentPlantName: String = "Planta"
    var isDebugFake = false

    init {
        initClassifier()
    }

    private fun initClassifier() {
        try {
            // Check if model file exists physically in assets
            val list = getApplication<Application>().assets.list("models") ?: emptyArray()
            val fileName = DiagnosisModelConfig.MODEL_FILE_NAME.substringAfterLast("/")
            val fileExists = list.contains(fileName)

            if (!fileExists) {
                if (BuildConfig.DEBUG) {
                    Log.w("DiagnosisVM", "Model file not found ($fileName). Using FakeDiagnosisClassifier in DEBUG mode.")
                    classifier = FakeDiagnosisClassifier()
                    isDebugFake = true
                } else {
                    _uiState.value = DiagnosisUiState.ModelUnavailable
                }
                return
            }

            // File exists: try loading real LiteRt classifier
            classifier = LiteRtDiagnosisClassifier(getApplication())
            isDebugFake = false
            Log.d("DiagnosisVM", "Real classifier initialized successfully for $fileName")
        } catch (e: Exception) {
            Log.e("DiagnosisVM", "Failed to load real model: ${e.message}", e)
            _uiState.value = DiagnosisUiState.Error("Error al cargar modelo de IA: ${e.localizedMessage}")
        }
    }

    fun resetToCamera() {
        if (_uiState.value !is DiagnosisUiState.ModelUnavailable) {
            _uiState.value = DiagnosisUiState.Camera
        }
    }

    fun onPhotoCaptured(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = file.inputStream()
                val original = BitmapFactory.decodeStream(inputStream)
                val corrected = ImagePreprocessor.fixOrientation(original, file.inputStream())
                withContext(Dispatchers.Main) {
                    _uiState.value = DiagnosisUiState.Captured(corrected)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = DiagnosisUiState.Error("Error procesando foto: ${e.localizedMessage}")
                }
            }
        }
    }

    fun retryCamera() {
        resetToCamera()
    }

    fun analyzePhoto(bitmap: Bitmap) {
        _uiState.value = DiagnosisUiState.Analyzing
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val cl = classifier
                if (cl == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = DiagnosisUiState.Error("Modelo de IA no inicializado")
                    }
                    return@launch
                }

                val prediction = cl.classify(bitmap)

                if (prediction.confidence < GameConfig.CONFIDENCE_THRESHOLD) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = DiagnosisUiState.LowConfidence
                    }
                } else {
                    val modelVer = if (isDebugFake) "debug-fake" else DiagnosisModelConfig.MODEL_VERSION
                    val savedId = repository.saveDiagnosis(
                        plantId = currentPlantId,
                        plantName = currentPlantName,
                        bitmap = bitmap,
                        result = prediction.result.name,
                        confidence = prediction.confidence,
                        modelVersion = modelVer
                    )

                    if (savedId > 0 && modelVer != "debug-fake") {
                        repository.incrementDiagnostics()
                    }

                    withContext(Dispatchers.Main) {
                        _uiState.value = DiagnosisUiState.Saved(savedId.toInt())
                    }
                }
            } catch (e: Exception) {
                Log.e("DiagnosisVM", "Error during classification", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = DiagnosisUiState.Error("Error al analizar la imagen: ${e.localizedMessage}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        classifier?.close()
        classifier = null
    }
}

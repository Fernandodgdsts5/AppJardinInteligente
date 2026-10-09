package com.example.appjardin.ui.minijuego

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appjardin.data.Repository
import com.example.appjardin.data.minijuego.AnswerResponse
import com.example.appjardin.data.minijuego.MinijuegoRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MinijuegoViewModel(application: Application) : AndroidViewModel(application) {
    private val mainRepository = Repository(application)
    private val minijuegoRepository = MinijuegoRepository(application, mainRepository)
    private val gson = Gson()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("MinijuegoViewModel", "Unhandled exception in MinijuegoViewModel", exception)
    }

    private val _initStateJson = MutableStateFlow<String?>(null)
    val initStateJson: StateFlow<String?> = _initStateJson

    private val _answerResponseJson = MutableStateFlow<String?>(null)
    val answerResponseJson: StateFlow<String?> = _answerResponseJson

    fun loadInitState() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val (coins, xp) = minijuegoRepository.getInitialState()
                val dailyStatus = minijuegoRepository.getDailyStatus()

                val payload = mapOf(
                    "coins" to coins,
                    "xp" to xp,
                    "dailyLimitReached" to dailyStatus.dailyLimitReached
                )

                val json = gson.toJson(payload)
                withContext(Dispatchers.Main) {
                    _initStateJson.value = json
                }
            } catch (e: Exception) {
                Log.e("MinijuegoViewModel", "Error loading initial state", e)
            }
        }
    }

    fun submitAnswer(jsonInput: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                @Suppress("UNCHECKED_CAST")
                val inputMap = gson.fromJson(jsonInput, Map::class.java) as? Map<String, Any> ?: return@launch
                val sessionId = inputMap["sessionId"]?.toString() ?: ""
                val caseId = inputMap["caseId"]?.toString() ?: ""
                val round = (inputMap["round"] as? Double)?.toInt() ?: (inputMap["round"] as? Number)?.toInt() ?: 1
                val selectedAnswer = inputMap["selectedAnswer"]?.toString() ?: ""

                val response: AnswerResponse? = minijuegoRepository.processAnswer(
                    sessionId = sessionId,
                    caseId = caseId,
                    round = round,
                    selectedAnswer = selectedAnswer
                )

                if (response != null) {
                    val responseJson = gson.toJson(response)
                    withContext(Dispatchers.Main) {
                        _answerResponseJson.value = responseJson
                    }
                }
            } catch (e: Exception) {
                Log.e("MinijuegoViewModel", "Error processing answer from JS", e)
            }
        }
    }
}

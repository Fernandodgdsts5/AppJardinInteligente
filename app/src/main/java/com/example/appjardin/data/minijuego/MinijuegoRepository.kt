package com.example.appjardin.data.minijuego

import android.content.Context
import com.example.appjardin.data.Repository
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

data class MinijuegoCase(
    val id: String,
    val correctAnswer: String,
    val explanation: String
)

data class AnswerResponse(
    val isCorrect: Boolean,
    val correctAnswer: String,
    val explanation: String,
    val awardedCoins: Int,
    val awardedXp: Int,
    val totalCoins: Int,
    val totalXp: Int,
    val streak: Int,
    val dailyLimitReached: Boolean
)

class MinijuegoRepository(
    private val context: Context,
    private val mainRepository: Repository
) {
    companion object {
        val CASES = mapOf(
            "s1" to MinijuegoCase("s1", "REGAR", "Con 22% de humedad, este geranio necesita agua para recuperarse."),
            "s2" to MinijuegoCase("s2", "ESPERAR", "Con 82% de humedad, conviene esperar y dejar que el sustrato drene."),
            "s3" to MinijuegoCase("s3", "REGAR", "Con calor intenso y solo 34% de humedad, la tomatera agradecerá un riego."),
            "s4" to MinijuegoCase("s4", "ESPERAR", "Con 74% de humedad y clima fresco, otro riego sería innecesario."),
            "s5" to MinijuegoCase("s5", "REGAR", "Con 28% de humedad, un riego moderado ayudará a este helecho joven."),
            "s6" to MinijuegoCase("s6", "ESPERAR", "Con 48% de humedad y temperatura templada, la tomatera puede esperar.")
        )
    }

    private val sessionStreaks = ConcurrentHashMap<String, Int>()

    fun computeRewardForStreak(isCorrect: Boolean, newStreak: Int): Pair<Int, Int> {
        if (!isCorrect) return Pair(0, 0)
        return if (newStreak >= 3) Pair(30, 40) else Pair(20, 30)
    }

    suspend fun getInitialState(): Pair<Int, Int> {
        val coins = mainRepository.coinsFlow.first()
        val xp = mainRepository.expFlow.first()
        return Pair(coins, xp)
    }

    suspend fun getDailyStatus(): MinijuegoDataStore.DailyStatus {
        return MinijuegoDataStore.getDailyStatus(context)
    }

    suspend fun processAnswer(
        sessionId: String,
        caseId: String,
        round: Int,
        selectedAnswer: String
    ): AnswerResponse? {
        val targetCase = CASES[caseId] ?: return null
        if (selectedAnswer != "REGAR" && selectedAnswer != "ESPERAR") return null
        if (round !in 1..6 || sessionId.isBlank()) return null

        val isCorrect = selectedAnswer == targetCase.correctAnswer
        val currentStreak = sessionStreaks[sessionId] ?: 0
        val newStreak = if (isCorrect) currentStreak + 1 else 0
        sessionStreaks[sessionId] = newStreak

        val (rawCoins, rawXp) = computeRewardForStreak(isCorrect, newStreak)

        val (creditedCoins, creditedXp) = MinijuegoDataStore.processAnswerRewardAtomic(
            context = context,
            sessionId = sessionId,
            caseId = caseId,
            rawCoinReward = rawCoins,
            rawXpReward = rawXp
        ) { coinsToCredit, xpToCredit ->
            mainRepository.addRewards(coinsToCredit, xpToCredit)
        }

        val dailyStatus = getDailyStatus()
        val totalCoins = mainRepository.coinsFlow.first()
        val totalXp = mainRepository.expFlow.first()

        return AnswerResponse(
            isCorrect = isCorrect,
            correctAnswer = targetCase.correctAnswer,
            explanation = targetCase.explanation,
            awardedCoins = creditedCoins,
            awardedXp = creditedXp,
            totalCoins = totalCoins,
            totalXp = totalXp,
            streak = newStreak,
            dailyLimitReached = dailyStatus.dailyLimitReached
        )
    }
}

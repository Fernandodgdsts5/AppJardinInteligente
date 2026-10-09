package com.example.appjardin.data.minijuego

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

private val Context.minijuegoDataStore: DataStore<Preferences> by preferencesDataStore(name = "minijuego_prefs")

object MinijuegoDataStore {
    const val MAX_DAILY_COINS = 5000
    const val MAX_DAILY_XP = 5000

    private val DAILY_DATE = stringPreferencesKey("daily_date")
    private val DAILY_COINS_EARNED = intPreferencesKey("daily_coins_earned")
    private val DAILY_XP_EARNED = intPreferencesKey("daily_xp_earned")
    private val LAST_MAX_DATE = stringPreferencesKey("last_max_date")
    private val PROCESSED_ANSWERS = stringSetPreferencesKey("processed_answers")

    data class DailyStatus(
        val coinsEarnedToday: Int,
        val xpEarnedToday: Int,
        val coinsLimitReached: Boolean,
        val xpLimitReached: Boolean,
        val dailyLimitReached: Boolean
    )

    private fun getTodayStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun getDailyStatus(context: Context): DailyStatus {
        val prefs = context.minijuegoDataStore.data.first()
        val rawToday = getTodayStr()
        val lastMaxDate = prefs[LAST_MAX_DATE] ?: ""
        val effectiveDate = if (lastMaxDate.isNotEmpty() && rawToday < lastMaxDate) lastMaxDate else rawToday

        val savedDate = prefs[DAILY_DATE] ?: ""
        val coinsEarned = if (savedDate == effectiveDate) (prefs[DAILY_COINS_EARNED] ?: 0) else 0
        val xpEarned = if (savedDate == effectiveDate) (prefs[DAILY_XP_EARNED] ?: 0) else 0

        val coinsReached = coinsEarned >= MAX_DAILY_COINS
        val xpReached = xpEarned >= MAX_DAILY_XP

        return DailyStatus(
            coinsEarnedToday = coinsEarned,
            xpEarnedToday = xpEarned,
            coinsLimitReached = coinsReached,
            xpLimitReached = xpReached,
            dailyLimitReached = coinsReached && xpReached
        )
    }

    suspend fun processAnswerRewardAtomic(
        context: Context,
        sessionId: String,
        caseId: String,
        rawCoinReward: Int,
        rawXpReward: Int,
        creditToMainEconomy: suspend (coins: Int, xp: Int) -> Unit
    ): Pair<Int, Int> {
        var creditedCoins = 0
        var creditedXp = 0

        context.minijuegoDataStore.edit { prefs ->
            val processed = (prefs[PROCESSED_ANSWERS] ?: emptySet()).toMutableSet()
            val answerKey = "$sessionId:$caseId"

            if (processed.contains(answerKey)) {
                // Idempotent retry: already processed for this session and case
                return@edit
            }

            val rawToday = getTodayStr()
            val lastMaxDate = prefs[LAST_MAX_DATE] ?: ""
            val effectiveDate = if (lastMaxDate.isNotEmpty() && rawToday < lastMaxDate) lastMaxDate else rawToday

            if (effectiveDate > lastMaxDate) {
                prefs[LAST_MAX_DATE] = effectiveDate
            }

            val savedDate = prefs[DAILY_DATE] ?: ""
            val currentDailyCoins = if (savedDate == effectiveDate) (prefs[DAILY_COINS_EARNED] ?: 0) else 0
            val currentDailyXp = if (savedDate == effectiveDate) (prefs[DAILY_XP_EARNED] ?: 0) else 0

            val coinsRemaining = maxOf(0, MAX_DAILY_COINS - currentDailyCoins)
            val xpRemaining = maxOf(0, MAX_DAILY_XP - currentDailyXp)

            creditedCoins = minOf(rawCoinReward, coinsRemaining)
            creditedXp = minOf(rawXpReward, xpRemaining)

            prefs[DAILY_DATE] = effectiveDate
            prefs[DAILY_COINS_EARNED] = currentDailyCoins + creditedCoins
            prefs[DAILY_XP_EARNED] = currentDailyXp + creditedXp

            processed.add(answerKey)
            // Limit processed set size to 500 items so storage doesn't grow indefinitely
            if (processed.size > 500) {
                val toKeep = processed.toList().takeLast(300).toSet()
                prefs[PROCESSED_ANSWERS] = toKeep
            } else {
                prefs[PROCESSED_ANSWERS] = processed
            }
        }

        if (creditedCoins > 0 || creditedXp > 0) {
            creditToMainEconomy(creditedCoins, creditedXp)
        }

        return Pair(creditedCoins, creditedXp)
    }
}

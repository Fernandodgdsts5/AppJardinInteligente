package com.example.appjardin.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants")
    suspend fun getAllPlantsSync(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE id = :id LIMIT 1")
    suspend fun getPlantById(id: Int): PlantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity): Long

    @Update
    suspend fun updatePlant(plant: PlantEntity)

    @Delete
    suspend fun deletePlant(plant: PlantEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlants(plants: List<PlantEntity>)

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun getCount(): Int
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTimeMs DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTimeMs DESC LIMIT 1")
    suspend fun getLastSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    @Query("DELETE FROM sessions WHERE id IN (:sessionIds)")
    suspend fun deleteSessionsByIds(sessionIds: List<Int>)
}

@Dao
interface DiagnosisDao {
    @Query("SELECT * FROM diagnostics ORDER BY createdAt DESC")
    fun getAllDiagnostics(): Flow<List<DiagnosisEntity>>

    @Query("SELECT * FROM diagnostics WHERE id = :id LIMIT 1")
    suspend fun getDiagnosisById(id: Int): DiagnosisEntity?

    @Query("SELECT * FROM diagnostics WHERE id IN (:ids)")
    suspend fun getDiagnosticsByIds(ids: List<Int>): List<DiagnosisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosis(diagnosis: DiagnosisEntity): Long

    @Query("SELECT COUNT(*) FROM diagnostics WHERE modelVersion != 'debug-fake'")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM diagnostics WHERE modelVersion != 'debug-fake'")
    fun getTotalDiagnosticsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM diagnostics WHERE modelVersion != 'debug-fake' AND createdAt >= :startOfDayMs AND createdAt < :endOfDayMs")
    fun getTodayDiagnosticsCountFlow(startOfDayMs: Long, endOfDayMs: Long): Flow<Int>

    @Query("SELECT * FROM diagnostics WHERE modelVersion = 'debug-fake'")
    suspend fun getFakeDiagnostics(): List<DiagnosisEntity>

    @Query("DELETE FROM diagnostics WHERE modelVersion = 'debug-fake'")
    suspend fun deleteFakeDiagnostics(): Int

    @Query("DELETE FROM diagnostics WHERE id IN (:ids)")
    suspend fun deleteDiagnosticsByIds(ids: List<Int>)
}

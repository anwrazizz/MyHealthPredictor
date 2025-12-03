package com.example.myhealthpredictor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(predictionHistory: PredictionHistory)

    @Query("SELECT * FROM prediction_history ORDER BY date DESC")
    fun getAllPredictionHistory(): Flow<List<PredictionHistory>>
}

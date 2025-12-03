package com.example.myhealthpredictor.Prediction

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(predictionHistory: PredictionHistory)

    @Query("SELECT * FROM prediction_history ORDER BY date DESC")
    fun getAllPredictionHistory(): Flow<List<PredictionHistory>>
}
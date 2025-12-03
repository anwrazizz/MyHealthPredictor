package com.example.myhealthpredictor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weightLog: WeightLog)

    @Update
    suspend fun update(weightLog: WeightLog)

    @Delete
    suspend fun delete(weightLog: WeightLog)

    @Query("SELECT * FROM weight_logs ORDER BY date DESC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>
}

package com.example.myhealthpredictor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhysicalActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(physicalActivity: PhysicalActivity)

    @Update
    suspend fun update(physicalActivity: PhysicalActivity)

    @Delete
    suspend fun delete(physicalActivity: PhysicalActivity)

    @Query("SELECT * FROM physical_activities ORDER BY date DESC")
    fun getAllPhysicalActivities(): Flow<List<PhysicalActivity>>
}

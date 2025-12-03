package com.example.myhealthpredictor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val weight: Float,
    val date: Long
)

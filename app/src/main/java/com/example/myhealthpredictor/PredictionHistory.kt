package com.example.myhealthpredictor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_history")
data class PredictionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gender: String,
    val age: Int,
    val height: Float,
    val weight: Float,
    val familyHistoryWithOverweight: Boolean,
    val favc: Boolean,
    val fcvc: Int,
    val ncp: Int,
    val caec: String,
    val smoke: Boolean,
    val ch2o: Int,
    val scc: Boolean,
    val faf: Int,
    val tue: Int,
    val calc: String,
    val mtrans: String,
    val nobeyerere: String,
    val date: Long
)

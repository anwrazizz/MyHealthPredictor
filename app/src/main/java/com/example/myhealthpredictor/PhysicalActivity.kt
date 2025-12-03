package com.example.myhealthpredictor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "physical_activities")
data class PhysicalActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val duration: Int, // in minutes
    val date: Long
)

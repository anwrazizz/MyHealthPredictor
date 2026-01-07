package com.example.myhealthpredictor.WeightLog

data class WeightLog(
    var id: String = "",
    val userId: String = "",
    val weight: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor untuk Firestore
    constructor() : this("", "", 0.0, 0L, 0L)
}
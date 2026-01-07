package com.example.myhealthpredictor.ActivityLog

data class ActivityLog(
    var id: String = "",
    val userId: String = "",
    val activity: String = "",
    val duration: Int = 0,
    val date: Long = System.currentTimeMillis()
) {
    // Constructor tanpa parameter untuk Firestore
    constructor() : this("", "", "", 0, 0L)
}
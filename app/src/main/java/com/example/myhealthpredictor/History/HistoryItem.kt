package com.example.myhealthpredictor.History

data class HistoryItem(
    val type: HistoryType,
    val title: String,
    val value: String,
    val date: Long,
    val icon: String
)

enum class HistoryType {
    WEIGHT
}
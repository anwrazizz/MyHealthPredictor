package com.example.myhealthpredictor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WeightLogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeightLogDao
    val allWeightLogs: LiveData<List<WeightLog>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = database.weightLogDao()
        allWeightLogs = repository.getAllWeightLogs().asLiveData()
    }

    fun insert(weightLog: WeightLog) = viewModelScope.launch {
        repository.insert(weightLog)
    }

    fun delete(weightLog: WeightLog) = viewModelScope.launch {
        repository.delete(weightLog)
    }
}

package com.example.myhealthpredictor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PhysicalActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhysicalActivityDao
    val allActivities: LiveData<List<PhysicalActivity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = database.physicalActivityDao()
        allActivities = repository.getAllPhysicalActivities().asLiveData()
    }

    fun insert(activity: PhysicalActivity) = viewModelScope.launch {
        repository.insert(activity)
    }

    fun delete(activity: PhysicalActivity) = viewModelScope.launch {
        repository.delete(activity)
    }
}

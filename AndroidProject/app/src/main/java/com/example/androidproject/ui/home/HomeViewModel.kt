package com.example.androidproject.ui.home

import android.os.Build
import androidx.lifecycle.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.androidproject.repository.ScheduleRepository
import com.example.androidproject.model.ScheduleResponse
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeViewModel(private val repository: ScheduleRepository) :
    ViewModel() {
    private val _schedules = MutableLiveData<List<ScheduleResponse>>()
    val schedules: LiveData<List<ScheduleResponse>> get() = _schedules
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduleByDay(day: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getScheduleByDay(day)
                // Sort by start_time
                val sortedSchedules = response.sortedBy { schedule ->
                    try {
                        schedule.startTime?.let {
                            LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                _schedules.value = sortedSchedules
                _errorMessage.value = null
                Log.d("HomeViewModel", "Schedules loaded: ${sortedSchedules.size}")
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load schedules"
                _schedules.value = emptyList()
                Log.e("HomeViewModel", "Error loading schedules", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun clearError() {
        _errorMessage.value = null
    }
}
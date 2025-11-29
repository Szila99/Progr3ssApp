package com.example.androidproject.ui.editschedule

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidproject.model.ScheduleResponse
import com.example.androidproject.model.UpdateScheduleRequest
import com.example.androidproject.repository.ScheduleRepository
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class EditScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _schedule = MutableLiveData<ScheduleResponse>()
    val schedule: LiveData<ScheduleResponse> = _schedule

    private val _updateResult = MutableLiveData<Result<String>>()
    val updateResult: LiveData<Result<String>> = _updateResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadSchedule(scheduleId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getScheduleById(scheduleId)
                if (response.isSuccessful && response.body() != null) {
                    _schedule.value = response.body()
                    _errorMessage.value = null
                    Log.d("EditScheduleVM", "Schedule loaded: ${response.body()?.id}")
                } else {
                    _errorMessage.value = "Failed to load schedule: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load schedule"
                Log.e("EditScheduleVM", "Error loading schedule", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSchedule(
        scheduleId: Long,
        startTime: String?,
        endTime: String?,
        durationMinutes: Int?,
        status: String?,
        date: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = UpdateScheduleRequest(
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    status = status,
                    date = date,
                    notes = notes
                )
                val response = repository.updateSchedule(scheduleId, request)
                if (response.isSuccessful && response.body() != null) {
                    _updateResult.value = Result.success("Schedule updated successfully!")
                    Log.d("EditScheduleVM", "Schedule updated: ${response.body()?.id}")
                } else {
                    _updateResult.value = Result.failure(Exception("Failed to update: ${response.code()}"))
                }
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
                Log.e("EditScheduleVM", "Error updating schedule", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }
}

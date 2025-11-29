package com.example.androidproject.ui.scheduledetails

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidproject.model.CreateProgressRequest
import com.example.androidproject.model.ScheduleResponse
import com.example.androidproject.model.UpdateScheduleRequest
import com.example.androidproject.repository.ScheduleRepository
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ScheduleDetailsViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _schedule = MutableLiveData<ScheduleResponse>()
    val schedule: LiveData<ScheduleResponse> = _schedule

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _progressResult = MutableLiveData<Result<String>>()
    val progressResult: LiveData<Result<String>> = _progressResult

    private val _updateNotesResult = MutableLiveData<Result<String>>()
    val updateNotesResult: LiveData<Result<String>> = _updateNotesResult

    private val _deleteResult = MutableLiveData<Result<String>>()
    val deleteResult: LiveData<Result<String>> = _deleteResult

    fun loadSchedule(scheduleId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getScheduleById(scheduleId)
                if (response.isSuccessful && response.body() != null) {
                    _schedule.value = response.body()
                    _errorMessage.value = null
                    Log.d("ScheduleDetailsVM", "Schedule loaded: ${response.body()?.id}")
                } else {
                    _errorMessage.value = "Failed to load schedule: ${response.code()}"
                    Log.e("ScheduleDetailsVM", "Failed to load schedule: ${response.code()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load schedule"
                Log.e("ScheduleDetailsVM", "Error loading schedule", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addProgress(
        scheduleId: Long,
        date: String,
        loggedTime: Int?,
        notes: String?,
        isCompleted: Boolean?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreateProgressRequest(
                    scheduleId = scheduleId,
                    date = date,
                    loggedTime = loggedTime,
                    notes = notes,
                    isCompleted = isCompleted
                )
                val response = repository.createProgress(request)
                if (response.isSuccessful && response.body() != null) {
                    _progressResult.value = Result.success("Progress added successfully!")
                    Log.d("ScheduleDetailsVM", "Progress created")
                    // Reload schedule to get updated progress list
                    loadSchedule(scheduleId)
                } else {
                    _progressResult.value = Result.failure(Exception("Failed to add progress: ${response.code()}"))
                }
            } catch (e: Exception) {
                _progressResult.value = Result.failure(e)
                Log.e("ScheduleDetailsVM", "Error adding progress", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearProgressResult() {
        _progressResult.value = null
    }

    fun updateScheduleNotes(scheduleId: Long, notes: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = UpdateScheduleRequest(notes = notes)
                val response = repository.updateSchedule(id = scheduleId, request = request)
                if (response.isSuccessful && response.body() != null) {
                    _updateNotesResult.value = Result.success("Notes updated successfully!")
                    Log.d("ScheduleDetailsVM", "Notes updated")
                    // Reload schedule to get updated notes
                    loadSchedule(scheduleId)
                } else {
                    _updateNotesResult.value = Result.failure(Exception("Failed to update notes: ${response.code()}"))
                }
            } catch (e: Exception) {
                _updateNotesResult.value = Result.failure(e)
                Log.e("ScheduleDetailsVM", "Error updating notes", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUpdateNotesResult() {
        _updateNotesResult.value = null
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.deleteSchedule(scheduleId)
                if (response.isSuccessful) {
                    _deleteResult.value = Result.success("Schedule deleted successfully!")
                    Log.d("ScheduleDetailsVM", "Schedule deleted")
                } else {
                    _deleteResult.value = Result.failure(Exception("Failed to delete schedule: ${response.code()}"))
                }
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
                Log.e("ScheduleDetailsVM", "Error deleting schedule", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }
}

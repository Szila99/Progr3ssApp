package com.example.androidproject.ui.schedule

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidproject.model.*
import com.example.androidproject.repository.ScheduleRepository
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class CreateScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _habits = MutableLiveData<List<HabitResponse>>()
    val habits: LiveData<List<HabitResponse>> = _habits

    private val _categories = MutableLiveData<List<HabitCategoryResponseDto>>()
    val categories: LiveData<List<HabitCategoryResponseDto>> = _categories

    private val _createResult = MutableLiveData<Result<String>?>()
    val createResult: LiveData<Result<String>?> = _createResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private fun loadHabitsInternal(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("CreateScheduleVM", "loadHabitsInternal called for userId: $userId")
                val response = repository.getHabitsByUserId(userId)
                if (response.isSuccessful && response.body() != null) {
                    _habits.value = response.body()
                    Log.d("CreateScheduleVM", "Habits loaded successfully: ${response.body()?.size} habits")
                    response.body()?.forEach {
                        Log.d("CreateScheduleVM", "  - Habit: ${it.name} (id=${it.id})")
                    }
                } else {
                    Log.e("CreateScheduleVM", "Failed to load habits: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CreateScheduleVM", "Error loading habits", e)
            }
        }
    }

    fun loadHabits() {
        Log.d("CreateScheduleVM", "======= loadHabits called =======")
        // Get userId from SessionManager instead of waiting for profile
        val sessionManager = com.example.androidproject.utils.SessionManager(repository.getContext())
        val userId = sessionManager.fetchUserId()

        Log.d("CreateScheduleVM", "SessionManager fetched userId: '$userId'")
        Log.d("CreateScheduleVM", "userId isNullOrEmpty: ${userId.isNullOrEmpty()}")

        if (!userId.isNullOrEmpty()) {
            Log.d("CreateScheduleVM", "Loading habits for userId: $userId (from SessionManager)")
            loadHabitsInternal(userId)
        } else {
            Log.e("CreateScheduleVM", "Cannot load habits: userId not found in SessionManager")
            Log.e("CreateScheduleVM", "User needs to login again or userId was not saved properly")
            // Set empty list so UI can show appropriate message
            _habits.value = emptyList()
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val response = repository.getHabitCategories()
                if (response.isSuccessful && response.body() != null) {
                    _categories.value = response.body()
                    Log.d("CreateScheduleVM", "Loaded ${response.body()?.size} categories")
                } else {
                    _categories.value = emptyList()
                    Log.e("CreateScheduleVM", "Failed to load categories: ${response.code()}")
                }
            } catch (e: Exception) {
                _categories.value = emptyList()
                Log.e("CreateScheduleVM", "Error loading categories", e)
            }
        }
    }

    fun createHabit(name: String, description: String?, categoryId: Long, goal: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreateHabitRequest(
                    name = name,
                    description = description,
                    categoryId = categoryId,
                    goal = goal
                )
                Log.d("CreateScheduleVM", "Creating habit: name=$name, categoryId=$categoryId, goal=$goal")
                val response = repository.createHabit(request)
                Log.d("CreateScheduleVM", "Response code: ${response.code()}, success: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    Log.d("CreateScheduleVM", "Habit created: ${response.body()?.name}")
                    loadHabits() // Reload habits list
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("CreateScheduleVM", "Failed to create habit: ${response.code()} - Error body: $err")
                    _createResult.value = Result.failure(Exception("Failed to create habit: ${response.code()} ${err ?: ""}".trim()))
                }
            } catch (e: Exception) {
                _createResult.value = Result.failure(e)
                Log.e("CreateScheduleVM", "Exception creating habit", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCustomSchedule(
        habitId: Long,
        date: String,
        startTime: String,
        durationMinutes: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreateCustomScheduleRequest(
                    habitId = habitId,
                    date = date,
                    startTime = startTime,
                    durationMinutes = durationMinutes,
                    notes = notes
                )
                val response = repository.createCustomSchedule(request)
                if (response.isSuccessful) {
                    _createResult.value = Result.success("Schedule created successfully!")
                    Log.d("CreateScheduleVM", "Custom schedule created")
                } else {
                    _createResult.value = Result.failure(Exception("Failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                _createResult.value = Result.failure(e)
                Log.e("CreateScheduleVM", "Error creating custom schedule", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createRecurringSchedule(
        habitId: Long,
        startTime: String,
        repeatPattern: String,
        durationMinutes: Int?,
        repeatDays: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreateRecurringScheduleRequest(
                    habitId = habitId,
                    startTime = startTime,
                    repeatPattern = repeatPattern,
                    durationMinutes = durationMinutes,
                    repeatDays = repeatDays,
                    notes = notes
                )
                val response = repository.createRecurringSchedule(request)
                if (response.isSuccessful && response.body() != null) {
                    val count = response.body()?.size ?: 0
                    _createResult.value = Result.success("$count schedules created successfully!")
                    Log.d("CreateScheduleVM", "Recurring schedules created: $count")
                } else {
                    _createResult.value = Result.failure(Exception("Failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                _createResult.value = Result.failure(e)
                Log.e("CreateScheduleVM", "Error creating recurring schedule", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createWeekdayRecurringSchedule(
        habitId: Long,
        startTime: String,
        daysOfWeek: List<Int>,
        numberOfWeeks: Int,
        durationMinutes: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreateWeekdayRecurringRequest(
                    habitId = habitId,
                    startTime = startTime,
                    daysOfWeek = daysOfWeek,
                    numberOfWeeks = numberOfWeeks,
                    durationMinutes = durationMinutes,
                    notes = notes
                )
                val response = repository.createWeekdayRecurringSchedule(request)
                if (response.isSuccessful && response.body() != null) {
                    val count = response.body()?.size ?: 0
                    _createResult.value = Result.success("$count schedules created successfully!")
                    Log.d("CreateScheduleVM", "Weekday schedules created: $count")
                } else {
                    _createResult.value = Result.failure(Exception("Failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                _createResult.value = Result.failure(e)
                Log.e("CreateScheduleVM", "Error creating weekday schedule", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearResult() {
        _createResult.value = null
    }
}

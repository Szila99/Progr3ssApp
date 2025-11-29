package com.example.androidproject.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.androidproject.model.*
import com.example.androidproject.network.RetrofitClient
import retrofit2.Response

@RequiresApi(Build.VERSION_CODES.O)
class ScheduleRepository(private val context: Context) {
    private val api by lazy { RetrofitClient.getInstance(context) }
    
    fun getContext(): Context = context
    
    suspend fun getScheduleByDay(day: String): List<ScheduleResponse> {
        val response = api.getScheduleByDay(day)
        return response.body() ?: emptyList()
    }
    
    suspend fun getScheduleById(id: Long): Response<ScheduleResponse> {
        return api.getScheduleById(id)
    }
    
    suspend fun updateSchedule(id: Long, request: UpdateScheduleRequest): Response<ScheduleResponse> {
        return api.updateSchedule(id, request)
    }
    
    suspend fun deleteSchedule(id: Long): Response<Unit> {
        return api.deleteSchedule(id)
    }
    
    // Habit methods
    suspend fun getHabitsByUserId(userId: String): Response<List<HabitResponse>> {
        return api.getHabitsByUserId(userId)
    }
    
    suspend fun createHabit(request: CreateHabitRequest): Response<HabitResponse> {
        return api.createHabit(request)
    }
    
    suspend fun getHabitCategories(): Response<List<HabitCategoryResponseDto>> {
        return api.getHabitCategories()
    }
    
    // Schedule creation methods
    suspend fun createCustomSchedule(request: CreateCustomScheduleRequest): Response<ScheduleResponse> {
        return api.createCustomSchedule(request)
    }
    
    suspend fun createRecurringSchedule(request: CreateRecurringScheduleRequest): Response<List<ScheduleResponse>> {
        return api.createRecurringSchedule(request)
    }
    
    suspend fun createWeekdayRecurringSchedule(request: CreateWeekdayRecurringRequest): Response<List<ScheduleResponse>> {
        return api.createWeekdayRecurringSchedule(request)
    }
    
    // Progress methods
    suspend fun createProgress(request: CreateProgressRequest): Response<ProgressResponseDto> {
        return api.createProgress(request)
    }
}
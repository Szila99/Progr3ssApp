package com.example.androidproject.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.androidproject.model.CreateHabitRequest
import com.example.androidproject.model.HabitCategoryResponseDto
import com.example.androidproject.model.HabitResponse
import com.example.androidproject.model.ProfileResponseDto
import com.example.androidproject.model.UpdateProfileDto
import com.example.androidproject.network.RetrofitClient
import okhttp3.MultipartBody
import retrofit2.Response

@RequiresApi(Build.VERSION_CODES.O)
class ProfileRepository(private val context: Context) {
    private val api by lazy { RetrofitClient.getInstance(context) }
    
    fun getContext(): Context = context
    
    suspend fun getProfile(): Response<ProfileResponseDto> {
        return api.getProfile()
    }
    
    suspend fun updateProfile(request: UpdateProfileDto): Response<ProfileResponseDto> {
        return api.updateProfile(request)
    }
    
    suspend fun uploadProfileImage(image: MultipartBody.Part): Response<ProfileResponseDto> {
        return api.uploadProfileImage(image)
    }
    
    suspend fun getHabitsByUserId(userId: String): Response<List<HabitResponse>> {
        return api.getHabitsByUserId(userId)
    }
    
    suspend fun logout(): Response<Unit> {
        return api.logout()
    }
    
    suspend fun createHabit(request: CreateHabitRequest): Response<HabitResponse> {
        return api.createHabit(request)
    }
    
    suspend fun getHabitCategories(): Response<List<HabitCategoryResponseDto>> {
        return api.getHabitCategories()
    }
}

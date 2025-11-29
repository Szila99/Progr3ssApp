package com.example.androidproject.network

import com.example.androidproject.model.AuthRequest
import com.example.androidproject.model.AuthResponse
import com.example.androidproject.model.ScheduleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("/auth/local/signin")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("/auth/local/refresh")
    suspend fun refresh(): Response<com.example.androidproject.model.Tokens>

    @retrofit2.http.Multipart
    @POST("/auth/local/signup")
    suspend fun signup(
        @retrofit2.http.Part("username") username: okhttp3.RequestBody,
        @retrofit2.http.Part("email") email: okhttp3.RequestBody,
        @retrofit2.http.Part("password") password: okhttp3.RequestBody,
        @retrofit2.http.Part profileImage: okhttp3.MultipartBody.Part? = null
    ): Response<AuthResponse>

    @GET("/schedule/day")
    suspend fun getScheduleByDay(@Query("date") day: String): Response<List<ScheduleResponse>>

    @GET("/schedule/{id}")
    suspend fun getScheduleById(@retrofit2.http.Path("id") id: Long): Response<ScheduleResponse>

    @retrofit2.http.PATCH("/schedule/{id}")
    suspend fun updateSchedule(
        @retrofit2.http.Path("id") id: Long,
        @Body request: com.example.androidproject.model.UpdateScheduleRequest
    ): Response<ScheduleResponse>

    @retrofit2.http.DELETE("/schedule/{id}")
    suspend fun deleteSchedule(@retrofit2.http.Path("id") id: Long): Response<Unit>

    @POST("/auth/reset-password-via-email")
    suspend fun resetPassword(@Body request: com.example.androidproject.model.ResetPasswordRequest): Response<com.example.androidproject.model.ResetPasswordResponse>

    @POST("/auth/local/logout")
    suspend fun logout(): Response<Unit>

    @GET("/profile")
    suspend fun getProfile(): Response<com.example.androidproject.model.ProfileResponseDto>

    @retrofit2.http.PATCH("/profile")
    suspend fun updateProfile(@Body request: com.example.androidproject.model.UpdateProfileDto): Response<com.example.androidproject.model.ProfileResponseDto>

    @retrofit2.http.Multipart
    @POST("/profile/upload-profile-image")
    suspend fun uploadProfileImage(
        @retrofit2.http.Part profileImage: okhttp3.MultipartBody.Part
    ): Response<com.example.androidproject.model.ProfileResponseDto>

    // Habit endpoints
    @GET("/habit")
    suspend fun getHabits(): Response<List<com.example.androidproject.model.HabitResponse>>

    @POST("/habit")
    suspend fun createHabit(@Body request: com.example.androidproject.model.CreateHabitRequest): Response<com.example.androidproject.model.HabitResponse>

    @GET("/habit/categories")
    suspend fun getHabitCategories(): Response<List<com.example.androidproject.model.HabitCategoryResponseDto>>

    @GET("/habit/user/{userId}")
    suspend fun getHabitsByUserId(@retrofit2.http.Path("userId") userId: String): Response<List<com.example.androidproject.model.HabitResponse>>


    // Schedule creation endpoints
    @POST("/schedule/custom")
    suspend fun createCustomSchedule(@Body request: com.example.androidproject.model.CreateCustomScheduleRequest): Response<ScheduleResponse>

    @POST("/schedule/recurring")
    suspend fun createRecurringSchedule(@Body request: com.example.androidproject.model.CreateRecurringScheduleRequest): Response<List<ScheduleResponse>>

    @POST("/schedule/recurring/weekdays")
    suspend fun createWeekdayRecurringSchedule(@Body request: com.example.androidproject.model.CreateWeekdayRecurringRequest): Response<List<ScheduleResponse>>

    // Progress endpoints
    @POST("/progress")
    suspend fun createProgress(@Body request: com.example.androidproject.model.CreateProgressRequest): Response<com.example.androidproject.model.ProgressResponseDto>
}
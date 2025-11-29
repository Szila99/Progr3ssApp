package com.example.androidproject.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class ScheduleResponse(
    val id: Long,
    @SerializedName("start_time")
    val startTime: String? = null,
    @SerializedName("end_time")
    val endTime: String? = null,
    val status: String? = null, // Planned, Completed, Skipped
    val date: String? = null,
    @SerializedName("is_custom")
    val isCustom: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    val type: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val participants: List<ParticipantDto>? = emptyList(),
    val habit: HabitResponse? = null,
    val progress: List<ProgressResponseDto>? = emptyList(),
    @SerializedName("is_participant_only")
    val isParticipantOnly: Boolean = false,
)
data class HabitResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val category: HabitCategory? = null,
    val goal: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)
data class HabitCategory(
    val id: Long? = null,
    val name: String? = null,
    val iconUrl: String? = null
)

data class HabitCategoryResponseDto(
    val id: Long,
    val name: String,
    val iconUrl: String
)
data class ProgressResponseDto(
    val id: Long,
    val scheduleId: Long? = null,
    val date: String? = null,
    @SerializedName("logged_time")
    val loggedTime: Int? = null,
    val notes: String? = null,
    @SerializedName("is_completed")
    val isCompleted: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)
data class ParticipantDto(
    val id: Long,
    val name: String,
    val email: String,
    @SerializedName("profile_image")
    val profileImage: String? = null
)

// Request models for creating schedules
data class CreateCustomScheduleRequest(
    val habitId: Long,
    val date: String,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("is_custom")
    val isCustom: Boolean = true,
    @SerializedName("end_time")
    val endTime: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,
    val participantIds: List<Long>? = null,
    val notes: String? = null
)

data class CreateRecurringScheduleRequest(
    val habitId: Long,
    @SerializedName("start_time")
    val startTime: String,
    val repeatPattern: String = "none", // none, daily, weekdays, weekends
    @SerializedName("is_custom")
    val isCustom: Boolean = true,
    @SerializedName("end_time")
    val endTime: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,
    val repeatDays: Int = 30,
    val participantIds: List<Long>? = null,
    val notes: String? = null
)

data class CreateWeekdayRecurringRequest(
    val habitId: Long,
    @SerializedName("start_time")
    val startTime: String,
    val daysOfWeek: List<Int>, // 1=Monday ... 7=Sunday
    val numberOfWeeks: Int = 4,
    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,
    @SerializedName("end_time")
    val endTime: String? = null,
    val participantIds: List<Long>? = null,
    val notes: String? = null
)

// Habit létrehozási kérés – igazítva a backend kötelező mezőihez (name, categoryId, goal kötelező)
data class CreateHabitRequest(
    val name: String,
    val description: String? = null,
    val categoryId: Long,
    val goal: String
)

data class CreateProgressRequest(
    val scheduleId: Long,
    val date: String,
    @SerializedName("logged_time")
    val loggedTime: Int? = null,
    val notes: String? = null,
    @SerializedName("is_completed")
    val isCompleted: Boolean? = null
)

data class UpdateScheduleRequest(
    @SerializedName("start_time")
    val startTime: String? = null,
    @SerializedName("end_time")
    val endTime: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,
    val status: String? = null, // "Planned", "Completed", "Skipped"
    val date: String? = null,
    @SerializedName("is_custom")
    val isCustom: Boolean? = null,
    val participantIds: List<Long>? = null,
    val notes: String? = null
)

data class ProfileResponseDto(
    val id: String,
    val email: String,
    val username: String,
    val description: String? = null,
    // Backend mezőnevek camelCase-ben: profileImageUrl, profileImageBase64, coverImageUrl, fcmToken
    val profileImageUrl: String? = null,
    val profileImageBase64: String? = null,
    val coverImageUrl: String? = null,
    val fcmToken: String? = null,
    val preferences: Any? = null,
    // Ezek a backend-ben snake_case (created_at, updated_at)
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class UpdateProfileDto(
    val username: String? = null,
    val description: String? = null
)

package com.example.androidproject.ui.profile

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidproject.model.CreateHabitRequest
import com.example.androidproject.model.HabitCategoryResponseDto
import com.example.androidproject.model.HabitResponse
import com.example.androidproject.model.ProfileResponseDto
import com.example.androidproject.model.UpdateProfileDto
import com.example.androidproject.repository.ProfileRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

@RequiresApi(Build.VERSION_CODES.O)
class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _profile = MutableLiveData<ProfileResponseDto>()
    val profile: LiveData<ProfileResponseDto> = _profile

    private val _habits = MutableLiveData<List<HabitResponse>>()
    val habits: LiveData<List<HabitResponse>> = _habits

    private val _categories = MutableLiveData<List<HabitCategoryResponseDto>>()
    val categories: LiveData<List<HabitCategoryResponseDto>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _logoutResult = MutableLiveData<Result<String>?>()
    val logoutResult: LiveData<Result<String>?> = _logoutResult

    private val _habitCreationResult = MutableLiveData<Result<String>?>()
    val habitCreationResult: LiveData<Result<String>?> = _habitCreationResult

    private val _profileUpdateResult = MutableLiveData<Result<String>?>()
    val profileUpdateResult: LiveData<Result<String>?> = _profileUpdateResult

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    _profile.value = response.body()
                    _errorMessage.value = null
                    Log.d("ProfileVM", "Profile loaded: ${response.body()?.username}")
                    // Habits are loaded separately via refreshHabits() from onResume()
                } else {
                    _errorMessage.value = "Failed to load profile: ${response.code()}"
                    Log.e("ProfileVM", "Failed to load profile: ${response.code()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load profile"
                Log.e("ProfileVM", "Error loading profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadHabits(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProfileVM", "loadHabits called for userId: $userId")
                val response = repository.getHabitsByUserId(userId)
                if (response.isSuccessful && response.body() != null) {
                    _habits.value = response.body()
                    Log.d("ProfileVM", "Habits loaded successfully: ${response.body()?.size} habits")
                    response.body()?.forEach {
                        Log.d("ProfileVM", "  - Habit: ${it.name} (id=${it.id})")
                    }
                } else {
                    Log.e("ProfileVM", "Failed to load habits: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Error loading habits", e)
            }
        }
    }
    
    fun refreshHabits() {
        Log.d("ProfileVM", "refreshHabits called")
        // Get userId from SessionManager instead of waiting for profile
        val userId = com.example.androidproject.utils.SessionManager(
            repository.getContext()
        ).fetchUserId()
        
        if (!userId.isNullOrEmpty()) {
            Log.d("ProfileVM", "Refreshing habits for userId: $userId (from SessionManager)")
            loadHabits(userId)
        } else {
            Log.e("ProfileVM", "Cannot refresh habits: userId not found in SessionManager")
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val response = repository.getHabitCategories()
                if (response.isSuccessful && response.body() != null) {
                    _categories.value = response.body()
                    Log.d("ProfileVM", "Categories loaded: ${response.body()?.size}")
                } else {
                    Log.e("ProfileVM", "Failed to load categories: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Error loading categories", e)
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
                Log.d("ProfileVM", "Creating habit: name=$name, categoryId=$categoryId, goal=$goal")
                val response = repository.createHabit(request)
                Log.d("ProfileVM", "Response code: ${response.code()}, success: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    _habitCreationResult.value = Result.success("Habit created successfully!")
                    Log.d("ProfileVM", "Habit created: ${response.body()?.name}")
                    // Reload habits immediately
                    Log.d("ProfileVM", "Triggering habit reload after creation...")
                    refreshHabits()
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("ProfileVM", "Failed to create habit: ${response.code()} - Error body: $err")
                    _habitCreationResult.value = Result.failure(Exception("Failed to create habit: ${response.code()} ${err ?: ""}".trim()))
                }
            } catch (e: Exception) {
                _habitCreationResult.value = Result.failure(e)
                Log.e("ProfileVM", "Exception creating habit", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.logout()
                if (response.isSuccessful) {
                    _logoutResult.value = Result.success("Logged out successfully!")
                    Log.d("ProfileVM", "Logged out successfully")
                } else {
                    _logoutResult.value = Result.failure(Exception("Failed to logout: ${response.code()}"))
                }
            } catch (e: Exception) {
                _logoutResult.value = Result.failure(e)
                Log.e("ProfileVM", "Error logging out", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearLogoutResult() {
        _logoutResult.value = null
    }

    fun clearHabitCreationResult() {
        _habitCreationResult.value = null
    }

    fun updateProfile(username: String?, description: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = UpdateProfileDto(
                    username = username,
                    description = description
                )
                val response = repository.updateProfile(request)
                if (response.isSuccessful && response.body() != null) {
                    _profile.value = response.body()
                    _profileUpdateResult.value = Result.success("Profile updated successfully!")
                    Log.d("ProfileVM", "Profile updated")
                } else {
                    _profileUpdateResult.value = Result.failure(Exception("Failed to update profile: ${response.code()}"))
                }
            } catch (e: Exception) {
                _profileUpdateResult.value = Result.failure(e)
                Log.e("ProfileVM", "Error updating profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadProfileImage(image: MultipartBody.Part) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.uploadProfileImage(image)
                if (response.isSuccessful && response.body() != null) {
                    _profile.value = response.body()
                    _profileUpdateResult.value = Result.success("Profile image uploaded successfully!")
                    Log.d("ProfileVM", "Profile image uploaded")
                } else {
                    _profileUpdateResult.value = Result.failure(Exception("Failed to upload image: ${response.code()}"))
                }
            } catch (e: Exception) {
                _profileUpdateResult.value = Result.failure(e)
                Log.e("ProfileVM", "Error uploading image", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearProfileUpdateResult() {
        _profileUpdateResult.value = null
    }
}

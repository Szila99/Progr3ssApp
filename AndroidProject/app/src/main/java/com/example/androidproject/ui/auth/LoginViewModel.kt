package com.example.androidproject.ui.auth

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.androidproject.model.AuthResponse
import com.example.androidproject.repository.AuthRepository
import kotlinx.coroutines.launch
import retrofit2.Response

class AuthViewModel(application: Application) :
    AndroidViewModel(application) {
    private val repository = AuthRepository(application)
    private val _authResult = MutableLiveData<Result<AuthResponse>>()
    val authResult: LiveData<Result<AuthResponse>> = _authResult

    companion object {
        private const val TAG = "AuthViewModel"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun login(email: String, password: String) {
        Log.d(TAG, "===== Login attempt =====")
        Log.d(TAG, "Email: $email")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling login API...")
                val response = repository.login(email, password)
                Log.d(TAG, "Login API response - Code: ${response.code()}, Success: ${response.isSuccessful}")
                handleResponse(response, "Login")
            } catch (e: Exception) {
                Log.e(TAG, "Login exception", e)
                _authResult.postValue(Result.failure(e))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun register(username: String, email: String, password: String) {
        Log.d(TAG, "===== Register attempt =====")
        Log.d(TAG, "Username: $username, Email: $email")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling signup API...")
                val response = repository.signup(username, email, password)
                Log.d(TAG, "Signup API response - Code: ${response.code()}, Success: ${response.isSuccessful}")
                handleResponse(response, "Register")
            } catch (e: Exception) {
                Log.e(TAG, "Register exception", e)
                _authResult.postValue(Result.failure(e))
            }
        }
    }

    private fun handleResponse(response: Response<AuthResponse>, action: String) {
        Log.d(TAG, "===== Handling $action response =====")
        if (response.isSuccessful && response.body() != null) {
            response.body()?.let { body ->
                Log.d(TAG, "Response body received")
                Log.d(TAG, "User ID: ${body.user.id}")
                Log.d(TAG, "User email: ${body.user.email}")
                Log.d(TAG, "Access token: ${body.tokens.accessToken.take(20)}...")
                Log.d(TAG, "Refresh token: ${body.tokens.refreshToken.take(20)}...")

                Log.d(TAG, "Persisting tokens...")
                repository.persistTokens(body.tokens)
                Log.d(TAG, "Tokens persisted")

                Log.d(TAG, "Persisting user ID...")
                repository.persistUserId(body.user.id)
                Log.d(TAG, "User ID persisted")

                Log.d(TAG, "$action successful!")
                _authResult.postValue(Result.success(body))
            }
        } else {
            val errorMsg = "Auth failed: ${response.code()}"
            Log.e(TAG, errorMsg)
            Log.e(TAG, "Error body: ${response.errorBody()?.string()}")
            _authResult.postValue(Result.failure(Exception(errorMsg)))
        }
        Log.d(TAG, "===== $action response handled =====")
    }
}
package com.example.androidproject.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.androidproject.model.AuthRequest
import com.example.androidproject.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepository(context: Context) {
    @RequiresApi(Build.VERSION_CODES.O)
    private val api = RetrofitClient.getInstance(context)
    private val sessionManager = com.example.androidproject.utils.SessionManager(context.applicationContext)

    companion object {
        private const val TAG = "AuthRepository"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun login(email: String, password: String) =
        api.login(AuthRequest(email = email, password = password))

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun signup(username: String, email: String, password: String): retrofit2.Response<com.example.androidproject.model.AuthResponse> {
        val mediaType = "text/plain".toMediaTypeOrNull()
        val usernamePart = username.toRequestBody(mediaType)
        val emailPart = email.toRequestBody(mediaType)
        val passwordPart = password.toRequestBody(mediaType)
        return api.signup(usernamePart, emailPart, passwordPart, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun refresh(): retrofit2.Response<com.example.androidproject.model.Tokens> {
        Log.d(TAG, "===== Token Refresh Request =====")
        val refresh = sessionManager.fetchRefreshToken()

        if (refresh == null) {
            Log.w(TAG, "No refresh token available, returning null response")
            return retrofit2.Response.success(null)
        }

        Log.d(TAG, "Calling refresh API (token will be added via interceptor)")
        val response = api.refresh()
        Log.d(TAG, "Refresh API response - Code: ${response.code()}, Success: ${response.isSuccessful}")

        if (response.body() != null) {
            Log.d(TAG, "New tokens received from refresh")
        } else {
            Log.w(TAG, "No tokens in refresh response body")
        }

        return response
    }

    fun persistTokens(tokens: com.example.androidproject.model.Tokens) {
        Log.d(TAG, "===== Persisting Tokens via Repository =====")
        sessionManager.saveTokens(tokens.accessToken, tokens.refreshToken)
        Log.d(TAG, "Tokens persisted via SessionManager")
    }
    
    fun persistUserId(userId: String) {
        Log.d(TAG, "Persisting userId: $userId")
        sessionManager.saveUserId(userId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun resetPassword(email: String) =
        api.resetPassword(com.example.androidproject.model.ResetPasswordRequest(email = email))
}
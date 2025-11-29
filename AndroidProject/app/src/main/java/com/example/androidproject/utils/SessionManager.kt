package com.example.androidproject.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        Log.d("SessionManager", "===== Saving tokens =====")
        Log.d("SessionManager", "Access token: ${accessToken.take(20)}...")
        Log.d("SessionManager", "Refresh token: ${refreshToken.take(20)}...")

        val success = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .commit() // Use commit() instead of apply() for synchronous save

        Log.d("SessionManager", "Tokens saved successfully: $success")

        // Verify tokens were saved
        val savedAccess = prefs.getString(KEY_ACCESS_TOKEN, null)
        val savedRefresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        Log.d("SessionManager", "Verification - Access token: ${if (savedAccess != null) "${savedAccess.take(20)}..." else "null"}")
        Log.d("SessionManager", "Verification - Refresh token: ${if (savedRefresh != null) "${savedRefresh.take(20)}..." else "null"}")
    }
    
    fun saveUserId(userId: String) {
        Log.d("SessionManager", "Saving userId: $userId")
        val success = prefs.edit().putString(KEY_USER_ID, userId).commit()
        Log.d("SessionManager", "UserId saved successfully: $success")
    }
    
    fun fetchUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun saveAccessToken(token: String) {
        Log.d("SessionManager", "Saving access token: ${token.take(20)}...")
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun fetchAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        Log.d("SessionManager", "Fetching access token: ${if (token != null) "${token.take(20)}..." else "null"}")
        return token
    }

    fun fetchRefreshToken(): String? {
        val token = prefs.getString(KEY_REFRESH_TOKEN, null)
        Log.d("SessionManager", "Fetching refresh token: ${if (token != null) "${token.take(20)}..." else "null"}")
        return token
    }

    fun clearTokens() {
        Log.d("SessionManager", "===== Clearing tokens and userId =====")
        val success = prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .commit()
        Log.d("SessionManager", "Tokens cleared successfully: $success")
    }
}
package com.example.androidproject.network

import android.content.Context
import android.util.Log
import com.example.androidproject.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(context: Context) : Interceptor {
    private val sessionManager =
        SessionManager(context.applicationContext)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // For refresh endpoint, use refresh token; for all others, use access token
        val token = if (request.url.encodedPath.contains("/auth/local/refresh")) {
            val refreshToken = sessionManager.fetchRefreshToken()
            Log.d("AuthInterceptor", "Using refresh token for /auth/local/refresh: ${refreshToken?.take(20)}...")
            refreshToken
        } else {
            val accessToken = sessionManager.fetchAccessToken()
            Log.d("AuthInterceptor", "Using access token: ${accessToken?.take(20)}...")
            accessToken
        }

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.w("AuthInterceptor", "No token found in SessionManager for ${request.url.encodedPath}")
        }
        return chain.proceed(requestBuilder.build())
    }
}
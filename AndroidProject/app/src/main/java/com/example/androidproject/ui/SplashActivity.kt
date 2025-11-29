package com.example.androidproject.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidproject.databinding.ActivitySplashBinding
import com.example.androidproject.repository.AuthRepository
import com.example.androidproject.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Screen Activity
 *
 * Functionality:
 * - Displays a startup animation upon application launch
 * - Performs an automatic login check to authenticate the user
 * - If authentication is successful, the user is auto-logged in and redirected to the Home Screen
 * - If no valid authentication is found, or if auto-login fails, the user is redirected to the Login Screen
 *
 * Backend Interaction:
 * - Token Refresh/Validation: POST /auth/local/refresh
 * - Requires refresh-token stored in SharedPreferences
 * - Returns new access and refresh tokens on success
 */
@RequiresApi(Build.VERSION_CODES.O)
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var repository: AuthRepository
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "SplashActivity"
        private const val ANIMATION_DURATION = 700L
        private const val SPLASH_DELAY = 800L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AuthRepository(applicationContext)
        sessionManager = SessionManager(applicationContext)

        Log.d(TAG, "SplashActivity started")

        // Show progress indicator
        binding.progress.visibility = View.VISIBLE

        // Animate logo
        animateLogo()

        // Start auto-login process
        lifecycleScope.launch {
            delay(SPLASH_DELAY)
            tryAutoLogin()
        }
    }

    /**
     * Animates the app logo with fade-in and scale effects
     */
    private fun animateLogo() {
        binding.logoContainer.apply {
            scaleX = 0.5f
            scaleY = 0.5f
            alpha = 0f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION)
                .start()
        }
    }

    /**
     * Attempts to auto-login the user using stored refresh token
     *
     * Flow:
     * 1. Check if refresh token exists
     * 2. If exists, call /auth/local/refresh endpoint
     * 3. If successful, save new tokens and navigate to Home
     * 4. If failed or no token, navigate to Login screen
     */
    private suspend fun tryAutoLogin() {
        Log.d(TAG, "===== Starting auto-login process =====")
        val refreshToken = sessionManager.fetchRefreshToken()

        if (refreshToken.isNullOrEmpty()) {
            Log.d(TAG, "No refresh token found, navigating to login")
            Log.d(TAG, "===== Auto-login process FAILED (no token) =====")
            navigateToMain(home = false)
            return
        }

        Log.d(TAG, "Refresh token found: ${refreshToken.take(30)}...")
        Log.d(TAG, "Attempting auto-login with backend")

        try {
            val response = repository.refresh()

            Log.d(TAG, "Response received - Code: ${response.code()}, Success: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                response.body()?.let { tokens ->
                    Log.d(TAG, "Auto-login successful!")
                    Log.d(TAG, "New access token: ${tokens.accessToken.take(30)}...")
                    Log.d(TAG, "New refresh token: ${tokens.refreshToken.take(30)}...")
                    Log.d(TAG, "Saving new tokens...")
                    repository.persistTokens(tokens)
                    Log.d(TAG, "Tokens saved successfully")
                    Log.d(TAG, "===== Auto-login process SUCCESS =====")
                    navigateToMain(home = true)
                }
            } else {
                Log.w(TAG, "Auto-login failed: ${response.code()} - ${response.message()}")
                Log.w(TAG, "Response body: ${response.errorBody()?.string()}")
                // Clear invalid tokens
                Log.d(TAG, "Clearing invalid tokens")
                sessionManager.clearTokens()
                Log.d(TAG, "===== Auto-login process FAILED (invalid response) =====")
                navigateToMain(home = false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto-login", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            // Clear potentially corrupted tokens
            Log.d(TAG, "Clearing potentially corrupted tokens")
            sessionManager.clearTokens()
            Log.d(TAG, "===== Auto-login process FAILED (exception) =====")
            navigateToMain(home = false)
        }
    }

    /**
     * Navigates to MainActivity
     *
     * @param home If true, navigates to Home screen; if false, stays on Login screen
     */
    private fun navigateToMain(home: Boolean) {
        Log.d(TAG, "Navigating to MainActivity, home=$home")
        val intent = Intent(this, MainActivity::class.java)
        if (home) {
            intent.putExtra(MainActivity.EXTRA_NAVIGATE_TO_HOME, true)
        }
        startActivity(intent)
        finish()
    }
}

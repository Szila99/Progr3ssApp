package com.example.androidproject.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.androidproject.R
import com.example.androidproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: androidx.navigation.NavController

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_NAVIGATE_TO_HOME = "navigateToHome"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "===== MainActivity onCreate =====")

        // Check intent extras
        val navigateToHome = intent?.getBooleanExtra(EXTRA_NAVIGATE_TO_HOME, false) ?: false
        Log.d(TAG, "Intent extra '$EXTRA_NAVIGATE_TO_HOME': $navigateToHome")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Only include top-level destinations where back button should NOT show
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.profileFragment, R.id.loginFragment)
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
        
        // Enable title display
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // Hide bottom navigation and action bar on auth screens, customize titles
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment, R.id.resetPasswordFragment -> {
                    binding.bottomNav.visibility = android.view.View.GONE
                    supportActionBar?.hide()
                }
                R.id.homeFragment -> {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                    supportActionBar?.show()
                    supportActionBar?.title = getString(R.string.title_home)
                }
                R.id.profileFragment -> {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                    supportActionBar?.show()
                    supportActionBar?.title = getString(R.string.title_profile)
                }
                else -> {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                    supportActionBar?.show()
                    // Use default title from navigation graph or app name
                }
            }
        }

        // Handle auto-login from SplashActivity - must be called after navigation is set up
        handleAutoLogin(navController)
    }

    /**
     * Handles navigation after auto-login from SplashActivity
     * If auto-login succeeded, navigates to Home screen
     * Otherwise, stays on the default Login screen
     */
    private fun handleAutoLogin(navController: androidx.navigation.NavController) {
        val navigateToHome = intent?.getBooleanExtra(EXTRA_NAVIGATE_TO_HOME, false) ?: false

        if (navigateToHome) {
            Log.d(TAG, "Auto-login successful, navigating to Home and clearing back stack")
            // Use post to ensure navigation happens after the graph is fully initialized
            binding.root.post {
                try {
                    navController.navigate(R.id.homeFragment, null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Navigation error during auto-login", e)
                }
            }
        } else {
            Log.d(TAG, "No auto-login, staying on Login screen")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

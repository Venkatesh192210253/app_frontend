package com.simats.myfitnessbuddy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.simats.myfitnessbuddy.data.local.SettingsManager

import com.simats.myfitnessbuddy.R
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize SettingsManager
        SettingsManager.init(this)

        // Initialize Views for Animation
        val logo = findViewById<View>(R.id.logo_container)
        val title = findViewById<View>(R.id.tv_title)
        val subtitle = findViewById<View>(R.id.tv_subtitle)

        // Set initial states
        logo.alpha = 0f
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        
        title.alpha = 0f
        title.translationY = 30f
        
        subtitle.alpha = 0f
        subtitle.translationY = 30f

        // Start Animations
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        title.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        subtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Start sync and route in order
        lifecycleScope.launch {
            // Concurrently start the minimum delay
            val delayJob = launch { kotlinx.coroutines.delay(2000) }
            
            // Perform sync
            syncAuthStatus()
            
            // Ensure minimum splash time is met
            delayJob.join()
            
            // Route
            routeUser()
        }
    }

    private suspend fun syncAuthStatus() {
        val token = SettingsManager.authToken
        if (!token.isNullOrEmpty()) {
            try {
                val response = RetrofitClient.apiService.verifyToken()
                if (response.isSuccessful) {
                    val body = response.body()
                    val completed = body?.goals_completed ?: false
                    SettingsManager.goalsCompleted = completed
                    body?.user?.let { user ->
                        SettingsManager.userId = user.id
                        SettingsManager.userName = user.username
                        SettingsManager.fullName = user.full_name ?: user.profile?.full_name ?: ""
                        SettingsManager.userEmail = user.email
                    }
                } else if (response.code() == 401) {
                    // Token expired or invalid
                    SettingsManager.authToken = null
                    SettingsManager.refreshToken = null
                }
            } catch (e: Exception) {
                // Ignore network errors here, fallback to local state
            }
        }
    }

    private fun routeUser() {
        val prefs = SettingsManager
        val intent: Intent

        if (!prefs.authToken.isNullOrEmpty()) {
            // User is signed in - Always go to Dashboard as requested
            intent = Intent(this, DashboardActivity::class.java)
        } else {
            // User is not signed in
            intent = if (prefs.onboardingSeen) {
                Intent(this, LoginActivity::class.java)
            } else {
                Intent(this, OnboardingActivity::class.java)
            }
        }

        startActivity(intent)
        finish()
    }
}

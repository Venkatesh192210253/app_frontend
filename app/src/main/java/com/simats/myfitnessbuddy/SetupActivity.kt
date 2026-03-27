package com.simats.myfitnessbuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel

class SetupActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        setContent {
            val viewModel: OnboardingViewModel = viewModel()

            SetupFlow(
                onFinish = {
                    viewModel.saveProfile { success ->
                        if (success) {
                            val intent = Intent(this, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            )
        }
    }

    fun onSelectionCompleted(isCompleted: Boolean) {
        // This is a placeholder to fix a build error from an older, unused fragment.
    }
}

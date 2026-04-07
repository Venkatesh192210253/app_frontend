package com.simats.myfitnessbuddy

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager

import com.simats.myfitnessbuddy.data.local.AppDatabase
import com.simats.myfitnessbuddy.data.local.StepEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate

class StepViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val stepDao = database.stepDao()

    val steps = mutableStateOf(SettingsManager.totalStepsToday)
    val goal = mutableStateOf(SettingsManager.stepGoal)
    val calories = mutableStateOf(calculateCalories(SettingsManager.totalStepsToday))
    val distance = mutableStateOf(calculateDistance(SettingsManager.totalStepsToday))

    val userId = SettingsManager.userId

    // History Flow from Room
    val history: StateFlow<List<StepEntry>> = stepDao.getAllSteps(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyTotal = mutableStateOf(0)
    val averageSteps = mutableStateOf(0.0)

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        loadStats()
        // Sync history once on startup
        syncHistoryToBackend()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                if (userId.isNotEmpty()) {
                    refresh()
                }
                kotlinx.coroutines.delay(3000) // Poll every 3 seconds
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    fun refresh() {
        if (userId.isEmpty()) return
        
        val currentSteps = SettingsManager.totalStepsToday
        val currentGoal = SettingsManager.stepGoal
        
        steps.value = currentSteps
        goal.value = currentGoal
        calories.value = calculateCalories(currentSteps)
        distance.value = calculateDistance(currentSteps)
        
        saveToLocalDb(currentSteps)
        loadStats()
        // Push today's steps to backend
        syncSteps()
    }

    private fun saveToLocalDb(currentSteps: Int) {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()
                val existing = stepDao.getStepsForDate(today, userId)
                if (existing != null) {
                    stepDao.updateSteps(today, userId, currentSteps, System.currentTimeMillis())
                } else {
                    stepDao.insertOrUpdate(StepEntry(userId = userId, date = today, steps = currentSteps))
                }
            } catch (e: Exception) {
                Log.e("StepVM", "Local save failed: ${e.message}")
            }
        }
    }

    private fun loadStats() {
        if (userId.isEmpty()) return
        
        viewModelScope.launch {
            val sevenDaysAgo = LocalDate.now().minusDays(7).toString()
            weeklyTotal.value = stepDao.getWeeklyTotal(sevenDaysAgo, userId) ?: 0
            averageSteps.value = stepDao.getAverageSteps(userId) ?: 0.0
        }
    }

    private fun calculateCalories(steps: Int): Int = (steps * 0.04).toInt()
    private fun calculateDistance(steps: Int): Double = steps * 0.000762 // in km

    private fun syncSteps() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.updateDailyStats(mapOf("steps" to steps.value))
            } catch (e: Exception) {
                Log.e("StepVM", "Sync failed: ${e.message}")
            }
        }
    }

    private fun syncHistoryToBackend() {
        viewModelScope.launch {
            try {
                // Get history from Room
                val localHistory = stepDao.getAllSteps(userId).stateIn(viewModelScope).value
                localHistory.forEach { entry ->
                    // Only sync if it's within a reasonable range (last 30 days)
                    val entryDate = LocalDate.parse(entry.date)
                    if (entryDate.isAfter(LocalDate.now().minusDays(30))) {
                        RetrofitClient.apiService.updateDailyStats(
                            mapOf("date" to entry.date, "steps" to entry.steps)
                        )
                    }
                }
                Log.d("StepVM", "History sync complete")
            } catch (e: Exception) {
                Log.e("StepVM", "History sync failed: ${e.message}")
            }
        }
    }
}

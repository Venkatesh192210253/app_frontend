package com.simats.myfitnessbuddy

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserLevel(
    val currentLevel: Int = 5,
    val currentXp: Int = 1250,
    val nextLevelXp: Int = 2000,
    val totalWorkouts: Int = 24,
    val streakDays: Int = 7
)

class GamificationViewModel : ViewModel() {
    private val _userLevel = MutableStateFlow(UserLevel())
    val userLevel: StateFlow<UserLevel> = _userLevel.asStateFlow()

    fun addXp(amount: Int) {
        val current = _userLevel.value
        val newXp = current.currentXp + amount
        if (newXp >= current.nextLevelXp) {
            _userLevel.value = current.copy(
                currentLevel = current.currentLevel + 1,
                currentXp = newXp - current.nextLevelXp,
                nextLevelXp = (current.nextLevelXp * 1.2).toInt()
            )
        } else {
            _userLevel.value = current.copy(currentXp = newXp)
        }
    }

    fun getXpProgress(): Float {
        val level = _userLevel.value
        return level.currentXp.toFloat() / level.nextLevelXp.toFloat()
    }
}

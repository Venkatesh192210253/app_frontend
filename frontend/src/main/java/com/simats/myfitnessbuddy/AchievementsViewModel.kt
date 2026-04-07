package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import kotlinx.coroutines.launch
import android.util.Log

data class AchievementUi(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean,
    val unlockedAt: String?
)

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val achievements: List<AchievementUi> = emptyList(),
    val error: String? = null
)

class AchievementsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        fetchAchievements()
    }

    fun fetchAchievements() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitClient.apiService.getAchievements()
                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()
                    val mapped = data.map { ach ->
                        AchievementUi(
                            id = ach.id,
                            title = ach.title,
                            description = ach.description,
                            icon = mapIconName(ach.iconName),
                            color = Color(android.graphics.Color.parseColor(ach.colorHex)),
                            isUnlocked = ach.isUnlocked,
                            unlockedAt = ach.unlockedAt
                        )
                    }
                    _uiState.value = AchievementsUiState(
                        isLoading = false,
                        achievements = mapped
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AchievementsViewModel", "Error fetching achievements", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    private fun mapIconName(name: String): ImageVector {
        return when (name) {
            "WbSunny" -> Icons.Default.WbSunny
            "Whatshot" -> Icons.Default.Whatshot
            "Groups" -> Icons.Default.Groups
            "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
            "FitnessCenter" -> Icons.Default.FitnessCenter
            "LocalDrink" -> Icons.Default.LocalDrink
            "NightsStay" -> Icons.Default.NightsStay
            "EmojiEvents" -> Icons.Default.EmojiEvents
            else -> Icons.Default.EmojiEvents
        }
    }
}

package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.remote.ChallengeResponse
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupChallengesViewModel : ViewModel() {
    private val _challenges = MutableStateFlow<List<ChallengeResponse>>(emptyList())
    val challenges: StateFlow<List<ChallengeResponse>> = _challenges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadChallenges(groupId: String) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getGroupChallenges(groupId)
                if (response.isSuccessful) {
                    _challenges.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinChallenge(challengeId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getChallengeParticipants(challengeId)
                if (response.isSuccessful) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteChallenge(challengeId: String, groupId: String) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.deleteChallenge(challengeId)
                if (response.isSuccessful) {
                    loadChallenges(groupId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

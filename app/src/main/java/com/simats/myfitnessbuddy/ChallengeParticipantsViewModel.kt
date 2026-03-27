package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.remote.ChallengeParticipantResponse
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChallengeParticipantsViewModel : ViewModel() {
    private val _participants = MutableStateFlow<List<ChallengeParticipantResponse>>(emptyList())
    val participants: StateFlow<List<ChallengeParticipantResponse>> = _participants.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadParticipants(challengeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SettingsManager.authToken ?: ""
                val response = RetrofitClient.apiService.getChallengeParticipants(challengeId)
                if (response.isSuccessful) {
                    _participants.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

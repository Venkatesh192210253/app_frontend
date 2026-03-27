package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaqItem(
    val id: Int,
    val question: String,
    val answer: String,
    val isExpanded: Boolean = false
)

data class HelpSupportUiState(
    val faqs: List<FaqItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HelpSupportViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HelpSupportUiState())
    val uiState: StateFlow<HelpSupportUiState> = _uiState.asStateFlow()

    init {
        fetchFaqs()
    }

    fun fetchFaqs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getFaqs()
                if (response.isSuccessful) {
                    val faqs = response.body()?.map { 
                        FaqItem(it.id, it.question, it.answer)
                    } ?: emptyList()
                    _uiState.update { it.copy(faqs = faqs, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load FAQs") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun toggleFaq(faqId: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                faqs = currentState.faqs.map { 
                    if (it.id == faqId) it.copy(isExpanded = !it.isExpanded) else it 
                }
            )
        }
    }

    fun submitTicket(subject: String, message: String, category: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.createTicket(
                    com.simats.myfitnessbuddy.data.remote.TicketRequest(subject, message, category)
                )
                if (response.isSuccessful) {
                    onSuccess()
                }
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }
}

package com.example.ruvirtual.ui.card

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruvirtual.data.UserDataHolder
import com.example.ruvirtual.data.UserRepository
import com.example.ruvirtual.data.model.ProvisionResponse
import com.example.ruvirtual.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _provisionResponse = MutableStateFlow<ProvisionResponse?>(null)
    val provisionResponse = _provisionResponse.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _lastRefreshed = MutableStateFlow<Date?>(null)
    val lastRefreshed = _lastRefreshed.asStateFlow()

    init {
        // Load initial data from UserDataHolder if available, then refresh if needed
        UserDataHolder.provisionResponse?.let {
            _provisionResponse.value = it
            _lastRefreshed.value = Date() // Set current time on initial load from holder
        } ?: run {
            refreshCardData(true) // Attempt to load if UserDataHolder is empty
        }
    }

    fun refreshCardData(initialLoad: Boolean = false) {
        if (!initialLoad && _isRefreshing.value) return // Prevent multiple simultaneous refreshes

        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                // For refresh, we'll try to get card details, assuming it doesn't need login credentials again.
                // If your API requires user identification for getCardDetails, you might need a token or ID.
                // For now, let's assume it's a simple GET.
                val response = apiService.getCardDetails()
                if (response.isSuccessful) {
                    response.body()?.let {
                        UserDataHolder.provisionResponse = it // Update global holder
                        _provisionResponse.value = it // Update ViewModel's state
                        _errorMessage.value = null
                        _lastRefreshed.value = Date() // Update timestamp on successful refresh
                        Log.d("CardViewModel", "Card data refreshed successfully.")
                    } ?: run {
                        _errorMessage.value = "Resposta inesperada do servidor ao carregar detalhes do cartão."
                        Log.e("CardViewModel", "Empty body received for card details.")
                    }
                } else {
                    _errorMessage.value = "Erro ao carregar detalhes do cartão: ${response.code()} - ${response.message()}"
                    Log.e("CardViewModel", "API error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro de conexão ao carregar detalhes do cartão: ${e.localizedMessage ?: "Tente novamente."}"
                Log.e("CardViewModel", "Network error during card data refresh", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun errorMessageShown() {
        _errorMessage.value = null
    }
}

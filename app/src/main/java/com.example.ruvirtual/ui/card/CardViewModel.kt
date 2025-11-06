package com.example.ruvirtual.ui.card

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruvirtual.data.UserRepository
import com.example.ruvirtual.data.model.User
import com.example.ruvirtual.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        val cachedUser = userRepository.getUser()
        if (cachedUser == null) {
            refreshCardData(true)
        } else {
            _user.value = cachedUser
        }
    }

    fun refreshCardData(initialLoad: Boolean = false) {
        if (!initialLoad && _isRefreshing.value) return

        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getCardDetails()
                if (response.isSuccessful) {
                    response.body()?.let {
                        val user = User(
                            vCardId = it.vCardId?:String(),
                            nome = it.nome,
                            matricula = it.matricula,
                            creditos = it.creditos,
                            transacoes = it.transacoes
                        )
                        userRepository.saveUser(user)
                        _user.value = user
                        _errorMessage.value = null
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

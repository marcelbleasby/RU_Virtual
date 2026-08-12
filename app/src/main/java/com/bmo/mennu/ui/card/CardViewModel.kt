package com.bmo.mennu.ui.card

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.MealHistoryResult
import com.bmo.mennu.data.MealRepository
import com.bmo.mennu.data.UserRepository
import com.bmo.mennu.data.model.RefeicaoServida
import com.bmo.mennu.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CardUiState(
    val user: User? = null,
    val historico: List<RefeicaoServida> = emptyList(),
    val refeicoesEsteMes: Int? = null,
    // Gap documentado: fica true quando a conta não tem o Cargo/permissão
    // "refeicaoservida.view.list" liberado no mennu-api pra ver o próprio consumo.
    val consumoIndisponivel: Boolean = false
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CardUiState?>(null)
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        refreshCardData(initialLoad = true)
    }

    fun refreshCardData(initialLoad: Boolean = false) {
        if (!initialLoad && _isRefreshing.value) return

        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val user = userRepository.getUser() ?: authRepository.refreshUsuarioAtivo().getOrNull()
                if (user == null) {
                    _errorMessage.value = "Sessão expirada. Faça login novamente."
                    _uiState.value = null
                    return@launch
                }

                when (val meals = mealRepository.getRefeicoesServidas(user.id)) {
                    is MealHistoryResult.Success -> {
                        val refeicoesEsteMes = meals.refeicoes.count { isNoMesAtual(it.dataHora) }
                        userRepository.updateRefeicoesMes(refeicoesEsteMes)
                        _uiState.value = CardUiState(
                            user = user,
                            historico = meals.refeicoes,
                            refeicoesEsteMes = refeicoesEsteMes
                        )
                    }
                    is MealHistoryResult.Unavailable -> {
                        _uiState.value = CardUiState(user = user, consumoIndisponivel = true)
                    }
                    is MealHistoryResult.Failure -> {
                        _uiState.value = CardUiState(user = user)
                        _errorMessage.value = meals.message
                    }
                }
            } catch (e: Exception) {
                Log.e("CardViewModel", "Erro ao carregar dados do cartão", e)
                _errorMessage.value = e.localizedMessage ?: "Erro de conexão. Tente novamente."
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun errorMessageShown() {
        _errorMessage.value = null
    }

    private fun isNoMesAtual(iso: String): Boolean {
        return try {
            val mesAtual = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            iso.startsWith(mesAtual)
        } catch (e: Exception) {
            false
        }
    }
}

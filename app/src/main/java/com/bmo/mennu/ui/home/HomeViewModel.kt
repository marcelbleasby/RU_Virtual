package com.bmo.mennu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.PlanoRepository
import com.bmo.mennu.data.UserRepository
import com.bmo.mennu.ui.cardapio.DietTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PratoDoDia(val nome: String, val tags: List<DietTag>)

data class HomeUiState(
    val nomeExibicao: String = "",
    val creditos: Int = 0, // populado no init via PlanoRepository (mock — ver PlanoRepository.kt)
    // MOCK — não há endpoint de cardápio no mennu-api hoje (ApiService só tem auth + /refeicao/).
    val pratosHoje: List<PratoDoDia> = listOf(
        PratoDoDia("Arroz Branco", listOf(DietTag.VEGETARIANO, DietTag.VEGANO, DietTag.SEM_GLUTEN)),
        PratoDoDia("Feijão Preto", listOf(DietTag.VEGETARIANO, DietTag.VEGANO, DietTag.SEM_GLUTEN)),
        PratoDoDia("Batata Assada", listOf(DietTag.VEGETARIANO, DietTag.VEGANO, DietTag.SEM_GLUTEN))
    )
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val planoRepository: PlanoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userRepository.getUser() ?: authRepository.refreshUsuarioAtivo().getOrNull()
            val primeiroNome = user?.nome?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Usuário"
            val plano = planoRepository.getPlanoInfo()
            _uiState.value = _uiState.value.copy(nomeExibicao = primeiroNome, creditos = plano.creditosDisponiveis)
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch { authRepository.logout() }
    }
}

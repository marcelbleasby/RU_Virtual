package com.bmo.mennu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.CardapioRepository
import com.bmo.mennu.data.PlanoRepository
import com.bmo.mennu.data.UserRepository
import com.bmo.mennu.data.network.ConnectivityObserver
import com.bmo.mennu.ui.cardapio.DayMenu
import com.bmo.mennu.ui.cardapio.Dish
import com.bmo.mennu.data.model.TipoRefeicaoResponse
import com.bmo.mennu.util.mondayOfCurrentWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val nomeExibicao: String = "",
    val creditos: Int = 0, // populado no init via PlanoRepository (mock — ver PlanoRepository.kt)
    val refeicaoAtualNome: String? = null, // nome do TipoRefeicao resolvido pra agora, ou próximo de hoje; null = nenhuma sobra hoje
    val pratosHoje: List<Dish> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val planoRepository: PlanoRepository,
    private val cardapioRepository: CardapioRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var loadJob: Job? = null

    init {
        loadHomeData()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        loadHomeData()
    }

    fun onLogoutClicked() {
        viewModelScope.launch { authRepository.logout() }
    }

    private fun loadHomeData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val user = userRepository.getUser() ?: authRepository.refreshUsuarioAtivo().getOrNull()
                val primeiroNome = user?.nome?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Usuário"
                val plano = planoRepository.getPlanoInfo()
                _uiState.value = _uiState.value.copy(nomeExibicao = primeiroNome, creditos = plano.creditosDisponiveis)

                val weekStart = mondayOfCurrentWeek()

                // Offline-first: mostra o que já está em cache (Room) antes de tentar
                // rede — funciona mesmo sem conexão se já houve sync antes.
                applyWeekData(
                    cardapioRepository.observeWeekMenu(weekStart).first(),
                    cardapioRepository.observeTiposRefeicao().first()
                )

                // Atualiza em background; erro aqui é engolido de propósito (mesmo
                // comportamento de antes) — só recompõe se vier algo novo do servidor.
                cardapioRepository.refreshWeekMenu(weekStart)
                cardapioRepository.refreshTiposRefeicao()
                applyWeekData(
                    cardapioRepository.observeWeekMenu(weekStart).first(),
                    cardapioRepository.observeTiposRefeicao().first()
                )
            } catch (e: Exception) {
                // Mantém o que já estava no estado (nome/créditos aplicados acima, ou
                // pratosHoje/refeicaoAtualNome de um load anterior) — sem Toast, pra não
                // introduzir um segundo canal de erro só pro Home.
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun applyWeekData(weekDays: List<DayMenu>, tipos: List<TipoRefeicaoResponse>) {
        val hoje = weekDays.firstOrNull { isSameDay(it.date, Date()) }
        val refeicaoAtual = MealWindowResolver.resolveCurrentOrNext(nowMinutesOfDay(), tipos)
        val pratosHoje = refeicaoAtual
            ?.let { tipo -> hoje?.meals?.entries?.firstOrNull { it.key.id == tipo.id }?.value }
            .orEmpty()

        _uiState.value = _uiState.value.copy(refeicaoAtualNome = refeicaoAtual?.nome, pratosHoje = pratosHoje)
    }
}

private fun isSameDay(a: Date, b: Date): Boolean {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(a) == fmt.format(b)
}

private fun nowMinutesOfDay(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

package com.bmo.mennu.ui.cardapio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.CardapioRepository
import com.bmo.mennu.data.UserRepository
import com.bmo.mennu.data.network.ConnectivityObserver
import com.bmo.mennu.util.mondayOfCurrentWeek
import com.bmo.mennu.util.shiftWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class CardapioUiState(
    val isLoading: Boolean = true,
    val nomeExibicao: String = "",
    val weekDays: List<DayMenu> = emptyList(),
    val selectedDayIndex: Int = 0,
    val selectedMealType: MealType? = null // null = "Todos"
) {
    val selectedDay: DayMenu? get() = weekDays.getOrNull(selectedDayIndex)

    // Abas de refeição disponíveis na semana carregada (união de todos os dias),
    // ordenadas pelo campo `ordem` real do TipoRefeicao — não é um enum fixo.
    val availableMealTypes: List<MealType> get() = weekDays
        .flatMap { it.meals.keys }
        .distinctBy { it.id }
        .sortedBy { it.ordem }

    // Blocos de seção a renderizar pro dia selecionado, restritos ao filtro escolhido
    // e a refeições com pelo menos um prato cadastrado naquele dia.
    val visibleMealTypes: List<MealType> get() = availableMealTypes
        .filter { selectedMealType == null || it.id == selectedMealType.id }
        .filter { selectedDay?.meals?.get(it)?.isNotEmpty() == true }
}

@HiltViewModel
class CardapioViewModel @Inject constructor(
    private val cardapioRepository: CardapioRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _uiState = MutableStateFlow(CardapioUiState())
    val uiState = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    val lastSyncedAt = _lastSyncedAt.asStateFlow()

    private var weekStart: Date = mondayOfCurrentWeek()
    private var observeJob: Job? = null
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            val user = userRepository.getUser() ?: authRepository.refreshUsuarioAtivo().getOrNull()
            val primeiroNome = user?.nome?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Usuário"
            _uiState.value = _uiState.value.copy(nomeExibicao = primeiroNome)
        }
        observeCurrentWeek(resetDaySelection = true)
        triggerRefresh()
    }

    fun onDaySelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedDayIndex = index)
    }

    fun onMealTypeSelected(mealType: MealType?) {
        _uiState.value = _uiState.value.copy(selectedMealType = mealType)
    }

    fun onPreviousWeek() {
        weekStart = shiftWeek(weekStart, -7)
        observeCurrentWeek(resetDaySelection = true)
        triggerRefresh()
    }

    fun onNextWeek() {
        weekStart = shiftWeek(weekStart, 7)
        observeCurrentWeek(resetDaySelection = true)
        triggerRefresh()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        triggerRefresh()
    }

    fun onLogoutClicked() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun errorMessageShown() {
        _errorMessage.value = null
    }

    // Offline-first: assina o cache do Room pra essa semana (emite na hora, mesmo
    // sem rede) — reset de dia/filtro acontece só aqui, uma vez por troca de semana,
    // não a cada emissão da Flow.
    private fun observeCurrentWeek(resetDaySelection: Boolean) {
        observeJob?.cancel()
        if (resetDaySelection) {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedDayIndex = 0, selectedMealType = null)
        }
        observeJob = viewModelScope.launch {
            launch {
                cardapioRepository.observeWeekMenu(weekStart).collect { days ->
                    _uiState.value = _uiState.value.copy(isLoading = false, weekDays = days)
                }
            }
            launch {
                cardapioRepository.observeWeekMenuLastSyncedAt(weekStart).collect { _lastSyncedAt.value = it }
            }
        }
    }

    private fun triggerRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            cardapioRepository.refreshWeekMenu(weekStart)
                .onFailure { e -> _errorMessage.value = e.localizedMessage ?: "Erro de conexão. Tente novamente." }
            _isRefreshing.value = false
        }
    }
}

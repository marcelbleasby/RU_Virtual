package com.bmo.mennu.ui.cardapio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.CardapioRepository
import com.bmo.mennu.data.UserRepository
import com.bmo.mennu.util.mondayOfCurrentWeek
import com.bmo.mennu.util.shiftWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// resetSelection=true (navegação de semana): reseta o dia selecionado pra segunda.
// resetSelection=false (pull-to-refresh): preserva dia/filtro que o usuário já escolheu.
internal fun CardapioUiState.withFetchedWeek(days: List<DayMenu>, resetSelection: Boolean): CardapioUiState = copy(
    isLoading = false,
    weekDays = days,
    selectedDayIndex = if (resetSelection) 0 else selectedDayIndex
)

@HiltViewModel
class CardapioViewModel @Inject constructor(
    private val cardapioRepository: CardapioRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardapioUiState())
    val uiState = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var weekStart: Date = mondayOfCurrentWeek()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val user = userRepository.getUser() ?: authRepository.refreshUsuarioAtivo().getOrNull()
            val primeiroNome = user?.nome?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Usuário"
            _uiState.value = _uiState.value.copy(nomeExibicao = primeiroNome)
        }
        loadWeek(resetSelection = true)
    }

    fun onDaySelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedDayIndex = index)
    }

    fun onMealTypeSelected(mealType: MealType?) {
        _uiState.value = _uiState.value.copy(selectedMealType = mealType)
    }

    fun onPreviousWeek() {
        weekStart = shiftWeek(weekStart, -7)
        loadWeek(resetSelection = true)
    }

    fun onNextWeek() {
        weekStart = shiftWeek(weekStart, 7)
        loadWeek(resetSelection = true)
    }

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        loadWeek(resetSelection = false)
    }

    fun onLogoutClicked() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun errorMessageShown() {
        _errorMessage.value = null
    }

    private fun loadWeek(resetSelection: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (resetSelection) _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val days = cardapioRepository.getWeekMenu(weekStart)
                _uiState.value = _uiState.value.withFetchedWeek(days, resetSelection)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _errorMessage.value = e.localizedMessage ?: "Erro de conexão. Tente novamente."
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

package com.bmo.mennu.ui.cardapio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.CardapioRepository
import com.bmo.mennu.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardapioUiState())
    val uiState = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var weekStart: Date = mondayOfCurrentWeek()

    init {
        viewModelScope.launch {
            val user = userRepository.getUser() ?: authRepository.refreshUsuarioAtivo().getOrNull()
            val primeiroNome = user?.nome?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Usuário"
            _uiState.value = _uiState.value.copy(nomeExibicao = primeiroNome)
        }
        loadWeek()
    }

    fun onDaySelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedDayIndex = index)
    }

    fun onMealTypeSelected(mealType: MealType?) {
        _uiState.value = _uiState.value.copy(selectedMealType = mealType)
    }

    fun onPreviousWeek() {
        weekStart = shiftWeek(weekStart, -7)
        loadWeek()
    }

    fun onNextWeek() {
        weekStart = shiftWeek(weekStart, 7)
        loadWeek()
    }

    fun onLogoutClicked() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun errorMessageShown() {
        _errorMessage.value = null
    }

    private fun loadWeek() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val days = cardapioRepository.getWeekMenu(weekStart)
                _uiState.value = _uiState.value.copy(isLoading = false, weekDays = days, selectedDayIndex = 0)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _errorMessage.value = e.localizedMessage ?: "Erro de conexão. Tente novamente."
            }
        }
    }
}

private fun mondayOfCurrentWeek(): Date {
    val calendar = Calendar.getInstance()
    // DAY_OF_WEEK: domingo=1 ... sábado=7. Normaliza pra dias desde segunda (0..6),
    // independente do "primeiro dia da semana" do Locale.
    val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

private fun shiftWeek(date: Date, days: Int): Date {
    val calendar = Calendar.getInstance().apply { time = date }
    calendar.add(Calendar.DAY_OF_MONTH, days)
    return calendar.time
}

package com.bmo.mennu.ui.card

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.MealHistoryResult
import com.bmo.mennu.data.MealRepository
import com.bmo.mennu.data.NfcHardwareState
import com.bmo.mennu.data.NfcStatusRepository
import com.bmo.mennu.data.NfcTapEventBus
import com.bmo.mennu.data.PlanoInfo
import com.bmo.mennu.data.PlanoRepository
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
    val consumoIndisponivel: Boolean = false,
    val planoInfo: PlanoInfo? = null
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val mealRepository: MealRepository,
    private val planoRepository: PlanoRepository,
    private val nfcTapEventBus: NfcTapEventBus,
    private val nfcStatusRepository: NfcStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CardUiState?>(null)
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _tapDetected = MutableStateFlow<Long?>(null)
    val tapDetected = _tapDetected.asStateFlow()

    val nfcHardwareState = nfcStatusRepository.hardwareState
    val emulationAtiva = nfcStatusRepository.emulationAtiva

    init {
        refreshCardData(initialLoad = true)
        viewModelScope.launch {
            nfcTapEventBus.tapEvents.collect { timestamp -> _tapDetected.value = timestamp }
        }
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

                val planoInfo = planoRepository.getPlanoInfo()
                when (val meals = mealRepository.getRefeicoesServidas(user.id)) {
                    is MealHistoryResult.Success -> {
                        val refeicoesEsteMes = meals.refeicoes.count { isNoMesAtual(it.dataHora) }
                        userRepository.updateRefeicoesMes(refeicoesEsteMes)
                        _uiState.value = CardUiState(
                            user = user,
                            historico = meals.refeicoes,
                            refeicoesEsteMes = refeicoesEsteMes,
                            planoInfo = planoInfo
                        )
                    }
                    is MealHistoryResult.Unavailable -> {
                        _uiState.value = CardUiState(user = user, consumoIndisponivel = true, planoInfo = planoInfo)
                    }
                    is MealHistoryResult.SessionExpired -> {
                        // Sessão morta no meio do uso (ex.: conta desativada) — derruba a
                        // sessão local e volta pro mesmo estado de "sessão expirada" já
                        // usado quando não há usuário carregado, em vez de mostrar o
                        // cartão com "consumo indisponível".
                        authRepository.logout()
                        _errorMessage.value = "Sessão expirada. Faça login novamente."
                        _uiState.value = null
                    }
                    is MealHistoryResult.Failure -> {
                        _uiState.value = CardUiState(user = user, planoInfo = planoInfo)
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

    fun tapEventShown() {
        _tapDetected.value = null
    }

    fun onNfcHardwareStateChanged(state: NfcHardwareState) = nfcStatusRepository.updateHardwareState(state)

    fun onEmulationActiveChanged(ativa: Boolean) = nfcStatusRepository.setEmulationAtiva(ativa)

    private fun isNoMesAtual(iso: String): Boolean {
        return try {
            val mesAtual = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            iso.startsWith(mesAtual)
        } catch (e: Exception) {
            false
        }
    }
}

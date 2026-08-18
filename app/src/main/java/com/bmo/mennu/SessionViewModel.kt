package com.bmo.mennu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.SessionCheckResult
import com.bmo.mennu.data.TokenStore
import com.bmo.mennu.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    tokenStore: TokenStore,
    userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Lido uma vez no boot pra decidir o startDestination — leitura síncrona de
    // SharedPreferences, sem custo perceptível.
    val hasSavedSession: Boolean = tokenStore.getToken() != null && userRepository.getUser() != null

    private val _sessionInvalidated = MutableStateFlow(false)
    val sessionInvalidated = _sessionInvalidated.asStateFlow()

    init {
        // Offline-first: a navegação pro Home já aconteceu com a sessão salva
        // (ver AppNavigation). Essa validação roda em paralelo, sem bloquear a
        // entrada — só desloga se o servidor confirmar que a sessão morreu.
        if (hasSavedSession) {
            viewModelScope.launch {
                if (authRepository.validateSession() is SessionCheckResult.Invalid) {
                    authRepository.logout()
                    _sessionInvalidated.value = true
                }
            }
        }
    }
}

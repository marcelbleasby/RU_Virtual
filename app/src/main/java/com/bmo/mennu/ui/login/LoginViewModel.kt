package com.bmo.mennu.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _senha = MutableStateFlow("")
    val senha = _senha.asStateFlow()

    private val _loginResult = MutableStateFlow<User?>(null)
    val loginResult = _loginResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isEmailError = MutableStateFlow(false)
    val isEmailError = _isEmailError.asStateFlow()

    private val _isSenhaError = MutableStateFlow(false)
    val isSenhaError = _isSenhaError.asStateFlow()

    fun onEmailChange(newValue: String) {
        _email.value = newValue
        _isEmailError.value = false
    }

    fun onSenhaChange(newValue: String) {
        _senha.value = newValue
        _isSenhaError.value = false
    }

    fun onLoginClicked() {
        val currentEmail = email.value
        val currentSenha = senha.value

        if (currentEmail.isBlank()) {
            _isEmailError.value = true
            _errorMessage.value = "O e-mail não pode estar vazio."
            return
        }
        if (currentSenha.isBlank()) {
            _isSenhaError.value = true
            _errorMessage.value = "A senha não pode estar vazia."
            return
        }

        viewModelScope.launch {
            authRepository.login(currentEmail, currentSenha)
                .onSuccess {
                    _loginResult.value = it
                    _errorMessage.value = null
                }
                .onFailure {
                    _errorMessage.value = it.localizedMessage ?: "Erro de conexão. Tente novamente."
                }
        }
    }

    fun onNavigated() {
        _loginResult.value = null
    }

    fun onErrorMessageShown() {
        _errorMessage.value = null
    }
}

package com.example.ruvirtual.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruvirtual.data.UserRepository
import com.example.ruvirtual.data.model.LoginRequest
import com.example.ruvirtual.data.model.ProvisionResponse
import com.example.ruvirtual.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.ruvirtual.data.UserDataHolder // Importar UserDataHolder

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _matricula = MutableStateFlow("")
    val matricula = _matricula.asStateFlow()

    private val _senha = MutableStateFlow("")
    val senha = _senha.asStateFlow()

    private val _loginResult = MutableStateFlow<ProvisionResponse?>(null)
    val loginResult = _loginResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isMatriculaError = MutableStateFlow(false)
    val isMatriculaError = _isMatriculaError.asStateFlow()

    private val _isSenhaError = MutableStateFlow(false)
    val isSenhaError = _isSenhaError.asStateFlow()

    fun onMatriculaChange(newValue: String) {
        _matricula.value = newValue
        _isMatriculaError.value = false // Clear error when text changes
    }

    fun onSenhaChange(newValue: String) {
        _senha.value = newValue
        _isSenhaError.value = false // Clear error when text changes
    }

    fun onLoginClicked() {
        val currentMatricula = matricula.value
        val currentSenha = senha.value

        if (currentMatricula.isBlank()) {
            _isMatriculaError.value = true
            _errorMessage.value = "A matrícula não pode estar vazia."
            return
        }
        if (currentSenha.isBlank()) {
            _isSenhaError.value = true
            _errorMessage.value = "A senha não pode estar vazia."
            return
        }

        viewModelScope.launch {
            try {
                val response = apiService.provisionCard(LoginRequest(currentMatricula, currentSenha))
                if (response.isSuccessful) {
                    response.body()?.let {
                        userRepository.saveVirtualCardId(it.vCardId ?: "")
                        UserDataHolder.provisionResponse = it // Salvar a resposta no UserDataHolder
                        _loginResult.value = it
                        _errorMessage.value = null // Clear any previous error
                    } ?: run {
                        _errorMessage.value = "Resposta inesperada do servidor."
                    }
                } else {
                    _errorMessage.value = "Erro de login: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro de conexão: ${e.localizedMessage ?: "Tente novamente."}"
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

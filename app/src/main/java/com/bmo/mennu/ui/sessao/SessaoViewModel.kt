package com.bmo.mennu.ui.sessao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.AuthRepository
import com.bmo.mennu.data.model.Contexto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessaoViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    val sessao = authRepository.sessao
    private val _restaurando = MutableStateFlow(sessao.value.token != null)
    val restaurando = _restaurando.asStateFlow()
    private val _erro = MutableStateFlow<String?>(null)
    val erro = _erro.asStateFlow()
    private val _escolhendo = MutableStateFlow(false)
    val escolhendo = _escolhendo.asStateFlow()
    private val _carregando = MutableStateFlow(false)
    val carregando = _carregando.asStateFlow()

    init { if (sessao.value.token != null) atualizar() }

    fun atualizar() {
        if (_carregando.value) return
        _carregando.value = true
        _erro.value = null
        viewModelScope.launch {
            try { authRepository.refreshUsuarioAtivo().onSuccess { _restaurando.value = false }.onFailure { _erro.value = it.message } }
            finally { _carregando.value = false }
        }
    }

    fun trocar() { _escolhendo.value = true }

    fun selecionar(contexto: Contexto) {
        if (_carregando.value) return
        _carregando.value = true
        _erro.value = null
        viewModelScope.launch {
            try {
                authRepository.selecionarContexto(contexto)
                    .onSuccess { _escolhendo.value = false }
                    .onFailure { _erro.value = it.message }
            } finally { _carregando.value = false }
        }
    }

    fun sair() {
        viewModelScope.launch { authRepository.logout(); _escolhendo.value = false; _restaurando.value = false }
    }
}

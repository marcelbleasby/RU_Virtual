package com.bmo.mennu.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class NfcHardwareState { ATIVO, DESLIGADO, NAO_SUPORTADO }

// Fonte única de status do NFC (hardware + emulação ativa) — antes vivia só como
// remember local em CardScreen, invisível pro resto do app. Mesmo padrão de
// NfcTapEventBus: singleton + StateFlow privado com getter público.
@Singleton
class NfcStatusRepository @Inject constructor() {
    private val _hardwareState = MutableStateFlow(NfcHardwareState.ATIVO)
    val hardwareState: StateFlow<NfcHardwareState> = _hardwareState.asStateFlow()

    private val _emulationAtiva = MutableStateFlow(false)
    val emulationAtiva: StateFlow<Boolean> = _emulationAtiva.asStateFlow()

    fun updateHardwareState(state: NfcHardwareState) {
        _hardwareState.value = state
    }

    fun setEmulationAtiva(ativa: Boolean) {
        _emulationAtiva.value = ativa
    }
}

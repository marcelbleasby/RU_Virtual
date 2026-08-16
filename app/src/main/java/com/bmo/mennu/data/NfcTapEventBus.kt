package com.bmo.mennu.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// Bus em processo (sem IPC) — MennuHostApduService não declara android:process no
// manifest, então roda no processo principal do app, junto da Activity/ViewModel.
@Singleton
class NfcTapEventBus @Inject constructor() {
    private val _tapEvents = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 1)
    val tapEvents: SharedFlow<Long> = _tapEvents.asSharedFlow()

    // Chamado de processCommandApdu (callback síncrono, sem suspend) — tryEmit não bloqueia.
    fun emitTap() {
        _tapEvents.tryEmit(System.currentTimeMillis())
    }
}

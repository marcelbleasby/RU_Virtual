package com.bmo.mennu.data.model

data class User(
    val id: Int,
    val nome: String?,
    val email: String,
    val matricula: String?,
    val cargo: String?,
    val empresaId: Int?,
    // Vem de LoginResponseSchema.numero_cartao (UID da CredencialNFC ativa do
    // usuário) — populado em /api/auth/login e /api/auth/ativo. Fica null se o
    // usuário não tiver credencial NFC ativa cadastrada; a emulação NFC
    // (MennuHostApduService) já trata esse caso sem quebrar.
    val vCardId: String? = null
)

package com.bmo.mennu.data.model

interface PerfilSessao {
    val id: Int
    val nome: String?
    val email: String
    val matricula: String?
    val categoriaUsuario: String?
    val cargo: String?
    val ativo: Boolean
    val empresaId: Int?
    val criadoEm: String
    val atualizadoEm: String
    val numeroCartao: String?
    val tenantSalt: String?
    val contextos: List<Contexto>
}

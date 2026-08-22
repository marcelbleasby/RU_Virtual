package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

// Espelha LoginResponseSchema (mennu-api: user/api/schemas/auth/login_response.py).
data class LoginResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nome")
    val nome: String?,
    @SerializedName("email")
    val email: String,
    @SerializedName("documento")
    val documento: String?,
    @SerializedName("matricula")
    val matricula: String?,
    @SerializedName("tipo_usuario")
    val tipoUsuario: String,
    @SerializedName("categoria_usuario")
    val categoriaUsuario: String,
    @SerializedName("telefone")
    val telefone: String?,
    @SerializedName("cargo")
    val cargo: String?,
    @SerializedName("ativo")
    val ativo: Boolean,
    @SerializedName("empresa_id")
    val empresaId: Int?,
    @SerializedName("criado_em")
    val criadoEm: String,
    @SerializedName("atualizado_em")
    val atualizadoEm: String,
    @SerializedName("token_access")
    val tokenAccess: TokenAccess?,
    @SerializedName("feature_flags")
    val featureFlags: List<String> = emptyList(),
    @SerializedName("numero_cartao")
    val numeroCartao: String?,
    @SerializedName("tenant_salt")
    val tenantSalt: String?
)

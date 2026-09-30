package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

data class UsuarioAtivoResponse(
    @SerializedName("id") override val id: Int,
    @SerializedName("nome") override val nome: String?,
    @SerializedName("email") override val email: String,
    @SerializedName("matricula") override val matricula: String?,
    @SerializedName("categoria_usuario") override val categoriaUsuario: String?,
    @SerializedName("cargo") override val cargo: String?,
    @SerializedName("ativo") override val ativo: Boolean,
    @SerializedName("empresa_id") override val empresaId: Int?,
    @SerializedName("criado_em") override val criadoEm: String,
    @SerializedName("atualizado_em") override val atualizadoEm: String,
    @SerializedName("numero_cartao") override val numeroCartao: String?,
    @SerializedName("tenant_salt") override val tenantSalt: String?,
    @SerializedName("contextos") override val contextos: List<Contexto> = emptyList()
) : PerfilSessao

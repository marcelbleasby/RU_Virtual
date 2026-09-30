package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

data class Contexto(
    @SerializedName("empresa_id") val empresaId: Int,
    @SerializedName("empresa_nome") val empresaNome: String?,
    @SerializedName("unidade_id") val unidadeId: Int,
    @SerializedName("unidade_nome") val unidadeNome: String?,
    @SerializedName("slugs") val slugs: List<String> = emptyList()
) {
    fun mesmoPar(outro: Contexto?) = empresaId == outro?.empresaId && unidadeId == outro.unidadeId
}

data class ContextosResponse(val contextos: List<Contexto> = emptyList())

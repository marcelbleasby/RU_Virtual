package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

// Espelha CardapioSchema (mennu-api: core/api/schemas/cardapio/cardapio.py).
data class CardapioResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("data_refeicao")
    val dataRefeicao: String, // yyyy-MM-dd
    @SerializedName("tipo_refeicao")
    val tipoRefeicaoId: Int,
    @SerializedName("tipo_refeicao_nome")
    val tipoRefeicaoNome: String,
    @SerializedName("tipo_refeicao_ordem")
    val tipoRefeicaoOrdem: Int,
    @SerializedName("pratos")
    val pratos: List<PratoResponse> = emptyList()
)

// Espelha PratoSchema (mennu-api: core/api/schemas/prato/prato.py).
data class PratoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("tipo_prato")
    val tipoPrato: String,
    @SerializedName("nome")
    val nome: String,
    @SerializedName("restricoes")
    val restricoes: List<String> = emptyList()
)

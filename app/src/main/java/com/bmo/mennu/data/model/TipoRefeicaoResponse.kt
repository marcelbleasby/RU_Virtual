package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

// Espelha TipoRefeicaoSelfSchema (mennu-api: core/api/schemas/tipo_refeicao.py),
// exposto em GET /api/tipo-refeicao/minhas.
data class TipoRefeicaoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nome")
    val nome: String,
    @SerializedName("unidade_id")
    val unidadeId: Int,
    @SerializedName("horario_inicio")
    val horarioInicio: String, // "HH:mm:ss"
    @SerializedName("horario_fim")
    val horarioFim: String,
    @SerializedName("ordem")
    val ordem: Int
)

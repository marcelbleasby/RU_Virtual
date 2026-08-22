package com.bmo.mennu.data.model

import com.google.gson.annotations.SerializedName

// Espelha RefeicaoServidaSchema (mennu-api: core/api/schemas/refeicao.py).
// É um registro de presença (cardápio x usuário x data/hora), sem valor monetário
// nem tipo/método — diferente do conceito de "transação" que o app usava antes.
data class RefeicaoServida(
    @SerializedName("id")
    val id: Int,
    @SerializedName("cardapio_id")
    val cardapioId: Int,
    @SerializedName("usuario_id")
    val usuarioId: Int,
    @SerializedName("unidade_nome")
    val unidadeNome: String?,
    @SerializedName("usuario_nome")
    val usuarioNome: String?,
    @SerializedName("usuario_matricula")
    val usuarioMatricula: String?,
    @SerializedName("criado_por_id")
    val criadoPorId: Int?,
    @SerializedName("data_hora")
    val dataHora: String,
    @SerializedName("manual")
    val manual: Boolean,
    @SerializedName("motivo")
    val motivo: String?
)

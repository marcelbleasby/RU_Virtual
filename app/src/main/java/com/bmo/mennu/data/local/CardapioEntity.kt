package com.bmo.mennu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cardapio")
data class CardapioEntity(
    @PrimaryKey val id: Int,
    val dataRefeicao: String,
    val tipoRefeicaoId: Int,
    val tipoRefeicaoNome: String,
    val tipoRefeicaoOrdem: Int,
    // Lista de PratoResponse serializada — evita normalizar numa tabela separada
    // pra um payload pequeno por dia que sempre é lido/escrito inteiro.
    val pratosJson: String
)

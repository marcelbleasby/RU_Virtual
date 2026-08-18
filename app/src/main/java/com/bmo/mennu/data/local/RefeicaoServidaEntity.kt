package com.bmo.mennu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refeicao_servida")
data class RefeicaoServidaEntity(
    @PrimaryKey val id: Int,
    val usuarioId: Int,
    val cardapioId: Int,
    val unidadeNome: String?,
    val dataHora: String,
    val manual: Boolean
)

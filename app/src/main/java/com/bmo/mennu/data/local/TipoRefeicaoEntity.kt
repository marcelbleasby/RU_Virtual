package com.bmo.mennu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipo_refeicao")
data class TipoRefeicaoEntity(
    @PrimaryKey val id: Int,
    val nome: String,
    val unidadeId: Int,
    val horarioInicio: String,
    val horarioFim: String,
    val ordem: Int
)

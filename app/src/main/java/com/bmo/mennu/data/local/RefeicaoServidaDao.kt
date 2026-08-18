package com.bmo.mennu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RefeicaoServidaDao {
    @Query("SELECT * FROM refeicao_servida WHERE usuarioId = :usuarioId ORDER BY dataHora DESC")
    fun observeForUser(usuarioId: Int): Flow<List<RefeicaoServidaEntity>>

    @Query("DELETE FROM refeicao_servida WHERE usuarioId = :usuarioId")
    suspend fun deleteForUser(usuarioId: Int)

    @Insert
    suspend fun insertAll(items: List<RefeicaoServidaEntity>)

    @Transaction
    suspend fun replaceForUser(usuarioId: Int, items: List<RefeicaoServidaEntity>) {
        deleteForUser(usuarioId)
        insertAll(items)
    }
}

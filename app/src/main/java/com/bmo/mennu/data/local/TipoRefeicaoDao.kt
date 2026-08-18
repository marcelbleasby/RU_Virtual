package com.bmo.mennu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TipoRefeicaoDao {
    @Query("SELECT * FROM tipo_refeicao ORDER BY ordem")
    fun observeAll(): Flow<List<TipoRefeicaoEntity>>

    @Query("DELETE FROM tipo_refeicao")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<TipoRefeicaoEntity>)

    @Transaction
    suspend fun replaceAll(items: List<TipoRefeicaoEntity>) {
        deleteAll()
        insertAll(items)
    }
}

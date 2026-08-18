package com.bmo.mennu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CardapioDao {
    @Query("SELECT * FROM cardapio WHERE dataRefeicao BETWEEN :startIso AND :endIso")
    fun observeRange(startIso: String, endIso: String): Flow<List<CardapioEntity>>

    @Query("DELETE FROM cardapio WHERE dataRefeicao BETWEEN :startIso AND :endIso")
    suspend fun deleteRange(startIso: String, endIso: String)

    @Insert
    suspend fun insertAll(items: List<CardapioEntity>)

    @Transaction
    suspend fun replaceRange(startIso: String, endIso: String, items: List<CardapioEntity>) {
        deleteRange(startIso, endIso)
        insertAll(items)
    }
}

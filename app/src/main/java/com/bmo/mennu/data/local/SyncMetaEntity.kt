package com.bmo.mennu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Timestamp genérico de última sincronização, compartilhado entre os domínios
// que ganham cache offline (cardápio por semana, histórico de refeições por
// usuário) — evita repetir uma coluna lastSyncedAt em cada entidade.
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val lastSyncedAt: Long
)

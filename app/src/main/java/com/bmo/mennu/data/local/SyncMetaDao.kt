package com.bmo.mennu.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {
    @Query("SELECT lastSyncedAt FROM sync_meta WHERE `key` = :key")
    fun observeLastSyncedAt(key: String): Flow<Long?>

    @Upsert
    suspend fun upsert(meta: SyncMetaEntity)
}

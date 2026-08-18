package com.bmo.mennu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CardapioEntity::class,
        TipoRefeicaoEntity::class,
        RefeicaoServidaEntity::class,
        SyncMetaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardapioDao(): CardapioDao
    abstract fun tipoRefeicaoDao(): TipoRefeicaoDao
    abstract fun refeicaoServidaDao(): RefeicaoServidaDao
    abstract fun syncMetaDao(): SyncMetaDao
}

package com.bmo.mennu.di

import android.content.Context
import androidx.room.Room
import com.bmo.mennu.data.local.AppDatabase
import com.bmo.mennu.data.local.CardapioDao
import com.bmo.mennu.data.local.RefeicaoServidaDao
import com.bmo.mennu.data.local.SyncMetaDao
import com.bmo.mennu.data.local.TipoRefeicaoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "mennu.db").build()
    }

    @Provides
    fun provideCardapioDao(database: AppDatabase): CardapioDao = database.cardapioDao()

    @Provides
    fun provideTipoRefeicaoDao(database: AppDatabase): TipoRefeicaoDao = database.tipoRefeicaoDao()

    @Provides
    fun provideRefeicaoServidaDao(database: AppDatabase): RefeicaoServidaDao = database.refeicaoServidaDao()

    @Provides
    fun provideSyncMetaDao(database: AppDatabase): SyncMetaDao = database.syncMetaDao()
}

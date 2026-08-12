package com.bmo.mennu.presentation.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class MainViewModel(context: Context) : ViewModel() {

    private val vCardIdKey = stringPreferencesKey("vcard_id")
    private val refeicoesMesKey = intPreferencesKey("refeicoes_mes")

    val vCardId: StateFlow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[vCardIdKey]
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val refeicoesMes: StateFlow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[refeicoesMesKey]
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

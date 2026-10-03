package com.bmo.mennu.presentation.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.mennu.data.applyUserData
import com.bmo.mennu.data.dataStore
import com.bmo.mennu.data.pullUserData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val vCardIdKey = stringPreferencesKey("vcard_id")
    private val refeicoesMesKey = intPreferencesKey("refeicoes_mes")

    val vCardId: StateFlow<String?> = appContext.dataStore.data
        .map { preferences ->
            preferences[vCardIdKey]
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val refeicoesMes: StateFlow<Int?> = appContext.dataStore.data
        .map { preferences ->
            preferences[refeicoesMesKey]
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        pullUserData(appContext) { dataMap ->
            viewModelScope.launch { applyUserData(appContext, dataMap) }
        }
    }
}

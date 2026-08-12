package com.bmo.mennu.services

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bmo.mennu.nfc.MennuHostApduService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataLayerListenerService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents.forEach { event ->
            if (event.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED) {

                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                if (event.dataItem.uri.path == "/user_data") {
                    val vCardId = dataMapItem.dataMap.getString("vcard_id")
                    if (vCardId != null) {
                        Log.d("DataLayerListener", "Received VCardId: $vCardId")
                        // Update the in-memory cache for the NFC service
                        MennuHostApduService.vCardId = vCardId
                        serviceScope.launch { saveVCardIdToDataStore(vCardId) }
                    }

                    if (dataMapItem.dataMap.containsKey("refeicoes_mes")) {
                        val refeicoesMes = dataMapItem.dataMap.getInt("refeicoes_mes")
                        Log.d("DataLayerListener", "Received refeicoesMes: $refeicoesMes")
                        serviceScope.launch { saveRefeicoesMesToDataStore(refeicoesMes) }
                    }
                }
            }
        }
    }

    private suspend fun saveVCardIdToDataStore(vCardId: String) {
        dataStore.edit { preferences ->
            val key = stringPreferencesKey("vcard_id")
            preferences[key] = vCardId
            Log.d("DataLayerListener", "Saved VCardId to DataStore.")
        }
    }

    private suspend fun saveRefeicoesMesToDataStore(refeicoesMes: Int) {
        dataStore.edit { preferences ->
            val key = intPreferencesKey("refeicoes_mes")
            preferences[key] = refeicoesMes
            Log.d("DataLayerListener", "Saved refeicoesMes to DataStore.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

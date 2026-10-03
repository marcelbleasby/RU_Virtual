package com.bmo.mennu.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bmo.mennu.nfc.MennuHostApduService
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

// Único delegate do arquivo user_prefs: o DataStore não permite duas instâncias
// ativas para o mesmo arquivo no mesmo processo.
val Context.dataStore by preferencesDataStore(name = "user_prefs")

const val USER_DATA_PATH = "/user_data"

private const val TAG = "UserDataSync"

suspend fun applyUserData(context: Context, dataMap: DataMap) {
    val vCardId = dataMap.getString("vcard_id")
    val tenantSalt = dataMap.getString("tenant_salt")
    val hasRefeicoesMes = dataMap.containsKey("refeicoes_mes")
    val refeicoesMes = dataMap.getInt("refeicoes_mes")

    // Update the in-memory cache for the NFC service
    if (vCardId != null) MennuHostApduService.vCardId = vCardId
    if (tenantSalt != null) MennuHostApduService.tenantSalt = tenantSalt

    context.dataStore.edit { preferences ->
        if (vCardId != null) preferences[stringPreferencesKey("vcard_id")] = vCardId
        if (tenantSalt != null) preferences[stringPreferencesKey("tenant_salt")] = tenantSalt
        if (hasRefeicoesMes) preferences[intPreferencesKey("refeicoes_mes")] = refeicoesMes
    }
    Log.d(TAG, "User data saved (vCardId=${vCardId != null}, refeicoesMes=${if (hasRefeicoesMes) refeicoesMes else "-"})")
}

/**
 * onDataChanged só dispara quando o item muda. Ao abrir o app, lê o estado atual
 * já replicado pelo phone para não depender de um novo login/atualização.
 */
fun pullUserData(context: Context, onData: (DataMap) -> Unit) {
    Wearable.getDataClient(context).dataItems
        .addOnSuccessListener { buffer ->
            val maps = buffer
                .filter { it.uri.path == USER_DATA_PATH }
                .map { DataMapItem.fromDataItem(it).dataMap }
            buffer.release()
            Log.d(TAG, "Pulled ${maps.size} user data item(s)")
            maps.forEach(onData)
        }
        .addOnFailureListener { Log.e(TAG, "Failed to pull user data", it) }
}

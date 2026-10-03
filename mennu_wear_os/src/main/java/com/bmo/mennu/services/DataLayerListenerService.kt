package com.bmo.mennu.services

import android.util.Log
import com.bmo.mennu.data.USER_DATA_PATH
import com.bmo.mennu.data.applyUserData
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DataLayerListenerService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == USER_DATA_PATH) {
                // DataMap é copiado aqui; o buffer é liberado ao final do callback.
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                Log.d("DataLayerListener", "Received user data")
                serviceScope.launch { applyUserData(applicationContext, dataMap) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

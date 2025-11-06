package com.example.ru_virtual_wear_os.tiles

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.wear.tiles.*
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx a.wear.tiles.ResourceBuilders.Resources
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

private const val RESOURCES_VERSION = "1"
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class BalanceTileService : TileService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onTileRequest(requestParams: TileRequest) = serviceScope.future {
        val vCardId = vCardId()
        Tile.builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.builder()
                                    .setRoot(
                                        layout(vCardId, requestParams.deviceParameters!!)
                                    ).build()
                            ).build()
                    ).build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest) = Futures.immediateFuture(
        Resources.builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    )

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private suspend fun vCardId(): String {
        val key = stringPreferencesKey("vcard_id")
        val preferences = dataStore.data.first()
        return preferences[key] ?: "N/A"
    }

    private fun layout(vCardId: String, deviceParameters: LayoutElementBuilders.DeviceParameters) = Box.builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .addContent(
            Text.builder()
                .setText("VCard ID: $vCardId")
                .build()
        )
        .build()
}

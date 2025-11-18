package com.bmo.mennu.tiles

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TimelineBuilders

import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import androidx.wear.protolayout.LayoutElementBuilders

private const val RESOURCES_VERSION = "1"
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class BalanceTileService : TileService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest) = serviceScope.future {
        val vCardId = vCardId()
        TileBuilders.Tile.builder()
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

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest) = Futures.immediateFuture(
        ResourceBuilders.Resources.builder()
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

    private fun layout(vCardId: String, deviceParameters: DeviceParametersBuilders.DeviceParameters) = LayoutElementBuilders.Box.builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .addContent(
            LayoutElementBuilders.Text.builder()
                .setText("VCard ID: $vCardId")
                .build()
        )
        .build()
}

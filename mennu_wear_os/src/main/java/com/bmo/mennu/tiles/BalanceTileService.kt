package com.bmo.mennu.tiles

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import androidx.wear.tiles.DeviceParametersBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
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

private const val RESOURCES_VERSION = "1"
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class BalanceTileService : TileService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest) = serviceScope.future {
        val refeicoesMes = refeicoesMes()
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(
                                        layout(refeicoesMes, requestParams.deviceParameters!!)
                                    ).build()
                            ).build()
                    ).build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest) = Futures.immediateFuture(
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    )

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private suspend fun refeicoesMes(): Int? {
        val key = intPreferencesKey("refeicoes_mes")
        val preferences = dataStore.data.first()
        return preferences[key]
    }

    private fun layout(refeicoesMes: Int?, deviceParameters: DeviceParametersBuilders.DeviceParameters) = LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(if (refeicoesMes != null) "Refeições este mês: $refeicoesMes" else "Indisponível")
                .build()
        )
        .build()
}

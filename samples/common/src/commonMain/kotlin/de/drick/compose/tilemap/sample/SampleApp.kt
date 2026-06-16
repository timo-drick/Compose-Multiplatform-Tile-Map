package de.drick.compose.tilemap.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.LogSettings
import de.drick.compose.tilemap.TileMapView
import de.drick.compose.tilemap.drawScaleBar
import de.drick.compose.tilemap.rememberViewPortState
import de.drick.compose.tilemap.tileMapPointerInput
import de.drick.compose.tilemap.tileProviderDipul
import de.drick.compose.tilemap.tileProviderDipulZones
import de.drick.compose.tilemap.tileProviderMapBoxDark
import de.drick.compose.tilemap.tileProviderMapBoxSat
import de.drick.compose.tilemap.tileProviderOsm


@Composable
fun SampleApp() {
    LogSettings.logPlatform = { msg: () -> String, error: Throwable? ->
        log(msg())
    }
    val tileProvider = remember {
        tileProviderMapBoxDark(MAPBOX_TOKEN)
    }
    val state = rememberViewPortState(
        initialZoom = 12f,
        initPos = GeoPoint(52.5207, 13.4094), // Berlin
        tileProvider = arrayOf(tileProviderOsm),
    )
    val textMeasurer = rememberTextMeasurer()
    TileMapView(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .tileMapPointerInput(state),
        onDrawLabel = {
            drawScaleBar(state.metersPerPixel(), size, textMeasurer, Color.Black)
        }
    )
   /* TestMapUavZonesPortugal(
        modifier = Modifier.fillMaxSize().tileMapPointerInput(state),
        viewPortState = state
    )*/
}

expect fun log(msg: String)

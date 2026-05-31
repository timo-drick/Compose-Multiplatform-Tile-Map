package de.drick.compose.tilemap.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.TileMapView
import de.drick.compose.tilemap.rememberViewPortState
import de.drick.compose.tilemap.tileMapPointerInput
import de.drick.compose.tilemap.tileProviderMapBoxSat

@Composable
fun SampleApp() {
    val state = rememberViewPortState(
        initialZoom = 12f,
        initPos = GeoPoint(52.5207, 13.4094), // Berlin
        tileProvider = arrayOf(tileProviderMapBoxSat(MAPBOX_TOKEN))
    )
    TileMapView(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .tileMapPointerInput(state)
    )
    //TestMapUavZonesPortugal(Modifier.fillMaxSize(), isDarkMode = true)
}

expect fun log(msg: String)

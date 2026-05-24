package de.drick.compose.tilemap.sample

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import de.drick.compose.tilemap.BuildConfig
import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.TileMapView
import de.drick.compose.tilemap.rememberViewPortState
import de.drick.compose.tilemap.tileProviderOsm

const val MAPBOX_TOKEN = BuildConfig.MAPBOX_TOKEN

@Composable
fun SampleApp() {
    val state = rememberViewPortState(
        initialZoom = 12f,
        initPos = GeoPoint(52.5207, 13.4094), // Berlin
        tileProvider = arrayOf(tileProviderOsm)
    )
    /*TileMapView(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .draggable2D(
                state = rememberDraggable2DState { offset ->
                    state.movePx(offset.x, offset.y)
                }
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val inputChange = event.changes.first()
                            val scrollDelta = inputChange.scrollDelta.y.coerceIn(-1f, 1f)
                            state.zoom(state.zoom - scrollDelta)
                        }
                    }
                }
            }
    )*/
    TestMapUavZonesPortugal(Modifier.fillMaxSize())
}

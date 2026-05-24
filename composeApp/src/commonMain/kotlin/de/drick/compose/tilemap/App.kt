package de.drick.compose.tilemap

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import org.jetbrains.compose.ui.tooling.preview.Preview

val p1 = GeoPoint(50.16041006147746, 8.714076768439526)
val p2 = GeoPoint(50.16951012919926, 8.714636717976504)
private val zoom = 13f

@Composable
fun App() {
    val state = rememberViewPortState(
        initialZoom = zoom,
        initPos = p1,
        tileSize = 512,
        //tileProviderDipul,
        tileProviderOsm,
        //tileProviderMapBox,
        tileProviderDipulZones,
        tileProviderFrZones
    )
    MaterialTheme {
        TileMapView(
            modifier = Modifier
                .fillMaxSize()
                .focusable()
                .draggable2D(
                    state = rememberDraggable2DState {
                        offset ->
                        // Log.d("Draggable2D", "Dragged to $offset")
                        state.movePx(offset.x, offset.y)
                    }
                ),
            state = state
        ) {
            val o1 = p1.toOffset()
            val o2 = p2.toOffset()
            drawLine(Color.White, o1, o2, 10f)
            drawCircle(Color.Green, radius = 15f, center = o1, style = Fill)
            drawCircle(Color.Green, radius = 15f, center = o2)
        }
        Row(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            Button(onClick = { state.zoom(state.zoom + 1) }) {
                Text("Zoom in")
            }
            Button(onClick = { state.zoom(state.zoom - 1) }) {
                Text("Zoom out")
            }
        }
    }
}

@Composable
private fun HelloWorldPreview() {
    Box(Modifier.fillMaxSize().background(Color.Green)) {

    }
}
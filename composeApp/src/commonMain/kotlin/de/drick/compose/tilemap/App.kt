package de.drick.compose.tilemap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import composemultiplatformtilemap.composeapp.generated.resources.Res
import composemultiplatformtilemap.composeapp.generated.resources.compose_multiplatform

val p1 = GeoPoint(52.51629369771545, 13.377614937339327)
val p2 = GeoPoint(52.51460371598352, 13.35008338366604)
val zoom = 10f

@Composable
@Preview
fun App() {
    val state = remember {
        ViewPortState(zoom, p1)
    }
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
        Row {
            Button(onClick = { state.zoom(state.zoom + 1) }) {
                Text("Zoom in")
            }
            Button(onClick = { state.zoom(state.zoom - 1) }) {
                Text("Zoom out")
            }
        }
    }
}
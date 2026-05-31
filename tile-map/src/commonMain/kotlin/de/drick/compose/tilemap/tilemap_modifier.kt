package de.drick.compose.tilemap

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.tileMapPointerInput(state: ViewPortState) = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Scroll) {
                val inputChange = event.changes.first()
                val scrollDelta = inputChange.scrollDelta.y.coerceIn(-1f, 1f)
                state.smoothZoom(state.zoom - scrollDelta, inputChange.position)
                log("Zoom: $scrollDelta centroid: ${inputChange.position}")
            }
        }
    }
}
    .pointerInput(Unit) {
        detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, rotation ->
            // Update scale with the zoom
            log("zoom: $zoom pan: $pan rotation: $rotation centroid: $centroid")
            val dampedZoom = 1f + (zoom - 1f) * 0.1f
            state.zoom(state.zoom * dampedZoom, centroid)
            state.movePx(pan.x, pan.y)
            state.rotate(-rotation, centroid)
        }
    }

package de.drick.compose.tilemap

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs

fun Modifier.tileMapPointerInput(state: ViewPortState) = this
    .pointerInput(Unit) {
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
    /*.pointerInput(Unit) {
        val velocityTracker = VelocityTracker()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val isStopped = event.changes.all { it.changedToUp() }
                if (isStopped) {
                    val velocity = velocityTracker.calculateVelocity()
                    if (abs(velocity.x) > 100 || abs(velocity.y) > 100) {
                        state.fling(Offset(velocity.x, velocity.y))
                    }
                    velocityTracker.resetTracking()
                } else {
                    event.changes.forEach { change ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                    }
                }
            }
        }
    }*/
    .pointerInput(Unit) {
        detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, rotation ->
            // Update scale with the zoom
            log("zoom: $zoom pan: $pan rotation: $rotation centroid: $centroid")
            val dampedZoom = 1f + (zoom - 1f) * 0.1f
            state.zoom(state.zoom * dampedZoom, centroid)
            state.rotate(-rotation, centroid)
            state.movePx(pan.x, pan.y)
        }
    }

package de.drick.compose.tilemap.blog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.singleWindowApplication

val p1 = GeoPoint(52.51629369771545, 13.377614937339327)
val p2 = GeoPoint(52.51460371598352, 13.35008338366604)
val zoom = 12

fun main() = singleWindowApplication {
    TileMapViewFill(
        modifier = Modifier.fillMaxSize(),
        zoom = zoom,
        start = p1
    )
}
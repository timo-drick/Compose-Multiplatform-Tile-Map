package de.drick.compose.tilemap.blog.v1

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.drick.compose.tilemap.blog.GeoPoint
import de.drick.compose.tilemap.blog.loadTile
import de.drick.compose.tilemap.blog.toTilePos


@Composable
fun MinimalTileMapView(
    zoom: Int = 1,
    start: GeoPoint,
    modifier: Modifier = Modifier,
) {
    val size = with(LocalDensity.current) { 256.dp.toPx().toInt() }
    var tileImage by remember { mutableStateOf<ImageBitmap?>(null) }
    // Load the tile image when zoom or start point changes
    LaunchedEffect(zoom, start) {
        val tilePos = start.toTilePos(zoom)
        tileImage = loadTile(tilePos)
    }
    // Draw the tile image on a Canvas
    Canvas(modifier) {
        tileImage?.let { image ->
            drawImage(
                image = image,
                dstSize = IntSize(size, size)
            )
        }
    }
}
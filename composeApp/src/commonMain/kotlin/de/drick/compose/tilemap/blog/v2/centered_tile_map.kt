package de.drick.compose.tilemap.blog.v2

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.drick.compose.tilemap.blog.GeoPoint
import de.drick.compose.tilemap.blog.TilePos
import de.drick.compose.tilemap.blog.loadTile
import de.drick.compose.tilemap.blog.toTilePos
import kotlin.math.roundToInt

@Composable
fun CenteredTileMapView(
    zoom: Int = 1,
    start: GeoPoint,
    modifier: Modifier = Modifier,
) {
    val centerPos = remember(start, zoom) {
        start.toTilePos(zoom)
    }
    val tileSize = with(LocalDensity.current) { 256.dp.toPx().toInt() }
    fun calculateOffset(p: TilePos) = IntOffset(
        ((p.x.toInt() - centerPos.x) * tileSize).roundToInt(),
        ((p.y.toInt() - centerPos.y) * tileSize).roundToInt()
    )
    var tileImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val offset = remember { calculateOffset(centerPos) }
    // Load the tile image when zoom or start point changes
    LaunchedEffect(zoom, start) {
        tileImage = loadTile(centerPos)
    }
    // Draw the tile image on a Canvas
    Canvas(modifier) {
        translate(size.width / 2f, size.height / 2f) {
            tileImage?.let { image ->
                drawImage(
                    image = image,
                    dstOffset = offset,
                    dstSize = IntSize(tileSize, tileSize)
                )
            }
            drawCircle(Color.Green, radius = 10f, center = Offset.Zero)
        }
    }
}
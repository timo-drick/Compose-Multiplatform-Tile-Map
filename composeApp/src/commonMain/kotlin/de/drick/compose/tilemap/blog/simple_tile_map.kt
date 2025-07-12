package de.drick.compose.tilemap.blog

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.drick.compose.tilemap.radians
import io.ktor.client.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

data class GeoPoint(val latitude: Double, val longitude: Double)
data class TilePos(val zoom: Int, val x: Double, val y: Double)

private fun lonToTileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * (1 shl zoom)
}

private fun latToTileY(lat: Double, zoom: Int): Double {
    val latRad = radians(lat)
    return (1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
}

fun GeoPoint.toTilePos(zoom: Int) = TilePos(
    zoom = zoom,
    x = lonToTileX(longitude, zoom),
    y = latToTileY(latitude, zoom)
)

fun osmFree(p: TilePos) = "https://tile.openstreetmap.org/${p.zoom}/${p.x.toInt()}/${p.y.toInt()}.png"

private val client = HttpClient {
    install(HttpCache)
}

suspend fun loadTile(pos: TilePos) = withContext(Dispatchers.Default) {
    val response = client.request(osmFree(pos))
    response.bodyAsBytes().decodeToImageBitmap()
}

private val dstSize = IntSize(512, 512)

@Composable
fun TileMapView(
    zoom: Int = 1,
    start: GeoPoint,
    modifier: Modifier = Modifier,
) {
    var tileImage by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(zoom, start) {
        val tilePos = start.toTilePos(zoom)
        tileImage = loadTile(tilePos)
    }
    Canvas(modifier) {
        tileImage?.let { image ->
            drawImage(image = image, dstOffset = IntOffset.Zero, dstSize = dstSize)
        }
    }
}

@Composable
fun TileMapView2(
    zoom: Int = 1,
    start: GeoPoint,
    modifier: Modifier = Modifier,
) {
    var tileImageList by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(zoom, start) {
        val tilePos = start.toTilePos(zoom)
        //tileImage = loadTile(tilePos)
    }
    Canvas(modifier) {
        /*tileImage?.let { image ->
            drawImage(image = image, dstOffset = IntOffset.Zero, dstSize = dstSize)
        }*/
    }
}
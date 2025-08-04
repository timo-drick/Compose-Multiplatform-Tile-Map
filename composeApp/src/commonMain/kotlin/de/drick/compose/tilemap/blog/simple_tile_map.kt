package de.drick.compose.tilemap.blog

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

data class GeoPoint(val latitude: Double, val longitude: Double)
data class TilePos(val zoom: Int, val x: Double, val y: Double)

// There is no built-in toRadians/toDegrees in Kotlin Multiplatform
fun Double.toRadians(): Double = this / 180.0 * PI
fun Double.toDegrees(): Double = this * 180.0 / PI

fun GeoPoint.toTilePos(zoom: Int): TilePos {
    val n = 1 shl zoom
    val latRad = latitude.toRadians()
    return TilePos(
        zoom = zoom,
        x = (longitude + 180.0) / 360.0 * n,
        y = (1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * n
    )
}

private val client by lazy { HttpClient() }
suspend fun loadTile(p: TilePos): ImageBitmap? = withContext(Dispatchers.Default) {
    val url = "https://tile.openstreetmap.org/${p.zoom}/${p.x.toInt()}/${p.y.toInt()}.png"
    val response = client.request(url)
    response.bodyAsBytes().decodeToImageBitmap() //TODO error handling
}

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
        //tileImage = loadTile(tilePos)
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

@Composable
fun TileMapViewCenterPoint(
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

data class TileImage(
    val pos: TilePos,
    val image: ImageBitmap
)

@Composable
fun TileMapViewFill(
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
    var tileImageList: List<TileImage> by remember { mutableStateOf(emptyList()) }
    var canvasSizePx by remember { mutableStateOf(Size.Zero) }
    // Load the tile image when zoom or start point changes
    LaunchedEffect(zoom, start, canvasSizePx) {
        // Calculate the range of tiles to load based on the current canvas size
        val nX = (canvasSizePx.width / 2f / tileSize).roundToInt()+ 1
        val nY = (canvasSizePx.height / 2f / tileSize).roundToInt() + 1
        val xRange = (centerPos.x.toInt() - nX)..(centerPos.x.toInt() + nX)
        val yRange = (centerPos.y.toInt() - nY)..(centerPos.y.toInt() + nY)
        val imageList = mutableListOf<TileImage>()
        for (x in xRange) {
            for (y in yRange) {
                val tilePos = TilePos(zoom, x.toDouble(), y.toDouble())
                val image = loadTile(tilePos)
                if (image != null) {
                    imageList.add(TileImage(tilePos, image))
                }
            }
        }
        tileImageList = imageList
    }
    // Draw the tile image on a Canvas
    Canvas(modifier) {
        if (canvasSizePx != size) {
            canvasSizePx = size
        }
        translate(size.width / 2f, size.height / 2f) {
            for (tileImage in tileImageList) {
                drawImage(
                    image = tileImage.image,
                    dstOffset = calculateOffset(tileImage.pos),
                    dstSize = IntSize(tileSize, tileSize)
                )
            }
            drawCircle(Color.Green, radius = 10f, center = Offset.Zero)
        }
    }
}

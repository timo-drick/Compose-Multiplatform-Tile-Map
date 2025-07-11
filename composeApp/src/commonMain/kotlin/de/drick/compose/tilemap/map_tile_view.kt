package de.drick.compose.tilemap

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import de.drick.tools.log
import kotlinx.coroutines.launch

data class GeoPoint(val latitude: Double, val longitude: Double)

data class TileImage(
    val zoom: Int,
    val x: Int,
    val y: Int,
    var image: ImageBitmap?
)

data class TilePos(
    val zoom: Int,
    val x: Double,
    val y: Double
) {
    val tileX get() = x.toInt()
    val tileY get() = y.toInt()
}

interface TileProvider {
    val tileSize: Int
    suspend fun loadTile(zoom: Int, x: Int, y: Int): ImageBitmap
    fun cachedTile(zoom: Int, x: Int, y: Int): ImageBitmap?
}

data class VisibleTileRange(
    val startX: Int,
    val stopX: Int,
    val startY: Int,
    val stopY: Int
)

class ViewPortState(
    zoom: Float = 0f,
    center: GeoPoint = GeoPoint(0.0, 0.0),
    tileProvider: TileProvider = osmTileProvider,
) {
    var zoom by mutableStateOf(zoom)
        private set
    val tileZoom get() = zoom.toInt()
    var centerPos by mutableStateOf(center.toTilePos(tileZoom))
    var tileProvider by mutableStateOf(tileProvider)
    var pxSize = Size(0f, 0f)

    fun center(point: GeoPoint) {
        centerPos = point.toTilePos(tileZoom)
    }
    fun zoom(newZoom: Float) {
        val pos = centerPos.toGeoPoint() // After zoom level changed we need to recalculate the center position
        zoom = newZoom
        centerPos = pos.toTilePos(tileZoom)
    }
    fun movePx(x: Float, y: Float) {
        val newX = centerPos.x - x / tileProvider.tileSize
        val newY = centerPos.y - y / tileProvider.tileSize
        centerPos = centerPos.copy(
            x = newX,
            y = newY
        )
    }
}

interface MapDrawScope : DrawScope {
    fun GeoPoint.toOffset(): Offset
    fun TilePos.toOffset(): Offset
}

private class MapDrawScopeImpl(
    private val delegate: DrawScope,
    private val viewPortState: ViewPortState,
) : MapDrawScope, DrawScope by delegate {
    override fun GeoPoint.toOffset(): Offset {
        val tilePos = this.toTilePos(viewPortState.tileZoom)
        return tilePos.toOffset()
    }
    override fun TilePos.toOffset(): Offset {
        val size = viewPortState.tileProvider.tileSize
        return Offset(
            x = ((this.x - viewPortState.centerPos.x) * size).toFloat(),
            y = ((this.y - viewPortState.centerPos.y) * size).toFloat()
        )
    }
}

@Composable
fun TileMapView(
    state: ViewPortState,
    modifier: Modifier = Modifier,
    onDraw: MapDrawScope.() -> Unit = {}
) {
    val zoom = state.tileZoom
    val centerPos = state.centerPos
    val tileProvider = state.tileProvider

    val tileList = remember {
        mutableListOf<TileImage>()
    }
    var sizePx by remember { mutableStateOf(Size.Zero) }
    var visibleRange by remember { mutableStateOf(VisibleTileRange(0, 0, 0, 0)) }
    var invalidateCounter by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(sizePx, centerPos, zoom) {
        // Calculate visible tiles
        if (sizePx != Size.Zero) {
            val minX = (sizePx.width / 2f / tileProvider.tileSize).toInt()
            val minY = (sizePx.height / 2f / tileProvider.tileSize).toInt()
            val range = VisibleTileRange(
                startX = centerPos.tileX - minX - 1,
                stopX = centerPos.tileX + minX + 1,
                startY = centerPos.tileY - minY - 1,
                stopY = centerPos.tileY + minY + 1
            )
            if (range != visibleRange) {
                val maxTiles = 1 shl zoom
                fun wrapPos(pos: Int) = pos % maxTiles
                log("Prepare new tiles")
                visibleRange = range
                val newTiles = mutableListOf<TileImage>()
                for (x in range.startX..range.stopX) {
                    for (y in range.startY..range.stopY) {
                        // Check if tile is already loaded
                        val existingTile = tileList.find { it.zoom == zoom && it.x == x && it.y == y && it.image != null }
                        if (existingTile != null) {
                            newTiles.add(existingTile)
                        } else {
                            val cachedImage = tileProvider
                                .cachedTile(zoom, wrapPos(x), wrapPos(y))
                            newTiles.add(TileImage(zoom, x, y, cachedImage))
                        }
                    }
                }
                tileList.clear()
                tileList.addAll(newTiles)
                invalidateCounter++
                //Loading tiles
                scope.launch {
                    val tilesToLoad = newTiles.filter { it.image == null }
                    log("load ${tilesToLoad.size}")
                    tilesToLoad.forEach { tile ->
                        try {
                            val image = tileProvider.loadTile(zoom, wrapPos(tile.x), wrapPos(tile.y))
                            tile.image = image
                            log("Tile loaded: ${wrapPos(tile.x)}, ${wrapPos(tile.y)}")
                            invalidateCounter++
                        } catch (err: Throwable) {
                            log(err)
                        }
                    }
                }
            }
        }
    }
    Canvas(modifier) {
        state.pxSize = size
        val mapDrawScope = MapDrawScopeImpl(this, state)
        sizePx = size
        val frame = invalidateCounter
        translate(size.width / 2, size.height / 2) {
            tileList.forEach { tile ->
                val tileOffset = Offset(
                    ((tile.x - centerPos.x) * tileProvider.tileSize).toFloat(),
                    ((tile.y - centerPos.y) * tileProvider.tileSize).toFloat()
                )
                tile.image?.let { image ->
                    drawImage(image, topLeft = tileOffset)
                }
            }
            onDraw(mapDrawScope)
        }
    }
}
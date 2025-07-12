package de.drick.compose.tilemap

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.drick.tools.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class GeoPoint(val latitude: Double, val longitude: Double)

data class TileImage(
    val pos: TilePos,
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

data class VisibleTileRange(
    val startX: Int,
    val stopX: Int,
    val startY: Int,
    val stopY: Int
)

class ViewPortState(
    initialZoom: Float = 0f,
    initialPos: GeoPoint = GeoPoint(0.0, 0.0),
    tileProvider: TileProvider = TileProvider(
        tileSize = 512,
        tileLoaderUrl = ::dipul
    ),
    val scope: CoroutineScope
) {
    var zoom by mutableStateOf(initialZoom)
        private set
    val tileZoom get() = zoom.toInt()
    var centerPos by mutableStateOf(initialPos.toTilePos(tileZoom))
    private val tileProvider by mutableStateOf(tileProvider)
    private var sizePx = Size(0f, 0f)

    val tileSize = IntSize(tileProvider.tileSize, tileProvider.tileSize)

    val tileList = mutableListOf<TileImage>()
    var invalidateCounter by mutableIntStateOf(0)

    fun updateSize(size: Size) {
        sizePx = size
        update()
    }

    fun center(point: GeoPoint) {
        centerPos = point.toTilePos(tileZoom)
        update()
    }
    fun zoom(newZoom: Float) {
        val pos = centerPos.toGeoPoint() // After zoom level changed we need to recalculate the center position
        zoom = newZoom
        centerPos = pos.toTilePos(tileZoom)
        update()
    }
    fun movePx(x: Float, y: Float) {
        val newX = centerPos.x - x / tileProvider.tileSize
        val newY = centerPos.y - y / tileProvider.tileSize
        centerPos = centerPos.copy(
            x = newX,
            y = newY
        )
        update()
        invalidateCounter++
    }

    private var visibleRange = VisibleTileRange(0, 0, 0, 0)

    fun calculateOffset(pos: TilePos) = IntOffset(
        ((pos.x - centerPos.x) * tileProvider.tileSize).roundToInt(),
        ((pos.y - centerPos.y) * tileProvider.tileSize).roundToInt()
    )

    private fun update() {
        scope.launch {
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
                    log("Prepare new tiles")
                    visibleRange = range
                    val newTiles = mutableListOf<TileImage>()
                    for (x in range.startX..range.stopX) {
                        for (y in range.startY..range.stopY) {
                            val tp = TilePos(tileZoom, x.toDouble(), y.toDouble())
                            // Check if tile is already loaded
                            val existingTile = tileList.find { it.pos == tp && it.image != null }
                            if (existingTile != null) {
                                newTiles.add(existingTile)
                            } else {
                                val cachedImage = tileProvider
                                    .cachedTile(tp)
                                newTiles.add(TileImage(tp, cachedImage))
                            }
                        }
                    }
                    tileList.clear()
                    tileList.addAll(newTiles)
                    invalidateCounter++
                    //Loading tiles
                    val tilesToLoad = newTiles.filter { it.image == null }
                    log("load ${tilesToLoad.size}")
                    tilesToLoad.forEach { tile ->
                        try {
                            val image = tileProvider.loadTile(tile.pos)
                            tile.image = image
                            log("Tile loaded: ${tile.pos}")
                            invalidateCounter++
                        } catch (err: CancellationException) {
                            throw err // Propagate cancellation to stop when scope is canceled
                        } catch (err: Throwable) {
                            log(err)
                        }
                    }
                }
            }
        }
    }

    fun geoPointToOffset(p: GeoPoint): Offset {
        val tilePos = p.toTilePos(tileZoom)
        return tilePosToOffset(tilePos)
    }
    fun tilePosToOffset(p: TilePos): Offset {
        val size = tileProvider.tileSize
        return Offset(
            x = ((p.x - centerPos.x) * size).toFloat(),
            y = ((p.y - centerPos.y) * size).toFloat()
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
    override fun GeoPoint.toOffset() = viewPortState.geoPointToOffset(this)
    override fun TilePos.toOffset() = viewPortState.tilePosToOffset(this)
}

@Composable
fun rememberViewPortState(
    initialZoom: Float = 0f,
    initPos: GeoPoint = GeoPoint(0.0, 0.0),
    tileProvider: TileProvider = TileProvider(
        tileSize = 512,
        tileLoaderUrl = ::dipul
    )
): ViewPortState {
    val scope = rememberCoroutineScope()
    return remember(tileProvider) {
        ViewPortState(initialZoom, initPos, tileProvider, scope)
    }
}

@Composable
fun TileMapView(
    state: ViewPortState,
    modifier: Modifier = Modifier,
    onDraw: MapDrawScope.() -> Unit = {}
) {
    Canvas(modifier) {
        state.updateSize(size)
        val mapDrawScope = MapDrawScopeImpl(this, state)
        val frame = state.invalidateCounter
        translate(size.width / 2, size.height / 2) {
            for (tile in state.tileList) {
                tile.image?.let { image ->
                    drawImage(image, dstOffset = state.calculateOffset(tile.pos), dstSize = state.tileSize)
                }
            }
            onDraw(mapDrawScope)
        }
    }
}
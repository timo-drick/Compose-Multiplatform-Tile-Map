package de.drick.compose.tilemap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import composemultiplatformtilemap.tile_map.generated.resources.Res
import composemultiplatformtilemap.tile_map.generated.resources.preview_map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh


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

    fun toGeoPoint() = GeoPoint(
        latitude = tileYToLat(y, zoom),
        longitude = tileXToLon(x, zoom)
    )

    fun wrap(): TilePos {
        val n = (1 shl zoom).toDouble()
        val wrappedX = ((x % n) + n) % n
        val wrappedY = ((y % n) + n) % n
        return copy(x = wrappedX, y = wrappedY)
    }

    companion object {
        fun tileXToLon(x: Double, zoom: Int): Double {
            val n = 1 shl zoom
            return x / n * 360.0 - 180.0
        }

        fun tileYToLat(y: Double, zoom: Int): Double {
            val n = 1 shl zoom
            val latRad = atan(sinh(PI * (1 - 2.0 * y / n)))
            return latRad.toDegrees()
        }
    }
}

data class VisibleTileRange(
    val startX: Int,
    val stopX: Int,
    val startY: Int,
    val stopY: Int
)

class TileLayerState(
    private val tileProvider: TileProvider,
    private val onInvalidate: () -> Unit
) {
    fun findParentTile(pos: TilePos): Pair<ImageBitmap, Int>? =
        tileProvider.cachedParentTile(pos)

    val tileList = mutableListOf<TileImage>()
    suspend fun update(
        newTileList: List<TilePos>
    ) {
        log("Prepare new tiles")
        val newTiles = newTileList.map { tp ->
            val existingTile = tileList.find { it.pos == tp && it.image != null }
            if (existingTile != null) {
                existingTile
            } else {
                val cachedImage = tileProvider.cachedTile(tp)
                TileImage(tp, cachedImage)
            }
        }
        tileList.clear()
        tileList.addAll(newTiles)
        onInvalidate()
        //Loading tiles
        val tilesToLoad = newTiles.filter { it.image == null }
        log("${tileProvider.name}: load ${tilesToLoad.size}")

        tilesToLoad.forEach { tile ->
            log("${tileProvider.name}: loading...: ${tile.pos}")
            val image = tileProvider.loadTile(tile.pos)
            tile.image = image
            log("${tileProvider.name}: loaded: ${tile.pos}")
            onInvalidate()
        }
        log("${tileProvider.name}: Loading finished")
    }
}

class ViewPortState(
    val scope: CoroutineScope,
    initialZoom: Float = 10f,
    initialPos: GeoPoint = GeoPoint(0.0, 0.0),
    val tileSize: Int = 512,
    vararg tileProviderList: TileProvider
) {
    init {
        log("Create instance: $this")
    }
    var zoom by mutableStateOf(initialZoom)
        private set
    /** Rotation angle in degrees (clockwise). */
    var rotation by mutableStateOf(0f)
    val tileZoom get() = zoom.toInt()
    /** Scale factor for fractional zoom: tiles are rendered at tileSize * tileScale */
    val tileScale: Float get() = 2f.pow(zoom - tileZoom)
    val scaledTileSize: Float get() = tileSize * tileScale
    var centerPos by mutableStateOf(initialPos.toTilePos(tileZoom))
    var tileStateList = tileProviderList.toList().toStateList()
    val size: IntSize get() = IntSize(scaledTileSize.toInt() + 1, scaledTileSize.toInt() + 1)

    private var sizePx = Size(0f, 0f)
    var invalidateCounter by mutableIntStateOf(0)

    fun List<TileProvider>.toStateList() = map { provider ->
        TileLayerState(provider) {
            invalidateCounter++
            log("Invalid counter: $invalidateCounter")
        }
    }

    fun updateTileProvider(vararg providerList: TileProvider){
        val newStateList = providerList.toList().toStateList()
        tileStateList = newStateList
        visibleRange = VisibleTileRange(0, 0, 0, 0)
        update()
        log("Update tile provider")
    }

    fun updateSize(size: Size) {
        if (size != sizePx) {
            log("Update size: $size old($sizePx)")
            sizePx = size
            update()
        }
    }

    fun center(point: GeoPoint) {
        centerPos = point.toTilePos(tileZoom)
        update()
    }

    private var smoothMoveJob: Job? = null
    fun smoothCenter(
        point: GeoPoint,
        animationSpec: AnimationSpec<Float> = tween(durationMillis = 500, easing = LinearEasing)
    ) {
        smoothMoveJob?.cancel()
        val startPos = centerPos
        val targetPos = point.toTilePos(tileZoom)

        val dx = targetPos.x - startPos.x
        val dy = targetPos.y - startPos.y

        smoothMoveJob = scope.launch(Dispatchers.Main.immediate) {
            val anim = Animatable(0f)
            anim.animateTo(1f, animationSpec) {
                centerPos = startPos.copy(
                    x = startPos.x + dx * value.toDouble(),
                    y = startPos.y + dy * value.toDouble()
                ).wrap()
                update()
            }
        }
    }

    fun zoom(newZoom: Float, centroid: Offset?) {
        // Vector from screen center to centroid in pixels
        val cx = centroid?.x?.let { it - sizePx.width / 2f } ?: 0f
        val cy = centroid?.y?.let { it - sizePx.height / 2f } ?: 0f

        // Rotate centroid offset to map coordinates
        val rad = rotation.toDouble().toRadians()
        val cosR = cos(rad).toFloat()
        val sinR = sin(rad).toFloat()
        val mapCx = cx * cosR + cy * sinR
        val mapCy = -cx * sinR + cy * cosR

        // Centroid offset in tile units at current zoom
        val tileOffX = mapCx / scaledTileSize
        val tileOffY = mapCy / scaledTileSize

        // The geo position under the centroid (in tile coords at current zoom)
        val centroidTileX = centerPos.x + tileOffX
        val centroidTileY = centerPos.y + tileOffY

        // Convert centroid tile position to geo so it survives the zoom change
        val centroidGeo = TilePos(tileZoom, centroidTileX, centroidTileY).toGeoPoint()

        // Apply new zoom
        smoothMoveJob?.cancel()
        zoom = newZoom

        // Recompute centroid position in new tile coords
        val newCentroidTile = centroidGeo.toTilePos(tileZoom)

        // New center = centroid tile pos minus the same pixel offset (in new tile units)
        val newScaledTileSize = tileSize * 2f.pow(zoom - tileZoom)
        val newTileOffX = mapCx / newScaledTileSize
        val newTileOffY = mapCy / newScaledTileSize

        centerPos = TilePos(
            tileZoom,
            newCentroidTile.x - newTileOffX,
            newCentroidTile.y - newTileOffY
        ).wrap()
        update()
    }
    private var smoothZoomJob: Job? = null
    fun smoothZoom(
        newZoom: Float,
        centroid: Offset?,
        animationSpec: AnimationSpec<Float> = tween(durationMillis = 500, easing = LinearEasing)
    ) {
        smoothZoomJob?.cancel()

        smoothZoomJob = scope.launch(Dispatchers.Main.immediate) {
            val anim = Animatable(zoom)
            anim.animateTo(newZoom, animationSpec) {
                zoom(value, centroid)
                update()
            }
        }
    }

    fun rotate(angleDelta: Float, centroid: Offset?) {
        // Vector from screen center to centroid in pixels
        val cx = centroid?.x?.let { it - sizePx.width / 2f } ?: 0f
        val cy = centroid?.y?.let { it - sizePx.height / 2f } ?: 0f

        // Convert centroid screen offset to map (tile) coordinates using current rotation
        val radBefore = rotation.toDouble().toRadians()
        val cosBefore = cos(radBefore).toFloat()
        val sinBefore = sin(radBefore).toFloat()
        val mapCx = cx * cosBefore - cy * sinBefore
        val mapCy = cx * sinBefore + cy * cosBefore

        // Geo point under the centroid (must stay fixed on screen)
        val centroidTileX = centerPos.x + mapCx / scaledTileSize
        val centroidTileY = centerPos.y + mapCy / scaledTileSize

        // Apply rotation
        rotation = (rotation + angleDelta) % 360f

        // Convert same screen centroid offset to map coordinates using NEW rotation
        val radAfter = rotation.toDouble().toRadians()
        val cosAfter = cos(radAfter).toFloat()
        val sinAfter = sin(radAfter).toFloat()
        val mapCxAfter = cx * cosAfter - cy * sinAfter
        val mapCyAfter = cx * sinAfter + cy * cosAfter

        // New center so that centroid tile pos maps back to the same screen point
        centerPos = centerPos.copy(
            x = centroidTileX - mapCxAfter / scaledTileSize,
            y = centroidTileY - mapCyAfter / scaledTileSize
        ).wrap()

        update()
        invalidateCounter++
    }
    fun movePx(x: Float, y: Float) {
        // Rotate the pixel delta back to map coordinates
        val rad = rotation.toDouble().toRadians()
        val cosR = cos(rad).toFloat()
        val sinR = sin(rad).toFloat()
        val mapX = x * cosR + y * sinR
        val mapY = -x * sinR + y * cosR
        val newX = centerPos.x - mapX / scaledTileSize
        val newY = centerPos.y - mapY / scaledTileSize
        //log("Old pos: $centerPos -> mx: $x my: $y")
        centerPos = centerPos.copy(
            x = newX,
            y = newY
        ).wrap() // make sure we do stay in the positive valid numbers
        update()
        invalidateCounter++
    }

    private var visibleRange = VisibleTileRange(0, 0, 0, 0)

    fun calculateOffset(pos: TilePos) = IntOffset(
        ((pos.x - centerPos.x) * scaledTileSize).roundToInt(),
        ((pos.y - centerPos.y) * scaledTileSize).roundToInt()
    )
    private var updateJob: Job? = null

    private fun update() {
        if (sizePx != Size.Zero) {
            // When rotated, the bounding box of the rotated viewport is larger
            val rad = rotation.toDouble().toRadians()
            val cosR = abs(cos(rad)).toFloat()
            val sinR = abs(sin(rad)).toFloat()
            val effectiveWidth = sizePx.width * cosR + sizePx.height * sinR
            val effectiveHeight = sizePx.width * sinR + sizePx.height * cosR
            val minX = (effectiveWidth / 2f / scaledTileSize).roundToInt()
            val minY = (effectiveHeight / 2f / scaledTileSize).roundToInt()
            val range = VisibleTileRange(
                startX = centerPos.tileX - minX - 1,
                stopX = centerPos.tileX + minX + 1,
                startY = centerPos.tileY - minY - 1,
                stopY = centerPos.tileY + minY + 1
            )
            if (visibleRange != range) {
                updateJob?.cancel()
                updateJob = scope.launch(Dispatchers.Main.immediate) {
                    log("Update tile list center: $centerPos")
                    log("Range: $range - $visibleRange")
                    val newTileList = mutableListOf<TilePos>()
                    for (x in range.startX..range.stopX) {
                        for (y in range.startY..range.stopY) {
                            newTileList.add(TilePos(tileZoom, x.toDouble(), y.toDouble()))
                        }
                    }
                    for (tileState in tileStateList) {
                        launch {
                            tileState.update(newTileList)
                        }
                    }
                    visibleRange = range
                }
            }
        }
    }

    /**
     * Returns the number of meters represented by one pixel at the current zoom level and center latitude.
     */
    fun metersPerPixel(): Double {
        val latRad = centerPos.toGeoPoint().latitude.toRadians()
        val n = 1 shl tileZoom
        return 2.0 * PI * EARTH_RADIUS_WGS84 * cos(latRad) / (n * scaledTileSize)
    }

    fun geoPointToOffset(p: GeoPoint): Offset {
        val tilePos = p.toTilePos(tileZoom)
        return tilePosToOffset(tilePos)
    }
    fun tilePosToOffset(p: TilePos): Offset {
        return Offset(
            x = ((p.x - centerPos.x) * scaledTileSize).toFloat(),
            y = ((p.y - centerPos.y) * scaledTileSize).toFloat()
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
    initialZoom: Float = 10f,
    initPos: GeoPoint = GeoPoint(0.0, 0.0),
    tileSize: Int = 512,
    vararg tileProvider: TileProvider = arrayOf(tileProviderOsm)
): ViewPortState {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        ViewPortState(scope, initialZoom, initPos, tileSize, *tileProvider)
    }
}

@Composable
fun rememberViewPortState(
    isDarkMode: Boolean,
    lightTileProvider: TileProvider,
    initialZoom: Float = 10f,
    initPos: GeoPoint = GeoPoint(0.0, 0.0),
    tileSize: Int = 512,
    darkTileProvider: TileProvider = lightTileProvider
): ViewPortState {
    val provider = if (isDarkMode) darkTileProvider else lightTileProvider
    val scope = rememberCoroutineScope()
    val state = remember(scope) {
        ViewPortState(scope, initialZoom, initPos, tileSize, provider)
    }
    LaunchedEffect(isDarkMode) {
        state.updateTileProvider(provider)
    }
    return state
}


private val scaleSteps = listOf(
    1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000,
    20000, 50000, 100000, 200000, 500000, 1000000, 2000000
)

private fun DrawScope.drawScaleBar(
    metersPerPixel: Double,
    canvasSize: Size,
    textMeasurer: TextMeasurer
) {
    val maxBarWidthPx = canvasSize.width / 2f
    val maxBarMeters = maxBarWidthPx * metersPerPixel
    val scaleMeters = scaleSteps.lastOrNull { it <= maxBarMeters } ?: scaleSteps.first()
    val barWidthPx = (scaleMeters / metersPerPixel).toFloat()

    val label = if (scaleMeters >= 1000) "${scaleMeters / 1000} km" else "$scaleMeters m"

    val margin = 16f
    val tickHeight = 8f
    val barY = canvasSize.height - margin - tickHeight
    val barStartX = margin
    val barEndX = margin + barWidthPx

    // White outline for contrast
    val outlineColor = Color.White
    val barColor = Color.Black
    val strokeOutline = 4f
    val strokeBar = 2f

    // Draw outline
    drawLine(
        color = outlineColor,
        start = Offset(barStartX, barY),
        end = Offset(barEndX, barY),
        strokeWidth = strokeOutline + strokeBar
    )
    drawLine(
        color = outlineColor,
        start = Offset(barStartX, barY - tickHeight / 2),
        end = Offset(barStartX, barY + tickHeight / 2),
        strokeWidth = strokeOutline + strokeBar
    )
    drawLine(
        color = outlineColor,
        start = Offset(barEndX, barY - tickHeight / 2),
        end = Offset(barEndX, barY + tickHeight / 2),
        strokeWidth = strokeOutline + strokeBar
    )

    // Draw bar
    drawLine(barColor, Offset(barStartX, barY), Offset(barEndX, barY), strokeWidth = strokeBar)
    drawLine(
        color = barColor,
        start = Offset(barStartX, barY - tickHeight / 2),
        end = Offset(barStartX, barY + tickHeight / 2),
        strokeWidth = strokeBar
    )
    drawLine(
        color = barColor,
        start = Offset(barEndX, barY - tickHeight / 2),
        end = Offset(barEndX, barY + tickHeight / 2),
        strokeWidth = strokeBar
    )

    // Draw label
    val textLayoutResult = textMeasurer.measure(label, TextStyle(color = barColor, fontSize = 12.sp))
    val textX = barStartX + (barWidthPx - textLayoutResult.size.width) / 2
    val textY = barY - tickHeight / 2 - textLayoutResult.size.height - 2f
    // White background for text
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(textX, textY),
        style = TextStyle(
            color = outlineColor,
            fontSize = 12.sp,
            shadow = androidx.compose.ui.graphics.Shadow(color = outlineColor, blurRadius = 4f)
        )
    )
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(textX, textY),
        style = TextStyle(color = barColor, fontSize = 12.sp)
    )
}

@Composable
fun TileMapView(
    state: ViewPortState,
    modifier: Modifier = Modifier,
    onDraw: MapDrawScope.() -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        Image(
            modifier = modifier,
            painter = painterResource(Res.drawable.preview_map),
            contentDescription = "Map preview",
            contentScale = ContentScale.Crop
        )
    } else {
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier.clipToBounds()) {
            state.updateSize(size)
            val mapDrawScope = MapDrawScopeImpl(this, state)
            val frame = state.invalidateCounter // needed to redraw when invalidated
            //log("Frame: $frame")
            rotate(-state.rotation) {
                translate(size.width / 2, size.height / 2) {
                for (tileState in state.tileStateList) {
                    for (tile in tileState.tileList) {
                        val image = tile.image
                        if (image != null) {
                            drawImage(
                                image = image,
                                dstOffset = state.calculateOffset(tile.pos),
                                dstSize = state.size
                            )
                        } else {
                            // Try to find a parent tile at a lower zoom level
                            tileState.findParentTile(tile.pos)?.let { (parentImage, parentZoom) ->
                                val zoomDiff = tile.pos.zoom - parentZoom
                                val scale = 1 shl zoomDiff
                                val subX = tile.pos.tileX % scale
                                val subY = tile.pos.tileY % scale
                                val srcTileWidth = parentImage.width / scale
                                val srcTileHeight = parentImage.height / scale
                                drawImage(
                                    image = parentImage,
                                    srcOffset = IntOffset(subX * srcTileWidth, subY * srcTileHeight),
                                    srcSize = IntSize(srcTileWidth, srcTileHeight),
                                    dstOffset = state.calculateOffset(tile.pos),
                                    dstSize = state.size
                                )
                            }
                        }
                    }
                }
                onDraw(mapDrawScope)
              }
            }
            drawScaleBar(state.metersPerPixel(), size, textMeasurer)
        }
    }
}

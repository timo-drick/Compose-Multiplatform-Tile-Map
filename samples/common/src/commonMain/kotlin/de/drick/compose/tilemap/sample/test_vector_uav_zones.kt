package de.drick.compose.tilemap.sample

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import de.drick.compose.tilemap.CircleProjection
import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.LonLat
import de.drick.compose.tilemap.PolygonProjection
import de.drick.compose.tilemap.TileMapView
import de.drick.compose.tilemap.TilePos
import de.drick.compose.tilemap.UASZoneData
import de.drick.compose.tilemap.ViewPortState
import de.drick.compose.tilemap.ZoneGeometry
import de.drick.compose.tilemap.loadUavZones
import de.drick.compose.tilemap.metersToTileLength
import de.drick.compose.tilemap.rememberViewPortState
import de.drick.compose.tilemap.tileMapPointerInput
import de.drick.compose.tilemap.tileProviderMapBoxDark
import de.drick.compose.tilemap.tileProviderMapBoxLight
import de.drick.compose.tilemap.toTilePos


fun LonLat.toGeoPoint() = GeoPoint(latitude = lat, longitude = lon)

data class MapCircle(
    val p: GeoPoint,
    val r: Float, // radius in tile
    val color: Color
)

data class MapPolygon(
    val points: List<TilePos>,
    val color: Color
)

@Composable
fun TestMapUavZonesPortugal(
    viewPortState: ViewPortState,
    modifier: Modifier = Modifier,
) {
    var zones by remember { mutableStateOf<UASZoneData?>(null) }
    var uavZoneCircles by remember { mutableStateOf<List<MapCircle>>(emptyList()) }
    var uavZonePolygons by remember { mutableStateOf<List<MapPolygon>>(emptyList()) }
    LaunchedEffect(Unit) {
        zones = loadUavZones()
    }
    LaunchedEffect(zones, viewPortState.zoom) {
        val zoom = viewPortState.tileZoom
        val color = Color.Red.copy(alpha = 0.3f)
        zones?.let { zoneData ->
            val geometryList = zoneData.features.flatMap { it.geometry }
            uavZoneCircles = extractCircles(geometryList, zoom, viewPortState, color)
            uavZonePolygons = extractPolygons(geometryList, zoom, color)
        }
    }
    TileMapView(
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .tileMapPointerInput(viewPortState),
        state = viewPortState,
    ) {
        uavZoneCircles.forEach { zc ->
            drawCircle(
                color = zc.color,
                radius = zc.r,
                center = zc.p.toOffset()
            )
        }
        uavZonePolygons.forEach { poly ->
            val points = poly.points.map { it.toOffset() }
            if (points.size > 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    close()
                }
                drawPath(
                    path = path,
                    color = poly.color
                )
            }
            drawPoints(
                points = points,
                pointMode = PointMode.Polygon,
                color = poly.color
            )
        }
    }
}

private fun extractCircles(
    geometryList: List<ZoneGeometry>,
    zoom: Int,
    viewPortState: ViewPortState,
    color: Color
) = geometryList
    .map { it.horizontalProjection }
    .filterIsInstance<CircleProjection>()
    .map {
        val p = it.center.toGeoPoint()
        val radius = metersToTileLength(p, zoom, it.radius)
        val pxRadius = radius.toFloat() * viewPortState.tileSize.toFloat()
        MapCircle(p, pxRadius.coerceIn(1f, null), color)
    }

private fun extractPolygons(
    geometryList: List<ZoneGeometry>,
    zoom: Int,
    color: Color
) = geometryList
    .map { it.horizontalProjection }
    .filterIsInstance<PolygonProjection>()
    .flatMap {
        it.coordinates
    }.map { coordinates: List<LonLat> ->
        val pointList = coordinates.map { lonLat ->
            lonLat.toGeoPoint().toTilePos(zoom)
        }
        MapPolygon(pointList, color)
    }

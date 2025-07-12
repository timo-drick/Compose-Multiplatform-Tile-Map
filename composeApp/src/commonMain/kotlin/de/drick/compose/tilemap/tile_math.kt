package de.drick.compose.tilemap

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

fun GeoPoint.toTilePos(zoom: Int): TilePos {
    val posX = lonToTileX(longitude, zoom)
    val posY = latToTileY(latitude, zoom)
    return TilePos(
        zoom = zoom,
        x = posX,
        y = posY
    )
}

fun TilePos.toGeoPoint() = GeoPoint(
    latitude = tileYToLat(y, zoom),
    longitude = tileXToLon(x, zoom)
)

private fun lonToTileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * (1 shl zoom)
}

private fun latToTileY(lat: Double, zoom: Int): Double {
    val latRad = radians(lat)
    return (1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
}

private fun tileXToLon(x: Double, zoom: Int): Double {
    val n = 1 shl zoom
    return x / n * 360.0 - 180.0
}

private fun tileYToLat(y: Double, zoom: Int): Double {
    val n = 1 shl zoom
    val latRad = atan(sinh(PI * (1 - 2.0 * y / n)))
    return degrees(latRad)
}

fun radians(degrees: Double): Double = degrees * PI / 180.0
fun degrees(radians: Double): Double = radians * 180.0 / PI

// https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84
val earthRadius = 6378137.0 // in meters WGS84

// EPSG:4326 lun,lat

// EPSG:3857 meters mercator projection

data class MeterPos(val x: Double, val y: Double)

fun GeoPoint.toMeter() = convert4326To3857(this)
fun MeterPos.toGeoPoint() = convert3857To4326(this)

private fun convert4326To3857(geoPoint: GeoPoint): MeterPos {
    val x = earthRadius * radians(geoPoint.longitude)
    val y = earthRadius * ln(tan(PI / 4 + radians(geoPoint.latitude) / 2))
    return MeterPos(x, y)
}

private fun convert3857To4326(meterPos: MeterPos): GeoPoint {
    val lon = degrees(meterPos.x / earthRadius)
    val lat = degrees(2 * atan(exp(meterPos.y / earthRadius)) - PI / 2)
    return GeoPoint(lat, lon)
}
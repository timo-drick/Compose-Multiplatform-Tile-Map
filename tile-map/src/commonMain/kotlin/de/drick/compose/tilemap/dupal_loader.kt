package de.drick.compose.tilemap

import io.ktor.http.URLBuilder
import kotlin.math.max
import kotlin.math.min

/**
 * German uav zones: https://maptool-dipul.dfs.de
 */

val tileProviderDipul = TileProvider(
    name = "Dipul",
    tileLoaderUrl = { pos ->
        URLBuilder("https://sgx.geodatenzentrum.de/wms_topplus_open").apply {
            parameters.apply {
                append("REQUEST", "GetMap")
                append("SERVICE", "WMS")
                append("STYLES", "")
                append("VERSION", "1.3.0")
                append("FORMAT", "image/png")
                append("TRANSPARENT", "TRUE")
                append("LAYERS", "web_scale_grau")
                append("TILED", "true")
                append("WIDTH", "256")
                append("HEIGHT", "256")
                append("CRS", "EPSG:3857")
                with(calculateBoundingBox(pos)) {
                    append("BBOX", "$startX,$startY,$endX,$endY")
                }
            }
        }.build()
    }
)

val layer = listOf(
    "flugplaetze", "flughaefen", "kontrollzonen", "flugbeschraenkungsgebiete", "bundesautobahnen", "bundesstrassen",
    "bahnanlagen", "binnenwasserstrassen", "seewasserstrassen", "schifffahrtsanlagen", "wohngrundstuecke", "freibaeder",
    "industrieanlagen", "kraftwerke", "umspannwerke", "stromleitungen", "windkraftanlagen", "justizvollzugsanstalten",
    "militaerische_anlagen", "labore", "behoerden", "diplomatische_vertretungen", "internationale_organisationen",
    "polizei", "sicherheitsbehoerden", "krankenhaeuser", "nationalparks", "naturschutzgebiete", "vogelschutzgebiete",
    "ffh-gebiete", "modellflugplaetze", "haengegleiter"
).map { "dipul:$it" }


val tileProviderDipulZones = TileProvider(
    name = "Dipul Zones",
    tileLoaderUrl = { pos ->
        URLBuilder("https://uas-betrieb.de/geoservices/dipul/wms").apply {
            parameters.apply {
                append("REQUEST", "GetMap")
                append("SERVICE", "WMS")
                append("STYLES", "")
                append("VERSION", "1.3.0")
                append("FORMAT", "image/png")
                append("FORMAT_OPTIONS", "dpi:180")
                append("TRANSPARENT", "TRUE")
                append("TILED", "true")
                append("WIDTH", "512")
                append("HEIGHT", "512")
                append("CRS", "EPSG:3857")

                append("LAYERS", layer.joinToString(","))
                with(calculateBoundingBox(pos)) {
                    append("BBOX", "$startX,$startY,$endX,$endY")
                }
            }
        }.build()
    }
)

data class BBox(val startX: Int, val startY: Int, val endX: Int, val endY: Int)

fun calculateBoundingBox(pos: TilePos): BBox {
    val start = pos.toGeoPoint() // Left top corner of the tile
    val end = pos.copy(x = pos.x + 1, y = pos.y + 1).toGeoPoint() // Right bottom corner of the tile
    val meterStart = start.toMeter()
    val meterEnd = end.toMeter()
    val startx = min(meterEnd.x, meterStart.x)
    val starty = min(meterEnd.y, meterStart.y)
    val endx = max(meterStart.x, meterEnd.x)
    val endy = max(meterStart.y, meterEnd.y)
    return BBox(
        startX = startx.toInt(),
        startY = starty.toInt(),
        endX = endx.toInt(),
        endY = endy.toInt()
    )
}

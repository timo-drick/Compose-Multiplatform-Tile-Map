package de.drick.compose.tilemap

import io.ktor.http.URLBuilder


// source: https://www.geoportail.gouv.fr/donnees/restrictions-uas-categorie-ouverte-et-aeromodelisme
val tileProviderFrZones = TileProvider(
    name = "Fr Zones",
    tileLoaderUrl = { pos ->
        URLBuilder("https://data.geopf.fr/wmts").apply {
            parameters.apply {
                append("layer", "TRANSPORTS.DRONES.RESTRICTIONS")
                append("style", "normal")
                append("tilematrixset", "PM")
                append("Service", "WMTS")
                append("Request", "GetTile")
                append("Version", "1.0.0")
                append("Format", "image/png")
                append("TileMatrix", pos.zoom.toString())
                append("TileCol", pos.tileX.toString())
                append("TileRow", pos.tileY.toString())
            }
        }.build()
    }
)
# Compose Multiplatform Tile Map

[![Maven Central](https://img.shields.io/maven-central/v/de.drick.compose/multiplatformtilemap)](https://mvnrepository.com/artifact/de.drick.compose/multiplatformtilemap)

A platform-independent, tile-based map component written in pure Compose Multiplatform common code.

The reusable map library lives in `tile-map` and the `samples` folder show how to use the library.

## Features

- Pure Kotlin + Compose Multiplatform implementation
- Cross-platform targets in the library: Android, JVM, and WasmJS
- Tile rendering with viewport-aware loading
- Built-in gesture integration via `tileMapPointerInput(...)`
- Map providers for OpenStreetMap and Mapbox variants
- Geographic conversion utilities (`GeoPoint`, `TilePos`, projection helpers)
- Overlay drawing support through `TileMapView(onDraw = { ... })`
- Dynamic tile provider switching (for light/dark mode use cases)

## Project Structure

- `/tile-map`: published Kotlin Multiplatform library module
- `/samples/common`: shared sample UI/composables using `TileMapView`
- `/samples/jvm`: desktop runner for the sample app
- `/samples/android`: Android runner for the sample app

## Maven Library

Published from `tile-map`:

- Group: `de.drick.compose`
- Artifact: `multiplatformtilemap`

[![Maven Central](https://img.shields.io/maven-central/v/de.drick.compose/multiplatformtilemap)](https://mvnrepository.com/artifact/de.drick.compose/multiplatformtilemap)

```kotlin
dependencies {
    implementation("de.drick.compose:multiplatformtilemap:<version>")
}
```

## Using `TileMapView`

### Minimal setup

```kotlin
@Composable
fun SimpleMap(mapboxToken: String) {
    val state = rememberViewPortState(
        initialZoom = 12f,
        initPos = GeoPoint(52.5207, 13.4094), // Berlin
        tileProvider = arrayOf(tileProviderMapBoxSat(mapboxToken))
    )

    TileMapView(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .tileMapPointerInput(state)
    )
}
```

### Draw custom overlays

```kotlin
@Composable
fun OverlayMap(state: ViewPortState, circles: List<Pair<GeoPoint, Float>>) {
    TileMapView(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .tileMapPointerInput(state)
    ) {
        circles.forEach { (point, radiusPx) ->
            drawCircle(
                color = Color.Red.copy(alpha = 0.3f),
                center = point.toOffset(),
                radius = radiusPx
            )
        }
    }
}
```

## Sample Implementation References

- Minimal app setup: `samples/common/src/commonMain/kotlin/de/drick/compose/tilemap/sample/SampleApp.kt`
- Advanced overlay example (`onDraw`, polygons/circles):
  `samples/common/src/commonMain/kotlin/de/drick/compose/tilemap/sample/test_vector_uav_zones.kt`

Run desktop sample:

```bash
./gradlew :samples:jvm:run
```

## Resources

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
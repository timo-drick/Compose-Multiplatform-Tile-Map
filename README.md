# Compose Multiplatform Tile Map

A platform-independent, tile-based map view implementation written in pure Compose Multiplatform common code.

This repository has been migrated to a library-first setup. The reusable map component now lives in the `tile-map` module and is published as a Maven library.

## Features

- **Pure Kotlin Implementation**: Written entirely in Kotlin using Compose Multiplatform for cross-platform compatibility
- **Tile-Based Rendering**: Efficiently loads and displays map tiles based on the current viewport
- **Multiple Map Sources**: Supports OpenStreetMap and Mapbox tile sources
- **Viewport Management**: Handles panning, zooming, and viewport calculations
- **Geographic Projections**: Converts between geographic coordinates (latitude/longitude) and tile coordinates
- **Memory Efficient**: Implements LRU caching for loaded tiles
- **Custom Drawing**: Allows drawing custom elements (markers, lines, shapes) on top of the map

## Project Structure

* `/tile-map` contains the published Kotlin Multiplatform library module
  - `commonMain` contains the platform-independent map implementation
  - platform source sets provide Android/JVM/Wasm support

* `/sample` contains a sample app that demonstrates library usage

* `/composeApp` and `/iosApp` are app modules used for local/demo runs

## Maven Library

The project is configured for Maven publishing via the `tile-map` module.

- **Group**: `de.drick.compose`
- **Artifact**: `edge-to-edge-preview`
- **Version**: `0.1.0`

Include it in your project from Maven Central (once published):

```kotlin
dependencies {
    implementation("de.drick.compose:edge-to-edge-preview:0.1.0")
}
```

## Implementation Details

The map view is implemented using several key components:

- **TileMapView**: The main composable function that renders the map
- **ViewPortState**: Manages the state of the map view (zoom, center position)
- **TileProvider**: Interface for loading map tiles with implementations for different sources
- **GeoPoint/TilePos**: Data classes for handling geographic and tile coordinates
- **Tile Math**: Functions for converting between geographic and tile coordinates

## Getting Started

To try the project locally, run the sample/demo applications:

- **Sample desktop app**: Run `:sample:jvmRun`
- **Compose app (Web)**: Run `:composeApp:wasmJsBrowserDevelopmentRun`
- **Compose app (Android/iOS/Desktop)**: Use the run configurations in your IDE

## Resources

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
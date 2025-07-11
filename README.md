# Compose Multiplatform Tile Map

A platform-independent, tile-based map view implementation written in pure Compose Multiplatform common code. This project provides a reusable component for displaying interactive maps across Android, iOS, Desktop, and Web platforms.

## Features

- **Pure Kotlin Implementation**: Written entirely in Kotlin using Compose Multiplatform for cross-platform compatibility
- **Tile-Based Rendering**: Efficiently loads and displays map tiles based on the current viewport
- **Multiple Map Sources**: Supports OpenStreetMap and Mapbox tile sources
- **Viewport Management**: Handles panning, zooming, and viewport calculations
- **Geographic Projections**: Converts between geographic coordinates (latitude/longitude) and tile coordinates
- **Memory Efficient**: Implements LRU caching for loaded tiles
- **Custom Drawing**: Allows drawing custom elements (markers, lines, shapes) on top of the map

## Project Structure

* `/composeApp` contains the shared code for the Compose Multiplatform application:
  - `commonMain` contains the platform-independent map implementation
  - Other folders contain platform-specific code for Android, iOS, Desktop, and Web

* `/iosApp` contains the iOS application entry point

## Implementation Details

The map view is implemented using several key components:

- **TileMapView**: The main composable function that renders the map
- **ViewPortState**: Manages the state of the map view (zoom, center position)
- **TileProvider**: Interface for loading map tiles with implementations for different sources
- **GeoPoint/TilePos**: Data classes for handling geographic and tile coordinates
- **Tile Math**: Functions for converting between geographic and tile coordinates

## Getting Started

You can run the application on different platforms:

- **Web**: Run the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task
- **Android**: Run the Android configuration in your IDE
- **iOS**: Open the Xcode project in the `/iosApp` directory
- **Desktop**: Run the Desktop configuration in your IDE

## Resources

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
# AGENTS Guide

This file provides repository-specific guidance for coding agents and contributors.

## Scope

- Focus on active modules:
  - `tile-map` (published Kotlin Multiplatform library)
  - `samples` (`samples/common`, `samples/jvm`, `samples/android` demo apps using `tile-map`)
  - `log` (small KMP logging utility used by library)
- **Ignore `/composeApp` for now** (out of current maintenance scope).

## Project Overview

- Repository type: Kotlin Multiplatform + Compose Multiplatform.
- Core artifact is published from `tile-map`:
  - Group: `de.drick.compose`
  - Artifact: `multiplatformtilemap`
  - Version: `0.2.0`
- Included Gradle modules (from `settings.gradle.kts`):
  - `:tile-map`
  - `:log`
  - `:samples:common`
  - `:samples:jvm`
  - `:samples:android`

## Module Responsibilities

### `tile-map`

- Main reusable map component/library.
- KMP targets: Android, JVM, WasmJS.
- Contains production map logic in `src/commonMain` and tests in `src/commonTest`.
- Maven publishing is configured via `com.vanniktech.maven.publish`.

### `samples`

- Demonstrates usage of `tile-map`.
- Contains shared sample code and platform runners (JVM desktop + Android).
- Generates `BuildConfig.kt` from `MAPBOX_TOKEN` env var or `local.properties` (`mapbox.token`).

### `log`

- Lightweight multiplatform logging abstraction used by other modules.

## Build & Test Commands

Run from repository root:

- Run all `tile-map` common tests:
  - `./gradlew :tile-map:allTests`
- Run a specific test class (example):
  - `./gradlew :tile-map:allTests --tests "tilemap.TestAltitudeInterpolation"`
- Run sample desktop app:
  - `./gradlew :samples:jvm:run`

If you changed only docs, tests/build can be skipped.

## Coding Conventions

- Keep changes minimal and module-scoped.
- Follow existing Kotlin and Gradle Kotlin DSL style in nearby files.
- Prefer edits in `tile-map` for library behavior; use `samples` only for demos/examples.
- Do not add secrets to source files; use environment variables or `local.properties`.

## TileMapView Sample References

- Minimal `TileMapView` setup with pointer input:
  - `samples/common/src/commonMain/kotlin/de/drick/compose/tilemap/sample/SampleApp.kt`
- Advanced `TileMapView` overlay drawing (`onDraw`) with polygons/circles:
  - `samples/common/src/commonMain/kotlin/de/drick/compose/tilemap/sample/test_vector_uav_zones.kt`

## Agent Safety Rules

- Do not modify generated/third-party assets unless explicitly required.
- Avoid touching `iosApp` and `/composeApp` unless a future task explicitly requests it.
- When changing public behavior/API in `tile-map`, update or add tests in `tile-map/src/commonTest`.

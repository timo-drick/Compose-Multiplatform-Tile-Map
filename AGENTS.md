# AGENTS Guide

This file provides repository-specific guidance for coding agents and contributors.

## Scope

- Focus on active modules:
  - `tile-map` (published Kotlin Multiplatform library)
  - `sample` (desktop/JVM sample app using `tile-map`)
  - `log` (small KMP logging utility used by library)
- **Ignore `/composeApp` for now** (out of current maintenance scope).

## Project Overview

- Repository type: Kotlin Multiplatform + Compose Multiplatform.
- Core artifact is published from `tile-map`:
  - Group: `de.drick.compose`
  - Artifact: `composemultiplatformtilemap`
  - Version: `0.1.0`
- Included Gradle modules (from `settings.gradle.kts`):
  - `:tile-map`
  - `:log`
  - `:sample`

## Module Responsibilities

### `tile-map`

- Main reusable map component/library.
- KMP targets: Android, JVM, WasmJS.
- Contains production map logic in `src/commonMain` and tests in `src/commonTest`.
- Maven publishing is configured via `com.vanniktech.maven.publish`.

### `sample`

- Demonstrates usage of `tile-map`.
- Currently configured for JVM desktop runtime.
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
  - `./gradlew :sample:jvmRun`

If you changed only docs, tests/build can be skipped.

## Coding Conventions

- Keep changes minimal and module-scoped.
- Follow existing Kotlin and Gradle Kotlin DSL style in nearby files.
- Prefer edits in `tile-map` for library behavior; use `sample` only for demos/examples.
- Do not add secrets to source files; use environment variables or `local.properties`.

## Agent Safety Rules

- Do not modify generated/third-party assets unless explicitly required.
- Avoid touching `iosApp` and `/composeApp` unless a future task explicitly requests it.
- When changing public behavior/API in `tile-map`, update or add tests in `tile-map/src/commonTest`.

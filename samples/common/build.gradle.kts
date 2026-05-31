import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig")
    outputs.dir(outputDir)

    val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
    inputs.files(localPropertiesFile).optional()

    doLast {
        val localProperties = Properties().apply {
            val propsFile = localPropertiesFile.asFile
            if (propsFile.exists()) {
                propsFile.inputStream().use { load(it) }
            }
        }

        val mapboxToken: String = System.getenv("MAPBOX_TOKEN")
            ?: localProperties.getProperty("MAPBOX_TOKEN", "")

        val dir = outputDir.get().asFile.resolve("de/drick/compose/tilemap/sample")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package de.drick.compose.tilemap.sample
            |
            |const val MAPBOX_TOKEN = "$mapboxToken"
            """.trimMargin()
        )
    }
}

kotlin {
    android {
        namespace = "de.drick.compose.tilemap.sample.common"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                api(project(":tile-map"))
                api(libs.compose.foundation)
                api(libs.compose.ui)
                api(libs.compose.material3)
                api(libs.compose.components.resources)
            }
        }
    }
}
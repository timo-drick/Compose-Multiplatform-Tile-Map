import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}


// Generate BuildConfig with secrets
val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig")
    outputs.dir(outputDir)

    // Use input property for configuration cache compatibility
    val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
    inputs.files(localPropertiesFile).optional()

    doLast {
        // Read local.properties for secrets like mapboxToken
        val localProperties = Properties().apply {
            val propsFile = localPropertiesFile.asFile
            if (propsFile.exists()) {
                propsFile.inputStream().use { load(it) }
            }
        }

        val mapboxToken: String = System.getenv("MAPBOX_TOKEN")
            ?: localProperties.getProperty("mapbox.token", "")

        val dir = outputDir.get().asFile.resolve("de/drick/compose/tilemap")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package de.drick.compose.tilemap
            |
            |object BuildConfig {
            |    const val MAPBOX_TOKEN = "$mapboxToken"
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(project(":tile-map"))
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.components.resources)
            }
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.vanniktechMavenPublish)
}

val mavenGroupId = "de.drick.compose"
val mavenArtifactId = "composemultiplatformtilemap"
val baseVersion = "0.1.0"

val isSnapshot = providers.environmentVariable("PUBLISH_SNAPSHOT")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

val mavenVersion = if (isSnapshot.get()) "$baseVersion-SNAPSHOT" else baseVersion

kotlin {

    android {
        namespace = "de.drick.compose.tilemap"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 26
        androidResources {
            enable = true
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":log"))

                implementation(libs.kotlin.stdlib)

                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.components.resources)
        }
    }
}

mavenPublishing {
    coordinates(mavenGroupId, mavenArtifactId, mavenVersion)
    configure(
        KotlinMultiplatform(
            sourcesJar = SourcesJar.Sources(),
            androidVariantsToPublish = listOf("release")
        )
    )
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name.set("Compose Multiplatform Tile Map")
        description.set("""
            TileMap library written in pure common code. So it will work on every supported platform.
        """.trimIndent())
        url.set("https://github.com/timo-drick/Compose-Multiplatform-Tile-Map")
        licenses {
            license {
                name = "The Unlicense"
                url = "https://unlicense.org/"
            }
        }
        developers {
            developer {
                id.set("timo-drick")
                name.set("Timo Drick")
                url.set("https://github.com/timo-drick")
            }
        }
        scm {
            url.set("https://github.com/timo-drick/Compose-Multiplatform-Tile-Map")
            connection.set("scm:git:git://github.com/timo-drick/Compose-Multiplatform-Tile-Map.git")
            developerConnection.set("scm:git:ssh://git@github.com/timo-drick/Compose-Multiplatform-Tile-Map.git")
        }
    }
}

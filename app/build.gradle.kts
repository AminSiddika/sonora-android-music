import com.android.build.api.variant.BuildConfigField
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * Sonora builds without any secret. The optional YouTube Data API v3 key is
 * resolved from (in order):
 *   1. -Pyoutube.api.key=... on the command line or in gradle.properties
 *   2. the YOUTUBE_API_KEY environment variable (CI passes the repository secret here)
 *   3. `youtube.api.key` inside the untracked local.properties file
 * When none are present the field is empty and the app runs in catalogue-only mode.
 */
val localPropertiesFile = rootProject.file("local.properties")
val localYoutubeKey: String = runCatching {
    if (localPropertiesFile.exists()) {
        Properties().apply { localPropertiesFile.inputStream().use { load(it) } }
            .getProperty("youtube.api.key")
            .orEmpty()
    } else {
        ""
    }
}.getOrDefault("")

val youtubeApiKey: String = (
    providers.gradleProperty("youtube.api.key").orNull
        ?: providers.environmentVariable("YOUTUBE_API_KEY").orNull
        ?: localYoutubeKey
    ).orEmpty().trim()

android {
    namespace = "dev.sonora.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.sonora.app"
        minSdk {
            version = release(26)
        }
        targetSdk {
            version = release(37)
        }
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
            )
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
        // Informational version suggestions are noise for a sample project.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        // Sonora opts in to the stable-but-annotated Compose APIs it relies on
        // (top app bars, flow row layouts, segmented controls).
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }
}

// AGP 9 exposes custom BuildConfig fields through the variant API.
androidComponents {
    onVariants { variant ->
        variant.buildConfigFields?.put(
            "YOUTUBE_API_KEY",
            BuildConfigField(
                type = "String",
                value = "\"${youtubeApiKey.replace("\"", "\\\"")}\"",
                comment = "Optional YouTube Data API v3 key; empty when not configured.",
            ),
        )
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.common)
    implementation(libs.media3.session)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(project(":core"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

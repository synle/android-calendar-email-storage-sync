// =============================================================================
// build.gradle.kts (APP module)
// =============================================================================
// WHY: This file configures how YOUR app is built. It specifies:
//   - Which Android version to target
//   - What libraries (dependencies) your app uses
//   - Kotlin and Compose settings
//
// This is the most important build file — you'll edit it often.
// =============================================================================

plugins {
    // "I am an Android application" (not a library)
    id("com.android.application")
    // "I use Kotlin"
    id("org.jetbrains.kotlin.android")
}

android {
    // -------------------------------------------------------------------------
    // Basic app identity
    // -------------------------------------------------------------------------
    namespace = "com.example.unifiedhub"  // Unique identifier for your app
    compileSdk = 34                        // Which Android SDK to compile against
    // WHY 34? It's Android 14 — the latest stable SDK as of 2024.
    // compileSdk means "I can USE features up to this version"

    defaultConfig {
        applicationId = "com.example.unifiedhub"  // Unique ID on Google Play
        minSdk = 33        // Minimum Android version: Android 13
        targetSdk = 34     // We follow Android 14 behavior rules
        versionCode = 1    // Internal version number (for updates)
        versionName = "1.0" // What users see

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Required for Jetpack Compose to work with string resources
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // -------------------------------------------------------------------------
    // Build types
    // -------------------------------------------------------------------------
    buildTypes {
        release {
            // For now, we won't minify (shrink) the code
            // In production, you'd set this to true for smaller APKs
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Kotlin / Java compatibility
    // -------------------------------------------------------------------------
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // -------------------------------------------------------------------------
    // Enable Jetpack Compose
    // -------------------------------------------------------------------------
    // WHY: Compose is Android's MODERN way to build UI.
    // Instead of XML layout files, you write UI in Kotlin code.
    // It's simpler, more powerful, and the future of Android UI.
    buildFeatures {
        compose = true
    }
    composeOptions {
        // This version must match your Kotlin version
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// =============================================================================
// Dependencies — the libraries our app uses
// =============================================================================
// WHY: No one builds everything from scratch. Libraries give us pre-built
// components for UI, architecture, etc. Think of them as LEGO sets.
// =============================================================================

dependencies {
    // --- Core Android ---
    implementation("androidx.core:core-ktx:1.12.0")           // Kotlin extensions for Android
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Lifecycle-aware components

    // --- Jetpack Compose (our UI toolkit) ---
    implementation(platform("androidx.compose:compose-bom:2024.01.00")) // BOM = Bill of Materials
    // WHY BOM? It ensures all Compose libraries use COMPATIBLE versions.
    // You declare the BOM once, then don't need version numbers below.
    implementation("androidx.compose.ui:ui")                    // Core Compose UI
    implementation("androidx.compose.ui:ui-graphics")           // Drawing/graphics
    implementation("androidx.compose.ui:ui-tooling-preview")    // Preview in Android Studio
    implementation("androidx.compose.material3:material3")      // Material Design 3 components
    implementation("androidx.compose.material:material-icons-extended") // Extra icons
    implementation("androidx.activity:activity-compose:1.8.2")  // Compose + Activity bridge

    // --- ViewModel (architecture) ---
    // WHY: ViewModel survives screen rotation. Without it, your data
    // would be lost every time the user rotates their phone!
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // --- Accompanist (permissions helper for Compose) ---
    // WHY: Asking for permissions in Compose is tricky. This library
    // gives us simple composable functions to handle it.
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // --- WorkManager (for scheduled tasks like daily digest) ---
    // WHY: If you want to run code at a specific time (like generating
    // a daily digest every morning), WorkManager is the right tool.
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // --- Debug tools ---
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

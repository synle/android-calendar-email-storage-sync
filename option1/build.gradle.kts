// =============================================================================
// build.gradle.kts (ROOT / Project-level)
// =============================================================================
// WHY: This is the TOP-LEVEL build file. It declares plugins that ALL modules
// in the project MIGHT use, but doesn't apply them here — each module
// decides which plugins it needs.
//
// Think of this as "registering tools in the workshop" — you put them on the
// shelf, but each worker (module) picks up only what they need.
// =============================================================================

plugins {
    // The Android Application plugin — needed to build an Android app
    id("com.android.application") version "8.2.2" apply false

    // Kotlin for Android
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

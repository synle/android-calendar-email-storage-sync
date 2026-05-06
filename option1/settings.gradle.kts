// =============================================================================
// settings.gradle.kts — The "entry point" for Gradle
// =============================================================================
// WHY: This file tells Gradle:
//   1. WHERE to find libraries (repositories)
//   2. WHAT modules make up our project
//
// Think of it as the TABLE OF CONTENTS for your project.
// =============================================================================

pluginManagement {
    // WHERE should Gradle look for build plugins?
    repositories {
        google()           // Google's repo — has Android-specific plugins
        mavenCentral()     // The main Java/Kotlin library repository
        gradlePluginPortal() // Gradle's own plugin repository
    }
}

dependencyResolutionManagement {
    // WHERE should Gradle look for libraries our app uses?
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// The name of our project
rootProject.name = "UnifiedHub"

// Our project has ONE module called "app"
// (Bigger projects might have multiple modules like :app, :network, :database)
include(":app")

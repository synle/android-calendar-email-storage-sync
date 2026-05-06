# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Unified Hub is a native Android app (Kotlin) that aggregates Calendar events, SMS messages, Call logs, and Emails into a single timeline screen with filtering and a daily digest feature. Email data is currently mocked — real integration would require Gmail API/OAuth setup.

## Build & Run

This is a Gradle-based Android project using Kotlin DSL. Open in Android Studio and sync Gradle.

- **Build:** `./gradlew assembleDebug`
- **Install on device:** `./gradlew installDebug`
- **Clean:** `./gradlew clean`
- **No tests exist yet.** The instrumentation runner is configured (`AndroidJUnitRunner`) but no test files are written.

Target: Android 13+ (minSdk 33, compileSdk/targetSdk 34).

## Architecture

MVVM with Repository pattern. Data flows one direction:

```
MainActivity (permissions, routing, share intent)
    → TimelineViewModel (StateFlow-based UI state, filtering, digest generation)
        → TimelineRepository (aggregates readers, respects granted permissions)
            → CalendarReader   (CalendarContract ContentProvider)
            → SmsReader        (Telephony.Sms ContentProvider)
            → CallLogReader    (CallLog.Calls ContentProvider)
            → EmailReader      (mock data — no real API)
```

**Key architectural decisions:**
- All data sources are normalized into `TimelineItem` (data/model/) with an `ItemType` enum before reaching the UI
- Repository checks which permissions are granted and only queries allowed providers — partial permission grants are handled gracefully
- ViewModel exposes a single `TimelineUiState` data class via `StateFlow`; the UI never mutates state directly
- ViewModel requires `ContentResolver`, injected via `TimelineViewModelFactory`
- UI is 100% Jetpack Compose (Material3 + dynamic colors on Android 12+), no XML layouts

## Key Conventions

- **Readers** accept a `ContentResolver` (not Context/Activity) to stay decoupled from Android lifecycle
- **Permissions** are declared in AndroidManifest.xml AND requested at runtime via `ActivityResultContracts.RequestMultiplePermissions` in MainActivity
- The `requiredPermissions` array is defined in `PermissionScreen.kt` and shared across the app
- Compose theme uses dynamic color scheme (wallpaper-based) on Android 12+, with manual light/dark fallbacks

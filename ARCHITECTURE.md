# android-calendar-email-storage-sync — Architecture

## High-Level Overview

Native Android app (Kotlin + Jetpack Compose, branded "Unified Hub") that aggregates **Calendar**, **Email**, **SMS**, and **Call Log** activity into a unified, searchable timeline. All data processing is on-device — no backend.

The repo contains **two independent Gradle projects** in side-by-side directories. Both target the same product but explore different architectural approaches:

| Module | Stack | Connection model |
|--------|-------|------------------|
| `option1/` | MVVM + Kotlin coroutines + `StateFlow`. Single-module Compose app. Calendar, SMS, and Call Log come from Android `ContentResolver`; email is currently mocked. | Readers (`CalendarReader`, `SmsReader`, `CallLogReader`, `EmailReader`) feed a `TimelineRepository`, which the `TimelineViewModel` exposes as `StateFlow` to Compose screens. **Primary** — built and released in CI. |
| `option2/` | Hilt DI + Room persistence + WorkManager scheduling + KSP. Heavier, layered data architecture. | Per-source `Repository` classes (Calendar/Email/Sms/CallLog) plus a `UnifiedTimelineRepository` write into a Room database (`UnifiedHubDatabase`). A `DigestWorker` scheduled by `DigestScheduler` produces periodic daily digests. Includes real Gmail (`GmailDataSource`) and IMAP (`ImapDataSource`) wiring. Reference / WIP — not built in CI. |

Both modules talk to Calendar/SMS/Call Log via Android system content providers (using `READ_CALENDAR`, `READ_SMS`, `READ_CALL_LOG`, `READ_CONTACTS` dangerous permissions requested at runtime). option2 additionally uses `INTERNET`, `POST_NOTIFICATIONS`, and `SCHEDULE_EXACT_ALARM` for Gmail/IMAP fetch and digest scheduling. Storage is in-memory in option1 and Room (SQLite) in option2.

## Key Directories

```
.
├── option1/                                  # Primary implementation (built in CI)
│   ├── settings.gradle.kts                   # Includes :app
│   ├── build.gradle.kts                      # Root build script
│   ├── gradle.properties
│   └── app/
│       ├── build.gradle.kts                  # AGP config, deps, compileSdk 34
│       ├── proguard-rules.pro
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── res/
│           └── java/com/example/unifiedhub/
│               ├── MainActivity.kt           # Compose entry point
│               ├── data/
│               │   ├── reader/               # CalendarReader, SmsReader, CallLogReader, EmailReader
│               │   ├── repository/           # TimelineRepository
│               │   ├── model/                # TimelineItem
│               │   └── util/                 # TimelineFilter
│               ├── viewmodel/                # TimelineViewModel
│               └── ui/                       # screens/, components/, theme/
│
├── option2/                                  # Reference implementation (Hilt/Room/WorkManager)
│   ├── settings.gradle.kts                   # Includes :app
│   ├── build.gradle.kts
│   ├── gradle.properties
│   └── app/
│       ├── build.gradle.kts                  # compileSdk 35, Hilt + KSP + Compose plugins
│       ├── proguard-rules.pro
│       └── src/main/
│           ├── AndroidManifest.xml
│           └── java/com/unifiedhub/app/
│               ├── UnifiedHubApp.kt          # @HiltAndroidApp + WorkManager Configuration.Provider
│               ├── MainActivity.kt
│               ├── di/                       # AppModule, WorkManagerModule
│               ├── data/
│               │   ├── repository/           # Calendar/Email/Sms/CallLog + UnifiedTimelineRepository
│               │   ├── local/                # database/, dao/, entity/
│               │   ├── remote/               # gmail/GmailDataSource, imap/ImapDataSource
│               │   └── model/                # TimelineItem, TimelineItemType, DailyDigest
│               ├── worker/                   # DigestWorker, DigestScheduler
│               └── ui/                       # navigation/, screen/{timeline,digest,permissions,filter}, component/, theme/
│
├── .github/workflows/
│   ├── build.yml                             # CI: tests + assembleDebug for option1 (option2 skipped)
│   └── release.yml                           # workflow_dispatch: option1 debug APK → GitHub Release
├── CLAUDE.md
├── DEV.md
├── README.md
└── format.sh
```

Each `option*` folder is a **self-contained Gradle project** with its own `settings.gradle.kts`. There is no top-level Gradle file — you must `cd` into the option directory before running `gradle` commands.

## Important Files

- **`option1/settings.gradle.kts`**, **`option2/settings.gradle.kts`** — Per-option Gradle entry points. Each declares `rootProject.name = "UnifiedHub"` and `include(":app")`. option2 uses `dependencyResolution {}` (Gradle 8.10+) syntax; option1 uses the classic `dependencyResolutionManagement {}` block.
- **`option1/build.gradle.kts`**, **`option2/build.gradle.kts`** — Root build scripts for each option. option2 additionally pulls Hilt, KSP, and Compose plugin aliases from a version catalog.
- **`option1/app/build.gradle.kts`** — `compileSdk = 34`, `minSdk = 33`, `applicationId = "com.example.unifiedhub"`, namespace `com.example.unifiedhub`. Plain `com.android.application` + `org.jetbrains.kotlin.android`.
- **`option2/app/build.gradle.kts`** — `compileSdk = 35`, `minSdk = 33`, namespace `com.unifiedhub.app`. Adds `hilt.android`, `ksp`, `kotlin.compose`; release build enables minification and resource shrinking.
- **`option1/app/src/main/AndroidManifest.xml`** — Declares the four dangerous-permission `<uses-permission>` entries (Calendar/SMS/Call Log/Contacts) and the single `.MainActivity` with `MAIN`/`LAUNCHER` intent filter.
- **`option2/app/src/main/AndroidManifest.xml`** — Same four permissions plus `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`. Declares `.UnifiedHubApp` as the `android:name` Application class and removes the default `androidx.startup` WorkManager initializer so Hilt can provide its own.
- **`option1/.../MainActivity.kt`** — Compose-only entry point; sets the content to the timeline screen, which the `TimelineViewModel` drives via `StateFlow`.
- **`option2/.../UnifiedHubApp.kt`** — `@HiltAndroidApp` Application subclass implementing `Configuration.Provider` to wire WorkManager through Hilt.
- **`option2/.../worker/DigestWorker.kt`** and **`DigestScheduler.kt`** — Background job that builds the daily digest; scheduled via WorkManager.
- **`option2/.../data/local/database/UnifiedHubDatabase.kt`** — Room database holding `TimelineEntity` via `TimelineDao`.
- **`.github/workflows/build.yml`** — On push/PR to `main`/`master` and on `workflow_dispatch`: JDK 17, Gradle 8.5, runs `gradle testDebugUnitTest` then `gradle assembleDebug` in `option1/`, uploads the resulting APK as the `unifiedhub-option1-debug-apk` artifact. option2 is not exercised by CI.
- **`README.md`**, **`DEV.md`** — Product overview and developer workflow notes.

## Build & Release Flow

**CI build (`build.yml`)** — every push and PR against `main`/`master`:
1. Checkout, set up Temurin JDK 17 and Gradle 8.5.
2. `cd option1 && gradle testDebugUnitTest --no-daemon`.
3. `cd option1 && gradle assembleDebug --no-daemon`.
4. Upload `option1/app/build/outputs/apk/debug/*.apk` as a workflow artifact (`if-no-files-found: error`).

**Release (`release.yml`)** — manual `workflow_dispatch` only:
1. Inputs: `tag` (e.g. `v0.1.0`, required) and `notes` (markdown, optional).
2. Checkout, JDK 17, Gradle 8.5.
3. `cd option1 && gradle assembleDebug --no-daemon` (debug APK — release signing is not configured).
4. Stage to `release-apks/unifiedhub-option1-<tag>-debug.apk`.
5. `softprops/action-gh-release@v2` creates a non-draft, non-prerelease GitHub Release at the supplied `tag` and uploads the staged APK.

The workflow requires the dispatcher to pass an explicit `tag` input (default `v0.0.0`); it does **not** derive the tag from `github.ref_name`, so it is safe against the `vmain`-clobber pattern. Only option1 is released; option2 has no release path.

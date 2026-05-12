# Developer Guide

Native Android app (Kotlin + Jetpack Compose, MVVM + StateFlow) that aggregates Email, Calendar, and SMS into one tabbed UI. The repo holds two implementations: `option1/` (primary, built in CI) and `option2/` (Hilt + Room + WorkManager, WIP).

Toolchain: JDK 17, Gradle 8.5, compileSdk 34, minSdk 33.

## Quick Start

The Gradle wrapper jar isn't checked in — generate it once per module:

```bash
# option1 (primary)
cd option1
gradle wrapper --gradle-version 8.5
chmod +x gradlew
./gradlew assembleDebug          # Debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug           # Build + install on connected device/emulator
```

```bash
# option2 (reference / WIP)
cd option2
gradle wrapper --gradle-version 8.11.1
chmod +x gradlew
./gradlew assembleDebug
```

Sideload the CI-built APK:

```bash
# Download `unifiedhub-option1-debug-apk` from the latest green Actions run, then:
unzip unifiedhub-option1-debug-apk.zip
adb install -r app-debug.apk
```

On first launch, tap **Grant Permissions** and approve `READ_CALENDAR`, `READ_SMS`, `READ_CONTACTS`. Email is mocked (no Gmail OAuth yet).

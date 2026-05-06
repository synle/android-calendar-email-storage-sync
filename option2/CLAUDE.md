# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Unified Hub** — native Android app (Kotlin, Jetpack Compose) aggregating Email, Calendar, SMS, and Call Logs into a unified timeline with daily digest. Local-only, no backend. See `README.md` for full diagrams and flows.

## Build Commands

Single-module Gradle project. Android SDK 35, minSdk 33.

```bash
./gradlew assembleDebug              # Debug APK
./gradlew assembleRelease            # Release (minified)
./gradlew testDebugUnitTest          # Unit tests
./gradlew connectedDebugAndroidTest  # Instrumented tests
./gradlew kspDebugKotlin             # KSP annotation processing
```

No `gradlew` wrapper checked in yet — generate with `gradle wrapper --gradle-version 8.11.1`.

## Architecture

MVVM + Repository + Room as single source of truth.

**Data flow:** Content Providers → per-source Repositories → `UnifiedTimelineRepository.refreshAll()` (parallel fetch with `runCatching`) → Room `timeline_items` table → Flow → ViewModels → Compose UI.

**Key file:** `data/repository/UnifiedTimelineRepository.kt` — the central hub that orchestrates all data fetching, merging, search, and digest generation.

## Package Layout

- `data/model/` — `TimelineItem`, `TimelineItemType`, `DailyDigest`
- `data/local/` — Room: `UnifiedHubDatabase`, `TimelineEntity`, `TimelineDao`
- `data/remote/` — interfaces: `GmailDataSource`, `ImapDataSource`
- `data/repository/` — `CalendarRepository`, `SmsRepository`, `CallLogRepository`, `EmailRepository`, `UnifiedTimelineRepository`
- `di/` — Hilt: `AppModule`, `WorkManagerModule`
- `ui/screen/` — Compose screens: `timeline/`, `digest/`, `permissions/`, `filter/`
- `ui/component/` — `TimelineItemCard`
- `worker/` — `DigestWorker`, `DigestScheduler`

## Critical Implementation Details

- WorkManager auto-init is **disabled** in manifest; `UnifiedHubApp` implements `Configuration.Provider` with `HiltWorkerFactory`.
- `TimelineEntity.sourceId` has a **unique index** — prevents duplicates on refresh (UPSERT via `OnConflictStrategy.REPLACE`).
- Email sources (`GmailDataSourceImpl`, `ImapDataSourceImpl`) are **stubs returning empty lists** — need OAuth2/credential integration.
- All versions in `gradle/libs.versions.toml`. Uses **KSP** (not kapt) for Room and Hilt.
- ProGuard rules in `app/proguard-rules.pro` for release builds.
- Runtime permissions (`READ_CALENDAR`, `READ_SMS`, `READ_CALL_LOG`, `READ_CONTACTS`, `POST_NOTIFICATIONS`) gate app via `PermissionsScreen`.
- Navigation: `permissions` → `timeline` → `digest`. Permissions screen pops itself from backstack on grant.

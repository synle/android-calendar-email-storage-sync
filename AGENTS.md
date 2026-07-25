# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

**Unified Hub** — a native Android app (Kotlin + Jetpack Compose) that aggregates **Email**, **Calendar**, and **SMS** into a tabbed UI with search, sortable expandable cards, and a daily digest. All data processing is local; no server backend.

Two independent implementations live in this repo:

| Folder | Status | Architecture |
|--------|--------|--------------|
| `option1/` | **Built in CI** — primary | MVVM + StateFlow + ContentResolver readers. Lightweight, single-screen Compose. compileSdk 34. |
| `option2/` | Reference / WIP | Hilt + Room + WorkManager + KSP, Navigation-based. Heavier; needs `gradle wrapper` regen before building. compileSdk 35. |

CI builds **option1**. Each option has its own deeper `CLAUDE.md` documenting internals.

## UI Surface (option1)

The current UI lives in `option1/app/src/main/java/com/example/unifiedhub/`:

- `MainActivity.kt` — permission gating + Compose entry. Routes between `PermissionScreen` and `TimelineScreen`.
- `ui/screens/TimelineScreen.kt` — TabRow (Email / Calendar / SMS) + search field + sort toggle + LazyColumn of cards.
- `ui/components/TimelineItemCard.kt` — accordion card. `rememberSaveable` keeps expansion state across recomposition. Tap header to expand/collapse.
- `viewmodel/TimelineViewModel.kt` — single `TimelineUiState` flow holding `selectedTab`, `searchQuery`, `sortDescending`, `visibleItems`. `recomputeVisible()` does tab-filter → search-filter → sort.

Call logs are read but **not surfaced** in the tabbed UI — they still feed the daily digest.

## Build & Run

option1 (the CI-built variant):

```bash
cd option1
gradle wrapper --gradle-version 8.5    # one-time wrapper jar generation
chmod +x gradlew
./gradlew assembleDebug                 # APK → app/build/outputs/apk/debug/
./gradlew installDebug                  # build + install on connected device
```

option2 (reference; uses Gradle 8.11.1):

```bash
cd option2
gradle wrapper --gradle-version 8.11.1
chmod +x gradlew
./gradlew assembleDebug
```

## Data Flow (option1)

```
MainActivity (permissions, share intent)
    → TimelineViewModel (StateFlow<TimelineUiState>)
        → TimelineRepository (aggregates readers, respects granted permissions)
            → CalendarReader   (CalendarContract ContentProvider)
            → SmsReader        (Telephony.Sms ContentProvider)
            → CallLogReader    (CallLog.Calls ContentProvider)  [feeds digest only]
            → EmailReader      (mock data — no real Gmail API)
```

All sources normalize into `data/model/TimelineItem` with an `ItemType` enum.

## Key Conventions

- UI is 100% Jetpack Compose Material 3, no XML layouts. The single XML resource is `themes.xml`'s base activity theme.
- Readers accept a `ContentResolver` (not `Context`/`Activity`) so they're decoupled from Android lifecycle.
- Permissions are declared in `AndroidManifest.xml` AND requested at runtime via `ActivityResultContracts.RequestMultiplePermissions`. Partial grants are handled gracefully — repository skips sources without permission.
- `requiredPermissions` list lives in `ui/screens/PermissionScreen.kt`.
- Card expansion state uses `rememberSaveable(item.id)` — survives recomposition AND scroll-off, keyed per item id.
- Compose BOM is `2024.01.00` (Material3 1.2.0-alpha) — `Divider` is the right divider component, not `HorizontalDivider`.

## Permissions

Declared in `option1/app/src/main/AndroidManifest.xml`:

- `READ_CALENDAR`
- `READ_SMS`
- `READ_CONTACTS`
- `READ_CALL_LOG` (declared but UI hides call items)

Email reading is currently **mocked** — see `EmailReader.kt`. Hooking up Gmail API requires OAuth setup, out of scope for the MVP.

## CI

`.github/workflows/build.yml` — runs on push to `main`/`master`, on PRs, and on `workflow_dispatch`. Builds option1 with JDK 17 + Gradle 8.5 and uploads `app-debug.apk` as the `unifiedhub-option1-debug-apk` artifact. option2 is **not** built in CI yet — its Hilt/Room/KSP setup needs more wiring.

See `dev.md` for sideload instructions (download artifact → `adb install`).

## Things to Watch

- **XML files require the `<?xml ?>` declaration on line 1.** Comments before the declaration cause AAPT manifest parse failures (was the cause of an early CI break).
- **No launcher icon resources are checked in** — `AndroidManifest.xml` deliberately omits `android:icon` / `android:roundIcon`. Adding them later requires placing PNGs in `mipmap-*` density folders.
- The wrapper jar is **not** in either option's `gradle/wrapper/` — local builds must run `gradle wrapper ...` first. CI sidesteps this by using `gradle/actions/setup-gradle` with an explicit version.

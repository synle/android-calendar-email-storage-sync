# Unified Hub

Native Android app (Kotlin + Jetpack Compose) that aggregates **Email**, **Calendar**, and **SMS** into a tabbed UI with search, sortable expandable cards, and a daily digest. All data processing is local — no server.

> The repo holds **two independent implementations** of the same idea. Pick the one that matches your taste:
>
> | Folder | Status | Architecture |
> |--------|--------|--------------|
> | [`option1/`](option1/) | **Built in CI** — primary | MVVM + StateFlow + ContentResolver readers. Lightweight, single-screen Compose. |
> | [`option2/`](option2/) | Reference / WIP | Hilt + Room + WorkManager + KSP. Heavier; needs `gradle wrapper` regen before building. |

CI builds **option1** by default. See [`dev.md`](dev.md) for build, sideload, and CI artifact instructions.

## What the app does

- **3 tabs**: Email · Calendar · SMS
- **Accordion cards** — collapsed view shows date, sender/location, and subject/title; tap to expand and read full content
- **Search bar** — filters items in the active tab by title, body, or sender
- **Sort toggle** — newest-first ↔ oldest-first
- **Daily Digest** FAB — produces a plaintext summary of today's activity, shareable via Android's share sheet

Email reading is currently **mocked** — wiring up Gmail OAuth is left as future work.

## Tech stack (option1)

- Kotlin · Jetpack Compose Material 3 · ContentResolver
- compileSdk 34, minSdk 33 (Android 13+)
- Gradle 8.5 · JDK 17

option2 adds Hilt, Room, WorkManager, KSP, and a more layered data architecture — see [`option2/README.md`](option2/README.md) for its design.

## Permissions

Declared in `option1/app/src/main/AndroidManifest.xml`, requested at runtime:

- `READ_CALENDAR`
- `READ_SMS`
- `READ_CONTACTS`
- `READ_CALL_LOG` (declared but not surfaced in the tabbed UI)

## Build / install

See [`dev.md`](dev.md) for the full toolchain table, local build, and how to download + sideload the CI APK artifact.

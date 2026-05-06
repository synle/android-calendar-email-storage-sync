# Developer Guide — Unified Hub

Native Android app (Kotlin + Jetpack Compose) that aggregates Email, Calendar, and SMS into a single tabbed UI with search and sortable, expandable items.

This repo contains **two independent implementations**:

| Folder | Status | Architecture |
|--------|--------|--------------|
| `option1/` | **Built in CI** — primary target | MVVM + StateFlow + ContentResolver readers. Lightweight. |
| `option2/` | Reference / WIP | Hilt + Room + WorkManager + KSP. More complex; needs `gradle wrapper` regeneration before building. |

The CI workflow builds **option1**.

## Toolchain (option1)

| Tool | Version |
|------|---------|
| JDK | 17 (Temurin recommended) |
| Gradle | 8.5 (CI auto-installs) |
| Compile SDK | 34 (Android 14) |
| Min SDK | 33 (Android 13) |
| Kotlin Compose Compiler | 1.5.8 |

## Local build

The wrapper jar isn't checked in. Generate it first:

```bash
cd option1
gradle wrapper --gradle-version 8.5
chmod +x gradlew
./gradlew assembleDebug          # Debug APK → app/build/outputs/apk/debug/
./gradlew installDebug           # Build + install on connected device
```

For `option2`:

```bash
cd option2
gradle wrapper --gradle-version 8.11.1
chmod +x gradlew
./gradlew assembleDebug
```

## Install on phone (sideload)

1. Open the [Actions tab](../../actions), pick the latest successful **Build APK** run.
2. Download `unifiedhub-option1-debug-apk` from Artifacts.
3. Unzip → `app-debug.apk`.
4. **ADB**: `adb install -r app-debug.apk` — or copy the APK to the phone and open it from Files.
5. Launch **Unified Hub**, tap **Grant Permissions**, and approve `READ_CALENDAR`, `READ_SMS`, `READ_CONTACTS`. (Email is mocked — no Gmail OAuth needed yet.)

## What the app does

- **3 tabs**: Email · Calendar · SMS
- Each item is an **accordion card**. Tap to expand and see full content; collapsed view shows date, from/to or location, and subject/title.
- **Search bar** at the top — filters items by title or content within the active tab.
- **Sort toggle** — newest-first (default) ↔ oldest-first, by item timestamp.
- **Daily Digest** FAB — generates a plaintext summary of today's items, shareable via Android's share sheet.

## Permissions

Declared in `option1/app/src/main/AndroidManifest.xml` and requested at runtime via `ActivityResultContracts.RequestMultiplePermissions`:

- `READ_CALENDAR`
- `READ_SMS`
- `READ_CONTACTS`
- `READ_CALL_LOG` (declared but not surfaced in the tabbed UI)

Email reading is currently **mocked** — see `EmailReader.kt`. Hooking up Gmail API requires OAuth setup (out of scope for the MVP).

## CI

`.github/workflows/build.yml` runs on push to `main`/`master`, on PRs, and on manual dispatch. It builds `option1` and uploads the debug APK as `unifiedhub-option1-debug-apk`.

# Unified Hub

A native Android application that aggregates Email, Calendar, SMS, and Call Logs into a single unified timeline view with daily digest generation. All data processing is local-only — no server backend.

**Target:** Android 13+ (API 33) | **Language:** Kotlin | **UI:** Jetpack Compose + Material 3

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Application Boot Sequence](#application-boot-sequence)
- [Screen Navigation Flow](#screen-navigation-flow)
- [Data Flow Pipeline](#data-flow-pipeline)
- [Code Structure](#code-structure)
- [File Map — What Lives Where](#file-map--what-lives-where)
- [Entry Points](#entry-points)
- [Data Model](#data-model)
- [Repository Layer — How Each Source Works](#repository-layer--how-each-source-works)
- [Room Database Schema](#room-database-schema)
- [Dependency Injection Graph](#dependency-injection-graph)
- [Daily Digest Workflow](#daily-digest-workflow)
- [WorkManager Background Jobs](#workmanager-background-jobs)
- [Permission Flow](#permission-flow)
- [UI Screens](#ui-screens)
- [Build & Run](#build--run)
- [Tech Stack & Versions](#tech-stack--versions)
- [Stub Status — What Needs Implementation](#stub-status--what-needs-implementation)
- [Security & Privacy](#security--privacy)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                        │
│                                                                  │
│  MainActivity                                                    │
│      └── UnifiedHubNavGraph                                      │
│              ├── PermissionsScreen  ←→  PermissionsViewModel     │
│              ├── TimelineScreen     ←→  TimelineViewModel        │
│              │       └── FilterBottomSheet                       │
│              └── DigestScreen       ←→  DigestViewModel          │
│                                                                  │
├──────────────────────── StateFlow / Flow ─────────────────────────┤
│                                                                  │
│                        DOMAIN / REPOSITORY LAYER                 │
│                                                                  │
│              UnifiedTimelineRepository                           │
│              ├── CalendarRepository    (CalendarContract)        │
│              ├── SmsRepository         (Telephony Provider)      │
│              ├── CallLogRepository     (CallLog Provider)        │
│              └── EmailRepository                                 │
│                      ├── GmailDataSource   (Google API + OAuth2) │
│                      └── ImapDataSource    (Angus/Jakarta Mail)  │
│                                                                  │
├──────────────────────── Room / SQLite ────────────────────────────┤
│                                                                  │
│                        PERSISTENCE LAYER                         │
│                                                                  │
│              UnifiedHubDatabase                                  │
│              └── TimelineDao                                     │
│                      └── timeline_items (table)                  │
│                                                                  │
├──────────────────────── WorkManager ──────────────────────────────┤
│                                                                  │
│                        BACKGROUND LAYER                          │
│                                                                  │
│              DigestScheduler  →  DigestWorker                    │
│              (schedules 24h periodic)  (refresh + digest + prune)│
│                                                                  │
├──────────────────────── Hilt DI ──────────────────────────────────┤
│                                                                  │
│              AppModule          (DB, DAO, ContentResolver, etc.) │
│              WorkManagerModule  (WorkManager config)             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Application Boot Sequence

```
Android System
    │
    ▼
UnifiedHubApp (@HiltAndroidApp)
    │  • Initializes Hilt dependency graph
    │  • Provides custom WorkManager Configuration
    │  • Injects HiltWorkerFactory
    │
    ▼
MainActivity (@AndroidEntryPoint)
    │  • enableEdgeToEdge()
    │  • setContent { UnifiedHubTheme { UnifiedHubNavGraph() } }
    │
    ▼
UnifiedHubNavGraph
    │  • startDestination = "permissions"
    │
    ├── If permissions NOT granted ──→ PermissionsScreen
    │                                      │
    │                                      │ (user grants)
    │                                      ▼
    └── If permissions granted ──────→ TimelineScreen
                                           │
                                           │ (on init)
                                           ▼
                                      TimelineViewModel.refresh()
                                           │
                                           ▼
                                      UnifiedTimelineRepository.refreshAll()
                                           │
                                      ┌────┴────┬─────────┬──────────┐
                                      ▼         ▼         ▼          ▼
                                  Calendar    SMS     CallLog     Email
                                  Provider  Provider  Provider    (stub)
                                      │         │         │          │
                                      └────┬────┘─────────┘──────────┘
                                           ▼
                                      Room DB (timeline_items)
                                           │
                                           ▼
                                      UI observes via Flow
```

---

## Screen Navigation Flow

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│                 │  grant   │                 │  tap     │                 │
│   Permissions   │────────→│    Timeline     │────────→│  Daily Digest   │
│   Screen        │ all     │    Screen       │ digest   │  Screen         │
│                 │ perms   │                 │ icon     │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
                                    │                           │
                            popUpTo(permissions,        popBackStack()
                            inclusive = true)                    │
                                    │                           │
                                    │    ┌──────────────┐       │
                                    └───→│ Filter Sheet │       │
                                    tap  │ (Modal       │       │
                                  filter │  BottomSheet)│       │
                                    icon └──────────────┘       │
                                                                │
                                    ←───────────────────────────┘
                                              back
```

**Routes defined in** `ui/navigation/NavGraph.kt`:
- `"permissions"` — gate screen, always first
- `"timeline"` — main content, replaces permissions in backstack
- `"digest"` — pushed on top of timeline

---

## Data Flow Pipeline

```
                         ┌─────────────────────────┐
                         │  User pulls to refresh   │
                         │  or app init triggers    │
                         │  TimelineViewModel       │
                         └────────────┬────────────┘
                                      │
                              refresh()
                                      │
                                      ▼
                    ┌─────────────────────────────────────┐
                    │  UnifiedTimelineRepository           │
                    │  .refreshAll(sinceMillis, sources)   │
                    └────────────────┬────────────────────┘
                                     │
                          coroutineScope { }
                         ┌───────────┼───────────┐──────────┐
                         │           │           │          │
                   async {     async {     async {    async {
                   runCatching  runCatching runCatching runCatching
                         │           │           │          │
                         ▼           ▼           ▼          ▼
                    ┌────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐
                    │Calendar│ │  SMS    │ │CallLog  │ │ Email  │
                    │  Repo  │ │  Repo   │ │  Repo   │ │  Repo  │
                    └───┬────┘ └────┬────┘ └────┬────┘ └───┬────┘
                        │          │           │          │
                  ContentResolver  ContentResolver    GmailDataSource
                  .query(          .query(             ImapDataSource
                  CalendarContract  Telephony          (stubs)
                  .Instances)       .Sms)
                  ContentResolver
                  .query(CallLog)
                        │          │           │          │
                        ▼          ▼           ▼          ▼
                    List<TimelineItem>  (each mapped to unified model)
                         │           │           │          │
                         └───────────┼───────────┘──────────┘
                                     │
                              merge all into
                              mutableListOf
                                     │
                                     ▼
                    ┌─────────────────────────────────────┐
                    │  timelineDao.insertAll(entities)     │
                    │  (UPSERT via OnConflictStrategy      │
                    │   .REPLACE on sourceId unique index) │
                    └────────────────┬────────────────────┘
                                     │
                              Room notifies
                              Flow observers
                                     │
                                     ▼
                    ┌─────────────────────────────────────┐
                    │  timelineDao.getTimelineItems(types) │
                    │  returns Flow<List<TimelineEntity>>  │
                    └────────────────┬────────────────────┘
                                     │
                              .map { toDomain() }
                                     │
                                     ▼
                    ┌─────────────────────────────────────┐
                    │  TimelineViewModel                   │
                    │  combine(filters, search, items)     │
                    │  groupBy(LocalDate)                  │
                    │  → TimelineUiState                   │
                    └────────────────┬────────────────────┘
                                     │
                              collectAsStateWithLifecycle()
                                     │
                                     ▼
                    ┌─────────────────────────────────────┐
                    │  TimelineScreen (Compose)            │
                    │  LazyColumn with day headers         │
                    │  + TimelineItemCard per item         │
                    └─────────────────────────────────────┘
```

---

## Code Structure

```
option2/
├── build.gradle.kts                 # Root build (plugin declarations only)
├── settings.gradle.kts              # Project name "UnifiedHub", includes :app
├── gradle.properties                # JVM args, AndroidX, non-transitive R
├── gradle/
│   ├── libs.versions.toml           # Version catalog (all dependency versions)
│   └── wrapper/
│       └── gradle-wrapper.properties
│
└── app/
    ├── build.gradle.kts             # App module build config
    ├── proguard-rules.pro           # ProGuard rules for release
    └── src/main/
        ├── AndroidManifest.xml      # Permissions, Application, Activity
        ├── res/values/
        │   ├── strings.xml
        │   └── themes.xml
        └── java/com/unifiedhub/app/
            │
            ├── UnifiedHubApp.kt             # APPLICATION ENTRY POINT
            ├── MainActivity.kt              # ACTIVITY ENTRY POINT
            │
            ├── data/                        # ── DATA LAYER ──
            │   ├── model/                   # Domain models
            │   │   ├── TimelineItemType.kt  #   enum: EMAIL, CALENDAR_EVENT, SMS, CALL_LOG
            │   │   ├── TimelineItem.kt      #   unified domain model (all sources map here)
            │   │   └── DailyDigest.kt       #   digest model + toFormattedText()
            │   │
            │   ├── local/                   # Room persistence
            │   │   ├── database/
            │   │   │   └── UnifiedHubDatabase.kt  # @Database, version 1
            │   │   ├── entity/
            │   │   │   └── TimelineEntity.kt      # Room entity + toDomain()/fromDomain()
            │   │   └── dao/
            │   │       └── TimelineDao.kt         # All queries: timeline, range, search, count
            │   │
            │   ├── remote/                  # External data source interfaces
            │   │   ├── gmail/
            │   │   │   └── GmailDataSource.kt     # Interface for Gmail API
            │   │   └── imap/
            │   │       └── ImapDataSource.kt      # Interface for IMAP
            │   │
            │   └── repository/              # Business logic per source
            │       ├── CalendarRepository.kt      # CalendarContract.Instances queries
            │       ├── SmsRepository.kt           # Telephony.Sms queries
            │       ├── CallLogRepository.kt       # CallLog.Calls queries
            │       ├── EmailRepository.kt         # Gmail + IMAP aggregation (+ stub impls)
            │       └── UnifiedTimelineRepository.kt  # ★ CENTRAL HUB: parallel fetch, merge, digest
            │
            ├── di/                          # ── DEPENDENCY INJECTION ──
            │   ├── AppModule.kt             # Provides: ContentResolver, Room DB, DAO, DataSources
            │   └── WorkManagerModule.kt     # Provides: WorkManager Configuration
            │
            ├── ui/                          # ── PRESENTATION LAYER ──
            │   ├── theme/
            │   │   └── Theme.kt             # Material 3 theme + dynamic color
            │   ├── navigation/
            │   │   └── NavGraph.kt          # Routes + NavHost (permissions → timeline → digest)
            │   ├── component/
            │   │   └── TimelineItemCard.kt  # Reusable card composable (icon, title, contact, preview)
            │   └── screen/
            │       ├── permissions/
            │       │   ├── PermissionsScreen.kt    # Permission request UI
            │       │   └── PermissionsViewModel.kt # Tracks grant state per permission
            │       ├── timeline/
            │       │   ├── TimelineScreen.kt       # Main screen: pull-to-refresh, day groups, LazyColumn
            │       │   └── TimelineViewModel.kt    # Reactive filter/search, refresh orchestration
            │       ├── filter/
            │       │   └── FilterBottomSheet.kt    # Toggle Email/Calendar/SMS/Call filter chips
            │       └── digest/
            │           ├── DigestScreen.kt         # Summary cards, copy/share/schedule buttons
            │           └── DigestViewModel.kt      # Digest generation, clipboard, share intent
            │
            └── worker/                      # ── BACKGROUND JOBS ──
                ├── DigestWorker.kt          # CoroutineWorker: refresh → digest → notify → prune
                └── DigestScheduler.kt       # Schedule/cancel 24h periodic work
```

---

## File Map — What Lives Where

### "I want to change how data is fetched from the device"
Start at `data/repository/`. Each source has its own repository:
- **Calendar:** `CalendarRepository.kt` — queries `CalendarContract.Instances`
- **SMS:** `SmsRepository.kt` — queries `Telephony.Sms.CONTENT_URI`
- **Calls:** `CallLogRepository.kt` — queries `CallLog.Calls.CONTENT_URI`
- **Email:** `EmailRepository.kt` — delegates to `GmailDataSource` and `ImapDataSource`

### "I want to change the unified data model"
- Domain model: `data/model/TimelineItem.kt`
- Room entity (mirrors domain): `data/local/entity/TimelineEntity.kt`
- Enum of types: `data/model/TimelineItemType.kt`

### "I want to change how sources are merged together"
- `data/repository/UnifiedTimelineRepository.kt` — the `refreshAll()` method

### "I want to change the database queries"
- `data/local/dao/TimelineDao.kt` — all Room `@Query` methods

### "I want to add a new screen"
1. Create screen composable in `ui/screen/<feature>/`
2. Create ViewModel in same package
3. Add route in `ui/navigation/NavGraph.kt`

### "I want to change the filter options"
- UI: `ui/screen/filter/FilterBottomSheet.kt`
- Logic: `TimelineViewModel.toggleFilter()` → Room query changes via `Flow`

### "I want to change what the daily digest looks like"
- Text format: `data/model/DailyDigest.toFormattedText()`
- UI: `ui/screen/digest/DigestScreen.kt`
- Data: `UnifiedTimelineRepository.generateDailyDigest()`

### "I want to change the background job schedule"
- `worker/DigestScheduler.kt` — controls timing
- `worker/DigestWorker.kt` — controls what runs

### "I want to add a new dependency"
- `gradle/libs.versions.toml` — add version + library entry
- `app/build.gradle.kts` — add `implementation(libs.your.library)`

---

## Entry Points

| Entry Point | File | Role |
|---|---|---|
| **Application** | `UnifiedHubApp.kt` | Hilt init, WorkManager config. First thing Android instantiates. |
| **Activity** | `MainActivity.kt` | Single activity. Sets up Compose + edge-to-edge + nav graph. |
| **Nav Graph** | `ui/navigation/NavGraph.kt` | Defines all screens and routes. Start destination: `"permissions"`. |
| **Background** | `worker/DigestWorker.kt` | WorkManager entry. Runs refresh + digest + notification + cache prune. |
| **DI Root** | `di/AppModule.kt` | All singleton providers. This is where the object graph is wired. |

---

## Data Model

### TimelineItem (Domain)
```
TimelineItem
├── id: String              # Unique ID (prefixed: "cal_", "sms_", "call_")
├── type: TimelineItemType  # EMAIL | CALENDAR_EVENT | SMS | CALL_LOG
├── title: String           # "Missed call", "Received message", event title, email subject
├── contact: String         # Phone number, email address, or resolved name
├── timestamp: Instant      # When it happened (UTC millis internally)
├── preview: String         # Snippet: SMS body, call duration, event time range
├── isRead: Boolean         # false for missed calls, unread SMS
├── sourceId: String        # Dedup key (unique index in Room)
└── metadata: Map           # Extra fields (e.g. phone number, call type)
```

### TimelineEntity (Room)
Same fields as `TimelineItem` but with `Long` timestamp (epoch millis) and `String` type (enum name). Bidirectional mapping via `toDomain()` and `fromDomain()`.

### DailyDigest
```
DailyDigest
├── date: LocalDate
├── emailCount, smsCount, missedCallCount, calendarEventCount
├── topEmails: List<TimelineItem>        (max 5)
├── topMessages: List<TimelineItem>      (max 5)
├── missedCalls: List<TimelineItem>
├── upcomingEvents: List<TimelineItem>
└── toFormattedText(): String            (structured text blob)
```

### ID Conventions
| Source | ID Format | Example |
|---|---|---|
| Calendar | `cal_{eventId}_{beginMillis}` | `cal_42_1709136000000` |
| SMS | `sms_{smsId}` | `sms_1847` |
| Call Log | `call_{callId}` | `call_293` |
| Email (Gmail) | `gmail_{messageId}` | `gmail_18d4f2a...` |
| Email (IMAP) | `imap_{uid}_{folder}` | `imap_5421_INBOX` |

---

## Repository Layer — How Each Source Works

### CalendarRepository
```
CalendarContract.Instances.CONTENT_URI
    + appendPath(startMillis)
    + appendPath(endMillis)

Projection: EVENT_ID, TITLE, BEGIN, END, EVENT_LOCATION, DESCRIPTION, ALL_DAY, ORGANIZER, CALENDAR_DISPLAY_NAME
Sort: BEGIN ASC
Maps to: TimelineItem with type=CALENDAR_EVENT
```

### SmsRepository
```
Telephony.Sms.CONTENT_URI

Selection: DATE >= ?
Projection: _ID, ADDRESS, BODY, DATE, TYPE, READ
Sort: DATE DESC LIMIT 200
Maps to: TimelineItem with type=SMS
Title derived from TYPE (Received/Sent/Draft)
```

### CallLogRepository
```
CallLog.Calls.CONTENT_URI

Selection: DATE >= ?
Projection: _ID, NUMBER, CACHED_NAME, DATE, DURATION, TYPE
Sort: DATE DESC LIMIT 200
Maps to: TimelineItem with type=CALL_LOG
Title derived from TYPE (Incoming/Outgoing/Missed/Rejected/Blocked)
isRead = false for MISSED_TYPE
```

### EmailRepository
```
Delegates to two interfaces in parallel:
├── GmailDataSource  (OAuth2 + Google API) — currently returns emptyList()
└── ImapDataSource   (Angus Mail / Jakarta) — currently returns emptyList()

Results merged and sorted by timestamp DESC
```

### UnifiedTimelineRepository (the hub)
```
refreshAll():
1. Launch 4 async coroutines (one per source), each wrapped in runCatching
2. Await all, collect non-null results into merged list
3. Map to TimelineEntity
4. insertAll() into Room (UPSERT)

observeTimeline():  Room Flow → filter by type → map to domain
search():           Room Flow → LIKE query on title/contact/preview
generateDailyDigest(): One-shot Room query → group by type → DailyDigest
```

---

## Room Database Schema

**Database:** `unified_hub.db` (version 1)

**Table:** `timeline_items`

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT | PRIMARY KEY |
| `type` | TEXT | "EMAIL", "CALENDAR_EVENT", "SMS", "CALL_LOG" |
| `title` | TEXT | |
| `contact` | TEXT | |
| `timestamp` | INTEGER | Epoch milliseconds |
| `preview` | TEXT | |
| `isRead` | INTEGER | 0 or 1 |
| `sourceId` | TEXT | UNIQUE INDEX (dedup) |

**Indices:**
- `index_timeline_items_timestamp` — chronological queries
- `index_timeline_items_type` — type filtering
- `index_timeline_items_sourceId` (UNIQUE) — prevents duplicates on refresh

---

## Dependency Injection Graph

```
AppModule (@SingletonComponent)
│
├── provideContentResolver()     → ContentResolver
│       Used by: CalendarRepository, SmsRepository, CallLogRepository
│
├── provideDatabase()            → UnifiedHubDatabase (Room)
│       └── provideTimelineDao() → TimelineDao
│               Used by: UnifiedTimelineRepository
│
├── provideGmailDataSource()     → GmailDataSourceImpl
│       Used by: EmailRepository
│
└── provideImapDataSource()      → ImapDataSourceImpl
        Used by: EmailRepository


Repositories (all @Singleton, @Inject constructor):
├── CalendarRepository(ContentResolver)
├── SmsRepository(ContentResolver)
├── CallLogRepository(ContentResolver)
├── EmailRepository(GmailDataSource, ImapDataSource)
└── UnifiedTimelineRepository(Calendar, Sms, CallLog, Email, TimelineDao)


ViewModels (@HiltViewModel):
├── TimelineViewModel(UnifiedTimelineRepository)
├── DigestViewModel(UnifiedTimelineRepository, DigestScheduler, Context)
└── PermissionsViewModel(Context)


WorkManagerModule (@SingletonComponent):
├── provideWorkManagerConfiguration(HiltWorkerFactory) → Configuration
└── provideWorkManager(Context) → WorkManager
        Used by: DigestScheduler
```

---

## Daily Digest Workflow

```
┌──────────────┐     ┌──────────────────────────┐
│ User taps    │     │ WorkManager fires         │
│ "Digest"     │     │ DigestWorker at           │
│ nav icon     │     │ scheduled time            │
└──────┬───────┘     └────────────┬──────────────┘
       │                          │
       ▼                          ▼
  DigestViewModel          DigestWorker.doWork()
  .generateDigest()              │
       │                    ┌────┴────┐
       │                    │ refresh │ ← fetch last 24h from all sources
       │                    │ All()   │
       │                    └────┬────┘
       │                         │
       ▼                         ▼
  UnifiedTimelineRepository.generateDailyDigest(date)
       │
       │  1. Query Room for date range (start-of-day to end-of-day)
       │  2. Group items by type
       │  3. Count: emails, sms, missed calls, events
       │  4. Build DailyDigest object
       │
       ▼
  DailyDigest
       │
       ├──→ .toFormattedText()  →  Structured text blob
       │         │
       │         ├──→ Copy to clipboard  (ClipboardManager)
       │         ├──→ Share via Intent   (ACTION_SEND, text/plain)
       │         └──→ Share via email    (ACTION_SEND + EXTRA_SUBJECT)
       │
       └──→ (from worker) showDigestNotification()
                    │
                    └──→ NotificationChannel "daily_digest"
                         NotificationCompat with summary counts

  After digest:
       │
       └──→ pruneCache(90 days)  ← delete old entries from Room
```

**Digest text output format:**
```
═══════════════════════════════════
  DAILY DIGEST — 2026-02-28
═══════════════════════════════════

  Summary
  - 12 emails received
  - 8 messages received
  - 2 missed calls
  - 5 calendar events

  Calendar Events
  - Team standup
    09:00 - 09:15 · Zoom
  ...

  Recent Emails
  - Q4 Report Final
    From: boss@company.com
  ...
```

---

## WorkManager Background Jobs

```
DigestScheduler.scheduleDailyDigest(targetTime: LocalTime)
       │
       │  Calculate initialDelay:
       │    targetDateTime = today @ targetTime (or tomorrow if past)
       │    delay = Duration.between(now, targetDateTime)
       │
       ▼
  PeriodicWorkRequestBuilder<DigestWorker>(24 hours)
       .setInitialDelay(delay)
       │
       ▼
  workManager.enqueueUniquePeriodicWork(
      "daily_digest_worker",
      ExistingPeriodicWorkPolicy.UPDATE
  )
```

**DigestWorker execution:**
1. Refresh all sources (last 24h)
2. Generate daily digest
3. Show notification
4. Prune cache (entries > 90 days old)
5. Retry up to 3 times on failure

**WorkManager initialization:**
- Auto-init is **disabled** in AndroidManifest.xml (removed `WorkManagerInitializer`)
- `UnifiedHubApp` implements `Configuration.Provider`
- Injects `HiltWorkerFactory` for DI support in workers

---

## Permission Flow

```
App Launch
    │
    ▼
PermissionsViewModel.refreshPermissionStates()
    │
    │  Check each permission via ContextCompat.checkSelfPermission():
    │  ├── READ_CALENDAR
    │  ├── READ_SMS
    │  ├── READ_CALL_LOG
    │  ├── READ_CONTACTS
    │  └── POST_NOTIFICATIONS
    │
    ├── All granted? ──→ Navigate to Timeline (pop Permissions from backstack)
    │
    └── Some missing? ──→ Show PermissionsScreen
                               │
                               │ User taps "Grant Permissions"
                               │
                               ▼
                          ActivityResultContracts
                          .RequestMultiplePermissions()
                               │
                               │ System dialog shown
                               │
                               ▼
                          Callback: refreshPermissionStates()
                               │
                               ├── All granted ──→ onAllGranted() ──→ Timeline
                               └── Some denied ──→ Stay on screen (show granted/ungranted)
```

**Permissions declared in AndroidManifest.xml:**

| Permission | Purpose | Runtime? |
|---|---|---|
| `READ_CALENDAR` | Calendar events | Yes |
| `READ_SMS` | Text messages | Yes |
| `READ_CALL_LOG` | Call history | Yes |
| `READ_CONTACTS` | Name/avatar resolution | Yes |
| `POST_NOTIFICATIONS` | Digest notifications (Android 13+) | Yes |
| `INTERNET` | Gmail API / IMAP | No (install-time) |
| `ACCESS_NETWORK_STATE` | Network checks | No (install-time) |
| `SCHEDULE_EXACT_ALARM` | Digest scheduling | No (install-time) |

---

## UI Screens

### Permissions Screen
- Lists each permission with granted/ungranted status (checkmark icons)
- Single "Grant Permissions" button launches system multi-permission dialog
- Auto-navigates to Timeline when all granted

### Timeline Screen
- **Top bar:** "Unified Hub" title + filter icon + digest icon
- **Pull-to-refresh:** Triggers `refreshAll()` across all sources
- **Content:** `LazyColumn` with day headers ("Today", "Yesterday", "Monday, March 3, 2026")
- **Cards:** `TimelineItemCard` — type icon (colored), title, contact, timestamp, preview
- **Empty state:** "No items yet — Pull down to refresh"
- **Error handling:** Snackbar for transient errors

### Filter Bottom Sheet
- Modal bottom sheet with 4 `FilterChip` components
- Toggle: Email, Calendar, SMS, Calls
- Changes reactively update the timeline via `Flow`

### Daily Digest Screen
- **Summary row:** 4 cards showing counts (emails, messages, missed calls, events)
- **Actions:** Copy to clipboard, Share (Sharesheet), Schedule at 8 AM
- **Preview:** Monospace text card showing the formatted digest
- **Schedule indicator:** Shows "Digest scheduled daily at 08:00" when active

---

## Build & Run

```bash
# Generate Gradle wrapper (one-time, if not present)
gradle wrapper --gradle-version 8.11.1

# Build debug APK
./gradlew assembleDebug

# Build release APK (minified + resource shrunk)
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Run KSP (Room + Hilt annotation processing)
./gradlew kspDebugKotlin

# Clean build
./gradlew clean assembleDebug
```

**Requirements:**
- JDK 17+
- Android SDK 35
- Gradle 8.11.1
- Android device or emulator running API 33+ (Android 13+)

---

## Tech Stack & Versions

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 2.1.0 | Language |
| AGP | 8.7.3 | Android build |
| KSP | 2.1.0-1.0.29 | Annotation processing (Room, Hilt) |
| Compose BOM | 2024.12.01 | UI framework |
| Material 3 | 1.3.1 | Design system |
| Hilt | 2.54 | Dependency injection |
| Room | 2.6.1 | Local database |
| Navigation Compose | 2.8.5 | Screen routing |
| WorkManager | 2.10.0 | Background jobs |
| Lifecycle | 2.8.7 | ViewModel + Flow collection |
| Coroutines | 1.9.0 | Async programming |
| Google API Client | 2.7.1 | Gmail integration |
| Gmail API | v1-rev20241203 | Email fetching |
| Angus Mail | 2.0.3 | IMAP (Jakarta Mail impl) |
| DataStore | 1.1.1 | Preferences storage |
| Security Crypto | 1.1.0-alpha06 | Encrypted storage |
| Coil | 2.7.0 | Image loading |

All versions managed in `gradle/libs.versions.toml`.

---

## Stub Status — What Needs Implementation

| Component | Status | What's needed |
|---|---|---|
| CalendarRepository | **Complete** | Fully queries CalendarContract.Instances |
| SmsRepository | **Complete** | Fully queries Telephony.Sms |
| CallLogRepository | **Complete** | Fully queries CallLog.Calls |
| GmailDataSourceImpl | **Stub** | Needs Google Sign-In OAuth2 setup, Gmail API service builder |
| ImapDataSourceImpl | **Stub** | Needs credential storage (EncryptedSharedPreferences), Jakarta Mail session connect |
| Contact avatar resolution | **Not started** | Query ContactsContract for photo URI by phone/email |
| Search UI | **Not started** | SearchBar composable in TimelineScreen (ViewModel method exists) |
| Email settings screen | **Not started** | IMAP host/port/user config, Gmail account picker |
| Time picker for digest | **Not started** | Replace hardcoded 8 AM with MaterialTimePicker |
| Unit tests | **Not started** | Repository tests with mock ContentResolver, ViewModel tests |

---

## Security & Privacy

- **Local-only processing** — no data leaves the device
- **No backup** — `android:allowBackup="false"` in manifest
- **Release minification** — ProGuard enabled with `isMinifyEnabled = true` and `isShrinkResources = true`
- **Credential storage** — Security Crypto library included for EncryptedSharedPreferences (IMAP passwords)
- **Permission transparency** — each permission shows clear rationale to user
- **Cache lifecycle** — automatic 90-day pruning in DigestWorker
- **Dedup safety** — unique index on `sourceId` prevents data duplication

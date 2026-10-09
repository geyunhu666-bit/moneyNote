# MoneyNote (记账本)

A simple, ad-free, fully offline bookkeeping app for Android. Built with Kotlin and Jetpack Compose, all data is stored locally — no network, no account, no backend. Your data never leaves your device.

[中文](README.md)

## Features

- **Add a transaction**: supports expense, income, and transfer; custom amount keypad; primary + secondary categories; transfers support source/destination accounts and a fee; notes, date, and time with edit/delete support.
- **Statistics**: category breakdown (donut chart) and ranking, switchable between expense/income and months.
- **Accounts**: cash, debit card, credit card, e-wallet, investment, and more; real-time net worth; create/edit/delete accounts with initial balance and icons.
- **Calendar**: month grid (week starts on Monday) with daily expense summaries and a per-day transaction list.
- **Search**: combined filters by keyword (note/category/account), type, category, account, amount range, and time range (any / this month / last month / last 3 months).
- **Budgets**: monthly total budget and per-category budgets with used/remaining or overspent progress, consistent with statistics (secondary categories included).
- **Category management**: expense and income categories, secondary categories, custom icons, create/edit/delete; preset categories cannot be deleted.
- **Data & backup**: export a full JSON backup and restore from it; export CSV (with UTF-8 BOM, opens correctly in Excel/WPS).
- **Appearance**: dark mode + Material You dynamic color (Android 12+), following system or manual.
- **Privacy**: no network permission, no accounts, no ads; all data stays on-device.

## Tech Stack

- **Language & UI**: Kotlin, Jetpack Compose (Material 3)
- **Architecture**: ViewModel, Lifecycle, Flow/Coroutines
- **Persistence**: Room (KSP), DataStore Preferences
- **Backup codec**: kotlinx.serialization (JSON)
- **Build**: Gradle Kotlin DSL, Version Catalog

| Component | Version |
|---|---|
| AGP / Gradle | 9.0.1 / 9.1.0 |
| Kotlin | 2.3.20 |
| Compose BOM | 2026.03.01 |
| Room / KSP | 2.8.5 / 2.3.12 |
| DataStore | 1.1.7 |
| kotlinx-serialization-json | 1.11.0 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0) |
| Java / Kotlin toolchain | 17 |

## Getting Started

### Requirements

- JDK 17
- Android SDK (`compileSdk 36`)
- Latest Android Studio recommended

### Build APK

```bash
./gradlew assembleDebug
# Windows: gradlew.bat assembleDebug
```

The APK is output to `app/build/outputs/apk/debug/app-debug.apk` and installs on Android 8.0+.

### Run unit tests

```bash
./gradlew testDebugUnitTest
```

## Architecture & Design

- **Single write entry point**: all writes go through `BookkeepingRepository`; the UI layer never touches DAOs directly.
- **Money stored in cents**: amounts use `Long` (unit: cents) throughout, converted to yuan with `BigDecimal` only at the UI boundary to avoid floating-point errors.
- **Dates stored as integers**: `epochDay` (days since 1970-01-01) + `minuteOfDay`; grouping by month/day is pure integer arithmetic with no timezone-string issues.
- **Balances are derived**: account balance = initial balance + all transaction effects. Transactions are the single source of truth; no redundant balance field is persisted.
- **Backup format decoupled from schema**: a DTO layer decouples Room entities from the file format; `ignoreUnknownKeys` keeps it forward/backward compatible. Restore replaces everything in a single transaction to avoid partial data.
- **Pure functions + unit tests**: logic that can run without Android (money conversion, month snapshots, calendar grid, budget progress, search filters, CSV export, backup codec) is extracted into pure functions with tests.
- **State-driven navigation**: one-level tabs + calendar/search overlays + the editor, a fixed hierarchy with no navigation library.

## Project Structure

```
app/src/main/java/com/witsky/moneynote/
├── data/
│   ├── BookkeepingRepository.kt   # single write entry point / business repository
│   ├── MonthSnapshot.kt           # month snapshot pure functions
│   ├── CalendarGrid.kt            # calendar grid pure functions
│   ├── BudgetProgress.kt          # budget progress pure functions
│   ├── SearchFilters.kt           # search filter pure functions
│   ├── CsvExport.kt               # CSV export pure functions
│   ├── BackupCodec.kt             # JSON backup codec
│   ├── BackupPayload.kt           # backup DTO and entity mapping
│   ├── local/                     # Room database, DAOs, entities, seed data
│   └── settings/                  # DataStore settings (theme / onboarding)
└── ui/
    ├── MoneyNoteApp.kt            # main navigation shell
    ├── bills/    # transactions home
    ├── stats/    # statistics
    ├── assets/   # accounts
    ├── mine/     # settings (incl. category management)
    ├── edit/     # transaction editor
    ├── calendar/ # calendar view
    ├── search/   # search
    ├── budget/   # budget management
    ├── data/     # data & backup
    ├── onboarding/ # first-run onboarding
    ├── common/   # shared components (money, date, progress, etc.)
    └── theme/    # theme (colors, typography, Material 3)
```

## License

To be determined (add one before publishing).

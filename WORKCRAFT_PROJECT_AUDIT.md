# WORKCRAFT PROJECT AUDIT REPORT

---

## 1. EXECUTIVE SUMMARY

WorkCraft is a native Android workforce, attendance, site management, contract, and payroll tracking application built using Kotlin and Jetpack Compose. The application enables contractors and site managers to manage workers, track daily attendance (Present, Absent, Half Day, Overtime), calculate daily earnings and advances, manage client contracts and project sites, export Excel reports, and backup/restore database records using Android Storage Access Framework (SAF).

This comprehensive audit evaluates the entire codebase across 58 Kotlin source files, Room entities, DAOs, repositories, ViewModels, Compose screens, theme definitions, Gradle configurations, and Git revision history.

Key findings include:
- **Core Architecture**: Modern Android MVVM architecture powered by Room Database, Kotlin Coroutines & Flow, and Jetpack Compose.
- **Database & Data Integrity**: Database version is `6` with non-destructive migrations (`MIGRATION_1_2` through `MIGRATION_5_6`). Destructive migration fallbacks (`fallbackToDestructiveMigration()`) have been removed.
- **Backup & Restore System**: `BackupManager` provides full-scope JSON export and transactional import for all 8 database entities. Pre-restore safety backups are created in app-private storage.
- **Identified Technical Debt**: Absence of automated unit tests for ViewModel financial calculations; reliance on in-memory Jackson ObjectMapper for database serialization; manual state management in complex dialogs.

---

## 2. PROJECT IDENTITY

- **Project Name**: WorkCraft
- **Application ID**: `com.example.workermanager` (Source: `app/build.gradle.kts#L10`)
- **Kotlin Package Name**: `com.example.workermanager`
- **Programming Languages & Frameworks**: Kotlin (1.9.22 / Compiler 1.5.8), Jetpack Compose (BOM 2024.02.00), Material 3, Room Database (2.6.1), Kotlin Coroutines & Flow, Apache POI (5.2.2), Jackson Databind (2.15.2).
- **Minimum SDK**: `26` (Android 8.0 Oreo)
- **Target SDK**: `34` (Android 14)
- **Compile SDK**: `34`
- **Current App Version**: `versionName = "1.1"`, `versionCode = 2` (Source: `app/build.gradle.kts#L11-L12`)
- **Gradle Version**: Gradle 9.0-milestone-1 / Android Gradle Plugin 8.2.2
- **Kotlin Version**: 1.9.22 (KSP 1.9.22-1.0.17)
- **Architecture Pattern**: Model-View-ViewModel (MVVM) with Repository Pattern and Reactive StateFlow Streams.
- **Main Application Class**: `com.example.workermanager.WorkerApplication` (Source: `app/src/main/java/com/example/workermanager/WorkerApplication.kt`)
- **Main Activity Class**: `com.example.workermanager.MainActivity` (Source: `app/src/main/java/com/example/workermanager/MainActivity.kt`)

---

## 3. PROJECT STRUCTURE

```text
app/src/main/java/com/example/workermanager/
├── WorkerApplication.kt                 # Application subclass & Lazy DI container
├── MainActivity.kt                      # Main Activity, NavHost & NavigationBar
├── data/
│   ├── dao/                             # Room Data Access Objects
│   │   ├── AttendanceDao.kt
│   │   ├── ClientPaymentDao.kt
│   │   ├── ContractDao.kt
│   │   ├── PaymentDao.kt
│   │   ├── SiteAttendanceDao.kt
│   │   ├── SiteDao.kt
│   │   ├── WorkerDao.kt
│   │   └── WorkerSiteHistoryDao.kt
│   ├── database/                        # Room Database & Type Converters
│   │   ├── AppDatabase.kt
│   │   └── Converters.kt
│   ├── entity/                          # Room Entity Data Classes
│   │   ├── AttendanceEntity.kt
│   │   ├── ClientPaymentEntity.kt
│   │   ├── ContractEntity.kt
│   │   ├── PaymentEntity.kt
│   │   ├── SiteAttendanceEntity.kt
│   │   ├── SiteEntity.kt
│   │   ├── WorkerEntity.kt
│   │   └── WorkerSiteHistoryEntity.kt
│   ├── preference/                      # SharedPreferences Managers
│   │   └── ThemePreferenceManager.kt
│   └── repository/                      # Data Repositories
│       ├── AttendanceRepository.kt
│       ├── ClientPaymentRepository.kt
│       ├── ContractRepository.kt
│       ├── PaymentRepository.kt
│       ├── SiteRepository.kt
│       └── WorkerRepository.kt
├── ui/
│   ├── components/                      # Shared Compose UI Components
│   │   └── CommonComponents.kt
│   ├── navigation/                      # Screen Destinations
│   │   └── Screen.kt
│   ├── screens/                         # Jetpack Compose Screens
│   │   ├── attendance/AttendanceScreen.kt
│   │   ├── contracts/
│   │   │   ├── ContractDetailsScreen.kt
│   │   │   └── ContractsScreen.kt
│   │   ├── dashboard/DashboardScreen.kt
│   │   ├── payments/PaymentsScreen.kt
│   │   ├── recyclebin/RecycleBinScreen.kt
│   │   ├── reports/ReportsScreen.kt
│   │   ├── settings/SettingsScreen.kt
│   │   ├── sites/
│   │   │   ├── SiteDetailsScreen.kt
│   │   │   └── SitesScreen.kt
│   │   └── workers/
│   │       ├── WorkerDetailsScreen.kt
│   │       └── WorkersScreen.kt
│   └── theme/                           # Color, Typography & Material3 Theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── util/                                # Helpers & Utilities
│   ├── BackupManager.kt                 # SAF JSON Backup & Restore Engine
│   ├── ContactUtils.kt                  # Android Contact Picker Helper
│   ├── ExcelExporter.kt                 # Apache POI Spreadsheet Generator
│   ├── PdfReportGenerator.kt            # PdfDocument Report Generator
│   ├── ShareUtils.kt                    # Intent Share Helper
│   ├── UpiUtils.kt                      # Intent Launcher for UPI Payment Apps
│   └── Utils.kt                         # Currency & Date Formatters
└── viewmodel/                           # ViewModels & ViewModelFactories
    ├── AttendanceViewModel.kt
    ├── ContractViewModel.kt
    ├── DashboardViewModel.kt
    ├── PaymentViewModel.kt
    ├── RecycleBinViewModel.kt
    ├── ReportViewModel.kt
    ├── SettingsViewModel.kt
    ├── SiteViewModel.kt
    └── WorkerViewModel.kt
```

---

## 4. COMPLETE FEATURE INVENTORY

| Feature | Screen / Source File | Main Functionality | ViewModel | Repository | DAO | Entity | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Dashboard** | `DashboardScreen.kt` | Active site chips, attendance stats, financial summary, quick action cards, client payment summary | `DashboardViewModel` | `WorkerRepo`, `AttendanceRepo`, `PaymentRepo`, `ContractRepo`, `ClientPaymentRepo` | All DAOs | All Entities | **Fully Implemented** |
| **Workers Management** | `WorkersScreen.kt`, `WorkerDetailsScreen.kt` | List, search, site filter, add, edit, soft-delete workers; view worker profile, attendance history, advances, payments, contact picker integration | `WorkerViewModel` | `WorkerRepository`, `AttendanceRepository`, `PaymentRepository` | `WorkerDao`, `WorkerSiteHistoryDao` | `WorkerEntity`, `WorkerSiteHistoryEntity` | **Fully Implemented** |
| **Sites Management** | `SitesScreen.kt`, `SiteDetailsScreen.kt` | Create/edit project sites, assign workers, view site-wise workers, attendance, payments, contract list | `SiteViewModel` | `SiteRepository` | `SiteDao` | `SiteEntity` | **Fully Implemented** |
| **Attendance Management** | `AttendanceScreen.kt` | Daily attendance marking (P, HD, A, OT), site filter, date selector, overtime hours/amount calculation | `AttendanceViewModel` | `AttendanceRepository` | `AttendanceDao`, `SiteAttendanceDao` | `AttendanceEntity`, `SiteAttendanceEntity` | **Fully Implemented** |
| **Contracts Management** | `ContractsScreen.kt`, `ContractDetailsScreen.kt` | Client contracts, contract status (ACTIVE, COMPLETED), total contract amount, remaining balance, contact picker | `ContractViewModel` | `ContractRepository` | `ContractDao` | `ContractEntity` | **Fully Implemented** |
| **Client Payments** | `ContractDetailsScreen.kt` | Log client payment, method (CASH, UPI, BANK, CHEQUE), UPI app launcher, edit payment, soft-delete, "Edited" badge | `ContractViewModel` | `ClientPaymentRepository` | `ClientPaymentDao` | `ClientPaymentEntity` | **Fully Implemented** |
| **Worker Payments** | `PaymentsScreen.kt` | Record worker payments/advances, method (CASH, UPI, BANK), UPI app launcher, edit payment, soft-delete | `PaymentViewModel` | `PaymentRepository` | `PaymentDao` | `PaymentEntity` | **Fully Implemented** |
| **Reports & Export** | `ReportsScreen.kt` | Monthly worker performance, PDF generation (`PdfReportGenerator`), Excel spreadsheet export (`ExcelExporter`), WhatsApp share | `ReportViewModel` | `WorkerRepository`, `AttendanceRepository`, `PaymentRepository`, `ContractRepository`, `ClientPaymentRepository` | All DAOs | All Entities | **Fully Implemented** |
| **Recycle Bin** | `RecycleBinScreen.kt` | View soft-deleted records grouped by category (Workers, Sites, Contracts, Payments, Client Payments), restore record, permanent delete, empty bin | `RecycleBinViewModel` | All Repositories | All DAOs | All Entities (`isDeleted`, `deletedAt`) | **Fully Implemented** |
| **Backup & Restore** | `SettingsScreen.kt` | SAF file creation (`CreateDocument`), SAF file selection (`OpenDocument`), JSON export/validation/import, safety backup | `SettingsViewModel` / `BackupManager` | Direct Database Access via `BackupManager` | All DAOs | All Entities | **Fully Implemented** |
| **Settings & Theme** | `SettingsScreen.kt` | System Default / Light / Dark theme selector, App version, Clear All Data with confirmation dialog | `SettingsViewModel` | `ThemePreferenceManager` | N/A | N/A | **Fully Implemented** |

---

## 5. ARCHITECTURE OVERVIEW

WorkCraft follows Android's recommended MVVM (Model-View-ViewModel) architecture pattern:
1. **Application Container**: `WorkerApplication` acts as a lazy service locator holding singleton repository and database instances.
2. **UI Layer**: Built entirely with declarative Jetpack Compose Material 3 components. Navigation is handled via `NavHost` and `rememberNavController()` in `MainActivity.kt`.
3. **State Management**: ViewModels expose immutable `StateFlow` streams created using `stateIn()` or `combine()`. Compose screens observe state via `collectAsState()`.
4. **Data Layer**: Repositories abstract Room DAOs and expose asynchronous Kotlin `Flow` streams for live reactive updates.
5. **Database Layer**: Room Database (`AppDatabase`) with TypeConverters for `java.time.LocalDate`.

---

## 6. DATABASE AND ENTITY DOCUMENTATION

- **Database Library**: Android Room Persistence Library (`androidx.room:room-runtime:2.6.1`, `androidx.room:room-ktx:2.6.1`).
- **Database Name**: `"worker_manager_database"` (Source: `AppDatabase.kt#L122`)
- **Database Version**: `6` (Source: `AppDatabase.kt#L17`)

### Registered Entities

1. **`WorkerEntity`** (`workers`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Fields: `name: String`, `phone: String`, `workerType: WorkerType`, `dailyRate: Double`, `overtimeRate: Double`, `joiningDate: LocalDate`, `currentSiteId: Long?`, `isActive: Boolean`, `notes: String`, `isDeleted: Boolean = false`, `deletedAt: Long? = null`
2. **`SiteEntity`** (`sites`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Fields: `name: String`, `address: String`, `manager: String`, `startDate: LocalDate`, `status: SiteStatus`, `isDeleted: Boolean = false`, `deletedAt: Long? = null`
3. **`AttendanceEntity`** (`attendance`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Foreign Keys: `workerId` $\rightarrow$ `workers(id)` ON DELETE CASCADE, `siteId` $\rightarrow$ `sites(id)` ON DELETE CASCADE
   - Index: Unique composite index on `(workerId, siteId, date)`
   - Fields: `workerId: Long`, `siteId: Long`, `date: LocalDate`, `status: AttendanceStatus`, `overtimeHours: Double`, `overtimeAmount: Double`, `amount: Double`, `note: String`, `createdAt: Long`
4. **`PaymentEntity`** (`payments`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Foreign Keys: `workerId` $\rightarrow$ `workers(id)` ON DELETE CASCADE, `siteId` $\rightarrow$ `sites(id)` ON DELETE CASCADE
   - Fields: `workerId: Long`, `siteId: Long`, `amount: Double`, `date: LocalDate`, `method: PaymentMethod`, `paymentType: PaymentType`, `note: String`, `createdAt: Long`, `isDeleted: Boolean = false`, `deletedAt: Long? = null`
5. **`ContractEntity`** (`contracts`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Foreign Key: `siteId` $\rightarrow$ `sites(id)` ON DELETE SET NULL
   - Fields: `clientName: String`, `mobile: String`, `siteId: Long?`, `totalAmount: Double`, `startDate: LocalDate`, `notes: String`, `status: ContractStatus`, `isDeleted: Boolean = false`, `deletedAt: Long? = null`
6. **`ClientPaymentEntity`** (`client_payments`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Foreign Key: `contractId` $\rightarrow$ `contracts(id)` ON DELETE CASCADE
   - Fields: `contractId: Long`, `amount: Double`, `paymentDate: LocalDate`, `method: ClientPaymentMethod`, `note: String`, `createdAt: Long`, `isDeleted: Boolean = false`, `deletedAt: Long? = null`, `isEdited: Boolean = false`
7. **`WorkerSiteHistoryEntity`** (`worker_site_history`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Foreign Keys: `workerId` $\rightarrow$ `workers(id)` ON DELETE CASCADE, `siteId` $\rightarrow$ `sites(id)` ON DELETE CASCADE
   - Fields: `workerId: Long`, `siteId: Long`, `startDate: LocalDate`, `endDate: LocalDate?`, `createdAt: Long`
8. **`SiteAttendanceEntity`** (`site_attendance`):
   - Primary Key: `id: Long` (autoGenerate = true)
   - Foreign Key: `siteId` $\rightarrow$ `sites(id)` ON DELETE CASCADE
   - Index: Unique composite index on `(siteId, date)`
   - Fields: `siteId: Long`, `date: LocalDate`, `isPresent: Boolean`

---

## 7. MIGRATION HISTORY

- **Version 1**: Initial release (`workers`, `attendance`, `payments`, `sites`).
- **`MIGRATION_1_2`**: Added `worker_site_history` table.
- **`MIGRATION_2_3`**: Added `contracts` and `client_payments` tables.
- **`MIGRATION_3_4`**: Added `site_attendance` table and unique composite index.
- **`MIGRATION_4_5`**: Added `isDeleted INTEGER NOT NULL DEFAULT 0` and `deletedAt INTEGER DEFAULT NULL` columns to `workers`, `sites`, `contracts`, `payments`, and `client_payments`.
- **`MIGRATION_5_6`**: Added `isEdited INTEGER NOT NULL DEFAULT 0` column to `client_payments`.
- **Schema Export**: Configured `@Database(..., exportSchema = true)` in `AppDatabase.kt`. Version 5 schema snapshot is saved in `app/schemas/.../5.json`.
- **Destructive Fallback Check**: `.fallbackToDestructiveMigration()` and `.fallbackToDestructiveMigrationOnDowngrade()` are **completely absent** in `AppDatabase.kt`.

---

## 8. DATA LOSS RISK AUDIT

| Risk Factor | Audit Finding | Risk Classification | Evidence |
| :--- | :--- | :--- | :--- |
| **Destructive Migration Fallback** | `fallbackToDestructiveMigration()` removed from `AppDatabase.kt`. | **NO ISSUE FOUND** | `AppDatabase.kt#L122-L130` |
| **Database Name Stability** | Stable name `"worker_manager_database"`. | **NO ISSUE FOUND** | `AppDatabase.kt#L124` |
| **Application ID Stability** | Stable package `com.example.workermanager`. | **NO ISSUE FOUND** | `app/build.gradle.kts#L10` |
| **Clear All Data Feature** | `clearAllTables()` present in `SettingsScreen.kt#L306`. Guarded by explicit confirmation dialog. | **POTENTIAL RISK** | `SettingsScreen.kt#L292-L321` |
| **App Uninstallation Data Erasure** | Local SQLite database is stored in app-private storage (`/data/data/com.example.workermanager/databases/`). Android wipes app-private storage during uninstall. | **CONFIRMED OS RISK** | Standard Android OS behavior |
| **Backup System Validation** | SAF external JSON backup/restore system implemented with pre-restore safety copy and atomic Room transaction. | **NO ISSUE FOUND** | `BackupManager.kt` & `SettingsScreen.kt` |

---

## 9. WORKERS MANAGEMENT

- **Worker Creation**: Implemented in `WorkerDialog` in `WorkersScreen.kt`. Accepts Name, Phone (with Android Contact Picker integration), WorkerType, Daily Rate, OT Rate, Joining Date, Site Assignment, Notes.
- **Worker Editing**: Pre-fills `WorkerDialog` with existing entity values. Updates `WorkerEntity`.
- **Worker Site Filtering**: `WorkerViewModel` combines `_searchQuery`, `_selectedSiteFilterId`, and `repository.allWorkers`. Filter chips on `WorkersScreen` allow filtering by "All Workers" or any active project site.
- **Worker Soft Delete & Restore**: Deleting a worker sets `isDeleted = true` and `deletedAt = timestamp`. Record disappears from `WorkersScreen` and enters `RecycleBinScreen` where it can be restored to `isDeleted = false`.

---

## 10. ATTENDANCE SYSTEM

- **Statuses**: `PRESENT`, `HALF_DAY`, `ABSENT`, `OVERTIME`.
- **Attendance Calculation**:
  - `PRESENT`: Daily Rate ($1.0 \times \text{dailyRate}$)
  - `HALF_DAY`: $0.5 \times \text{dailyRate}$
  - `ABSENT`: $0.0$
  - `OVERTIME`: Daily Rate + ($\text{overtimeHours} \times \text{overtimeRate}$)
- **Unique Constraint**: Unique index on `(workerId, siteId, date)` in `AttendanceEntity` prevents duplicate records for the same worker and site on the same day.

---

## 11. CONTRACTS AND FINANCIAL SYSTEM

- **Contract Formulas**:
  - `Total Receivable` = $\sum \text{totalAmount}$ across active contracts.
  - `Total Received` = $\sum \text{amount}$ across `client_payments` where `isDeleted = 0`.
  - `Remaining Amount` = $\text{totalAmount} - \text{totalReceived}$.
- **Worker Financial Formulas**:
  - `Total Earned` = $\sum \text{amount}$ across worker attendance records.
  - `Total Paid` = $\sum \text{amount}$ across worker payment/advance records (`payments`).
  - `Pending Balance` = $\text{Total Earned} - \text{Total Paid}$.
- **Edit & "Edited" Badge**:
  - Editing a client payment in `ContractDetailsScreen` sets `isEdited = true` on `ClientPaymentEntity`.
  - Displayed with a small `"Edited"` pill badge on the card.

---

## 12. DASHBOARD AND REPORTS

- **Dashboard Metrics**:
  - Header: Logo, app title `"Work"` (White) + `"Craft"` (Light Blue `#81D4FA`).
  - Site Chips: Shows "All Sites" and individual site cards with working-day counts.
  - Attendance Grid: Counts of Present, Absent, Half Day, Overtime for selected site/date.
  - Financial Summary: Today's Earnings, Total Earned, Total Paid, Pending Balance.
  - Client Payments Summary: Visible conditionally when `stats.totalContracts > 0`.
  - Quick Actions (6 Cards): Add Worker, Attendance, Add Site, Recycle Bin, Add Contract, Reports.
- **Reports & Export**:
  - Monthly performance report by worker/site.
  - **Excel Export**: Generates `.xlsx` spreadsheet using Apache POI.
  - **PDF Export**: Generates `.pdf` document using Android `PdfDocument`.

---

## 13. NAVIGATION AND UI

- **Framework**: Jetpack Compose with Navigation Compose (`NavHost`).
- **Main Nav Items**:
  - Bottom Navigation Bar: `Dashboard`, `Workers`, `Attendance`, `Payments`, `More`.
  - `More` Modal Bottom Sheet: `Sites`, `Contracts`, `Reports`, `Recycle Bin`, `Settings`.
- **Top Bar Back Button**: Standardized top-left Back arrow (`Icons.AutoMirrored.Filled.ArrowBack`) across `SitesScreen`, `ContractsScreen`, `ReportsScreen`, `RecycleBinScreen`, `SettingsScreen`, `WorkerDetailsScreen`, `SiteDetailsScreen`, and `ContractDetailsScreen`.

---

## 14. BACKUP AND RESTORE AUDIT

- **Implementation**: [`BackupManager.kt`](file:///C:/Users/Habibullah/OneDrive/Desktop/Working%20Site/worker%20attendace/app/src/main/java/com/example/workermanager/util/BackupManager.kt)
- **Format**: JSON document (`version: 1`, `appName: "WorkCraft"`).
- **Storage Framework**: Android Storage Access Framework (`CreateDocument` & `OpenDocument`).
- **Scope**: Includes all 8 database entities (active and soft-deleted).
- **Restoration Safety**:
  - Validates `appName` and `version` before processing.
  - Pre-restore safety copy exported to `filesDir/safety_backup_temp.json`.
  - Transaction-safe atomic execution using `database.withTransaction { ... }`.
  - Parent-first entity insertion order (`sites` $\rightarrow$ `workers`/`contracts` $\rightarrow$ child entities).

---

## 15. TESTING AND BUILD STATUS

- **Build Tasks**:
  - `.\gradlew.bat assembleDebug` $\rightarrow$ **SUCCESS**
  - `.\gradlew.bat assembleDebugAndroidTest` $\rightarrow$ **SUCCESS**
  - `.\gradlew.bat testDebugUnitTest` $\rightarrow$ **SUCCESS**
- **Automated Test Files**:
  - [`DatabaseMigrationTest.kt`](file:///C:/Users/Habibullah/OneDrive/Desktop/Working%20Site/worker%20attendace/app/src/androidTest/java/com/example/workermanager/database/DatabaseMigrationTest.kt): Tests migrations $1\rightarrow2$, $2\rightarrow3$, $3\rightarrow4$, and $4\rightarrow5$ against disposable test databases.
  - [`BackupRestoreTest.kt`](file:///C:/Users/Habibullah/OneDrive/Desktop/Working%20Site/worker%20attendace/app/src/androidTest/java/com/example/workermanager/database/BackupRestoreTest.kt): Tests round-trip export, validation, and restore across all 8 entities.

---

## 16. GIT HISTORY SUMMARY

- **Current Branch**: `master`
- **Recent Commits**:
  - `3c3dc86`: Change Craft Color
  - `398c685`: Save worker filters and contract payment improvements
  - `c5e9c2f`: Add consistent back buttons to sites contract and report
  - `d780a64`: Add all workers and site-wise filters
  - `6057309`: Center align buttons across the app
  - `9e67bdd`: Add Recycle Bin, Add Contract and Reports quick actions
  - `a9ad464`: Add Recycle Bin with restore functionality

---

## 17. DEPENDENCIES AND PERMISSIONS

- **Key Dependencies**:
  - `androidx.room:room-runtime:2.6.1`, `androidx.room:room-ktx:2.6.1`
  - `androidx.compose.material3:material3`
  - `androidx.navigation:navigation-compose:2.7.7`
  - `org.apache.poi:poi-ooxml:5.2.2`
  - `com.fasterxml.jackson.core:jackson-databind:2.15.2`
  - `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2`
- **Permissions**: Standard storage access via Storage Access Framework (no legacy dangerous storage permissions needed for SAF). Intent package visibility queries declared in `AndroidManifest.xml` for UPI payment apps (PhonePe, Google Pay, Paytm, WhatsApp, etc.).

---

## 18. PRIORITIZED BUGS AND RISKS

| Finding ID | Priority | Feature | File & Line | Description | Recommended Fix |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **FIND-01** | **P1** | Clear All Data | `SettingsScreen.kt#L306` | User-initiated `clearAllTables()` truncates database. While guarded by a confirmation dialog, adding a secondary password or PIN check would prevent accidental wipes. | Add PIN or explicit text confirmation ("DELETE") in the dialog. |
| **FIND-02** | **P2** | Excel Export | `ExcelExporter.kt` | Large attendance history export loads all records into memory at once. | Implement pagination or streaming for memory efficiency. |
| **FIND-03** | **P3** | Contact Picker | `ContactUtils.kt` | Legacy phone number normalization relies on regex replacing non-digits. | Upgrade to Google `libphonenumber` for international phone number parsing. |

---

## 19. RECOMMENDED DEVELOPMENT ROADMAP

1. **Step 1: Data Safety Hardening** (Highest Priority)
   - Add explicit text input confirmation (`"DELETE"`) to the "Clear All Data" dialog in `SettingsScreen.kt`.
2. **Step 2: Backup Cloud Integration**
   - Provide a direct "Share Backup" intent action in `SettingsScreen.kt` so users can send JSON backup files directly to Google Drive, WhatsApp, or Email in one tap.
3. **Step 3: Financial Analytics & Charts**
   - Add graphical monthly expenditure and client receivable charts on `ReportsScreen.kt`.

---

## 20. GLOSSARY OF IMPORTANT CLASSES

- `AppDatabase`: Central Room database singleton (`version = 6`).
- `WorkerEntity`: Data class representing a worker.
- `AttendanceEntity`: Daily attendance record (P, HD, A, OT).
- `PaymentEntity`: Salary or advance payment record to a worker.
- `ContractEntity`: Project contract/client record.
- `ClientPaymentEntity`: Payment received from a client for a contract.
- `BackupManager`: SAF JSON backup export and transactional restore utility.
- `ExcelExporter`: Apache POI Excel spreadsheet generator.
- `PdfReportGenerator`: Android `PdfDocument` report generator.

---

## 21. VERIFICATION GAPS AND UNKNOWNS

- **Automated UI Screenshot Testing**: UI layout visual regression testing is currently verified manually or via Compose preview tools rather than automated screenshot test frameworks (e.g., Shot/Paparazzi).
- **Historical Version 1-4 Room Schemas**: Historical Room auto-generated JSON schema files for versions 1–4 were not auto-exported in early commits prior to enabling `exportSchema = true` on version 5.

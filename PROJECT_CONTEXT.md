# WORKCRAFT PROJECT CONTEXT

## 1. What the App Does
WorkCraft is a native Android workforce management, payroll, and site contract tracking application. It allows contractors to:
- Manage workers, daily wages, and overtime rates.
- Track daily attendance (Present, Half Day, Absent, Overtime) by site and worker.
- Record worker payments and advances with optional UPI app integration.
- Create client contracts, log client payments, and track remaining balances.
- Export monthly payroll, attendance, and client payment reports to Excel (`.xlsx`) and PDF.
- Soft-delete records to a Recycle Bin with full restoration support.
- Backup and restore the entire SQLite database to/from JSON files using Android's Storage Access Framework (SAF).

---

## 2. Technical Architecture
- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Pattern**: MVVM with Repository Pattern & Reactive StateFlow
- **Database**: Room Database 2.6.1 with TypeConverters for `java.time.LocalDate`
- **JSON / Export Engine**: Jackson Databind 2.15.2 (with JSR310 JavaTimeModule) & Apache POI 5.2.2

---

## 3. Important File Paths
- **Application Entry Point**: `app/src/main/java/com/example/workermanager/WorkerApplication.kt`
- **Main Activity & Navigation**: `app/src/main/java/com/example/workermanager/MainActivity.kt`
- **Room Database Definition**: `app/src/main/java/com/example/workermanager/data/database/AppDatabase.kt`
- **Backup & Restore Utility**: `app/src/main/java/com/example/workermanager/util/BackupManager.kt`
- **Screens**:
  - Dashboard: `app/src/main/java/com/example/workermanager/ui/screens/dashboard/DashboardScreen.kt`
  - Workers: `app/src/main/java/com/example/workermanager/ui/screens/workers/WorkersScreen.kt`
  - Worker Details: `app/src/main/java/com/example/workermanager/ui/screens/workers/WorkerDetailsScreen.kt`
  - Attendance: `app/src/main/java/com/example/workermanager/ui/screens/attendance/AttendanceScreen.kt`
  - Payments: `app/src/main/java/com/example/workermanager/ui/screens/payments/PaymentsScreen.kt`
  - Contracts & Clients: `app/src/main/java/com/example/workermanager/ui/screens/contracts/ContractsScreen.kt`
  - Contract Details: `app/src/main/java/com/example/workermanager/ui/screens/contracts/ContractDetailsScreen.kt`
  - Sites: `app/src/main/java/com/example/workermanager/ui/screens/sites/SitesScreen.kt`
  - Site Details: `app/src/main/java/com/example/workermanager/ui/screens/sites/SiteDetailsScreen.kt`
  - Reports: `app/src/main/java/com/example/workermanager/ui/screens/reports/ReportsScreen.kt`
  - Recycle Bin: `app/src/main/java/com/example/workermanager/ui/screens/recyclebin/RecycleBinScreen.kt`
  - Settings: `app/src/main/java/com/example/workermanager/ui/screens/settings/SettingsScreen.kt`

---

## 4. Database Specifications
- **Database Name**: `"worker_manager_database"`
- **Database Version**: `6`
- **Entities (8 Tables)**:
  1. `WorkerEntity` (`workers`)
  2. `SiteEntity` (`sites`)
  3. `AttendanceEntity` (`attendance`)
  4. `PaymentEntity` (`payments`)
  5. `ContractEntity` (`contracts`)
  6. `ClientPaymentEntity` (`client_payments`)
  7. `WorkerSiteHistoryEntity` (`worker_site_history`)
  8. `SiteAttendanceEntity` (`site_attendance`)

---

## 5. Important Entity Relationships
- `AttendanceEntity` has foreign keys referencing `WorkerEntity(id)` (CASCADE) and `SiteEntity(id)` (CASCADE).
- `PaymentEntity` has foreign keys referencing `WorkerEntity(id)` (CASCADE) and `SiteEntity(id)` (CASCADE).
- `ContractEntity` has foreign key referencing `SiteEntity(id)` (SET NULL).
- `ClientPaymentEntity` has foreign key referencing `ContractEntity(id)` (CASCADE).
- Soft delete (`isDeleted = true`) preserves all foreign key relationships without triggering SQLite CASCADE deletes.

---

## 6. Critical Testing & Safety Rules for AI Coding Agents
1. **NEVER** use `fallbackToDestructiveMigration()` or `fallbackToDestructiveMigrationOnDowngrade()`.
2. **NEVER** uninstall the app, run `adb uninstall`, or clear app storage.
3. **NEVER** run `connectedDebugAndroidTest` or deploy to a physical phone.
4. **ALWAYS** use isolated in-memory databases (`Room.inMemoryDatabaseBuilder`) for automated tests.
5. **ALWAYS** preserve existing database migrations (`MIGRATION_1_2` through `MIGRATION_5_6`).
6. **NEVER** execute destructive Git commands (`git reset --hard`, `git checkout .`, `git clean`).

---

## 7. Current Development State & Recommended Next Task
- **Current State**: Fully functional, production-ready, styled with a modern two-color Dashboard title ("WorkCraft"), full-scope SAF backup/restore, soft delete recycle bin, and standardized back buttons.
- **Recommended Next Task**: Add an explicit text confirmation requirement (`"DELETE"`) inside the "Clear All Data" dialog in `SettingsScreen.kt` for extra user data protection.

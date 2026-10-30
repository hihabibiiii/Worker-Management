package com.example.workermanager.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.example.workermanager.data.dao.AttendanceDao;
import com.example.workermanager.data.dao.AttendanceDao_Impl;
import com.example.workermanager.data.dao.ClientPaymentDao;
import com.example.workermanager.data.dao.ClientPaymentDao_Impl;
import com.example.workermanager.data.dao.ContractDao;
import com.example.workermanager.data.dao.ContractDao_Impl;
import com.example.workermanager.data.dao.ContractorMaterialDao;
import com.example.workermanager.data.dao.ContractorMaterialDao_Impl;
import com.example.workermanager.data.dao.ContractorMaterialTransactionDao;
import com.example.workermanager.data.dao.ContractorMaterialTransactionDao_Impl;
import com.example.workermanager.data.dao.MaterialDao;
import com.example.workermanager.data.dao.MaterialDao_Impl;
import com.example.workermanager.data.dao.MaterialTransactionDao;
import com.example.workermanager.data.dao.MaterialTransactionDao_Impl;
import com.example.workermanager.data.dao.PaymentDao;
import com.example.workermanager.data.dao.PaymentDao_Impl;
import com.example.workermanager.data.dao.SiteAttendanceDao;
import com.example.workermanager.data.dao.SiteAttendanceDao_Impl;
import com.example.workermanager.data.dao.SiteDao;
import com.example.workermanager.data.dao.SiteDao_Impl;
import com.example.workermanager.data.dao.SiteExpenseDao;
import com.example.workermanager.data.dao.SiteExpenseDao_Impl;
import com.example.workermanager.data.dao.SiteExpensePaymentDao;
import com.example.workermanager.data.dao.SiteExpensePaymentDao_Impl;
import com.example.workermanager.data.dao.WorkerDao;
import com.example.workermanager.data.dao.WorkerDao_Impl;
import com.example.workermanager.data.dao.WorkerSiteHistoryDao;
import com.example.workermanager.data.dao.WorkerSiteHistoryDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile WorkerDao _workerDao;

  private volatile AttendanceDao _attendanceDao;

  private volatile PaymentDao _paymentDao;

  private volatile SiteDao _siteDao;

  private volatile WorkerSiteHistoryDao _workerSiteHistoryDao;

  private volatile ContractDao _contractDao;

  private volatile ClientPaymentDao _clientPaymentDao;

  private volatile SiteAttendanceDao _siteAttendanceDao;

  private volatile SiteExpenseDao _siteExpenseDao;

  private volatile SiteExpensePaymentDao _siteExpensePaymentDao;

  private volatile MaterialDao _materialDao;

  private volatile MaterialTransactionDao _materialTransactionDao;

  private volatile ContractorMaterialDao _contractorMaterialDao;

  private volatile ContractorMaterialTransactionDao _contractorMaterialTransactionDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(12) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `workers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `workerType` TEXT NOT NULL, `dailyRate` REAL NOT NULL, `overtimeRate` REAL NOT NULL, `joiningDate` TEXT NOT NULL, `currentSiteId` INTEGER, `isActive` INTEGER NOT NULL, `notes` TEXT NOT NULL, `photoPath` TEXT, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workerId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, `date` TEXT NOT NULL, `status` TEXT NOT NULL, `overtimeHours` REAL NOT NULL, `overtimeAmount` REAL NOT NULL, `amount` REAL NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_workerId_siteId_date` ON `attendance` (`workerId`, `siteId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workerId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, `amount` REAL NOT NULL, `date` TEXT NOT NULL, `method` TEXT NOT NULL, `paymentType` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `manager` TEXT NOT NULL, `startDate` TEXT NOT NULL, `status` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `worker_site_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workerId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT, `createdAt` INTEGER NOT NULL, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contracts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientName` TEXT NOT NULL, `mobile` TEXT NOT NULL, `siteId` INTEGER, `totalAmount` REAL NOT NULL, `startDate` TEXT NOT NULL, `notes` TEXT NOT NULL, `status` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `client_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contractId` INTEGER NOT NULL, `amount` REAL NOT NULL, `paymentDate` TEXT NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `isEdited` INTEGER NOT NULL, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`contractId`) REFERENCES `contracts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `site_attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `siteId` INTEGER NOT NULL, `date` TEXT NOT NULL, `isPresent` INTEGER NOT NULL, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_site_attendance_siteId_date` ON `site_attendance` (`siteId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `site_expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `siteId` INTEGER NOT NULL, `expenseDate` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `quantity` REAL, `rate` REAL, `amount` REAL NOT NULL, `paidBy` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL, `notes` TEXT NOT NULL, `receiptPath` TEXT, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `site_expense_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `expenseId` INTEGER NOT NULL, `paymentDate` TEXT NOT NULL, `amount` REAL NOT NULL, `paidBy` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`expenseId`) REFERENCES `site_expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `materials` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `unit` TEXT NOT NULL, `minStockLevel` REAL NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `material_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `materialId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, `transactionType` TEXT NOT NULL, `transactionDate` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `ratePerUnit` REAL NOT NULL, `totalAmount` REAL NOT NULL, `supplierName` TEXT NOT NULL, `supplierPhone` TEXT NOT NULL, `invoiceNumber` TEXT NOT NULL, `workDescription` TEXT NOT NULL, `targetSiteId` INTEGER, `linkedExpenseId` INTEGER, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`materialId`) REFERENCES `materials`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contractor_materials` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `unit` TEXT NOT NULL, `minStockLevel` REAL NOT NULL, `notes` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `isPendingSync` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contractor_material_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `materialId` INTEGER NOT NULL, `siteId` INTEGER, `transactionType` TEXT NOT NULL, `transactionDate` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `reasonOrNotes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `syncOrigin` INTEGER NOT NULL, `syncId` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isPendingSync` INTEGER NOT NULL, FOREIGN KEY(`materialId`) REFERENCES `contractor_materials`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1f628c3601912f7b530b903c6f288533')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `workers`");
        db.execSQL("DROP TABLE IF EXISTS `attendance`");
        db.execSQL("DROP TABLE IF EXISTS `payments`");
        db.execSQL("DROP TABLE IF EXISTS `sites`");
        db.execSQL("DROP TABLE IF EXISTS `worker_site_history`");
        db.execSQL("DROP TABLE IF EXISTS `contracts`");
        db.execSQL("DROP TABLE IF EXISTS `client_payments`");
        db.execSQL("DROP TABLE IF EXISTS `site_attendance`");
        db.execSQL("DROP TABLE IF EXISTS `site_expenses`");
        db.execSQL("DROP TABLE IF EXISTS `site_expense_payments`");
        db.execSQL("DROP TABLE IF EXISTS `materials`");
        db.execSQL("DROP TABLE IF EXISTS `material_transactions`");
        db.execSQL("DROP TABLE IF EXISTS `contractor_materials`");
        db.execSQL("DROP TABLE IF EXISTS `contractor_material_transactions`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWorkers = new HashMap<String, TableInfo.Column>(17);
        _columnsWorkers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("workerType", new TableInfo.Column("workerType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("dailyRate", new TableInfo.Column("dailyRate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("overtimeRate", new TableInfo.Column("overtimeRate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("joiningDate", new TableInfo.Column("joiningDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("currentSiteId", new TableInfo.Column("currentSiteId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("photoPath", new TableInfo.Column("photoPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkers.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWorkers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWorkers = new TableInfo("workers", _columnsWorkers, _foreignKeysWorkers, _indicesWorkers);
        final TableInfo _existingWorkers = TableInfo.read(db, "workers");
        if (!_infoWorkers.equals(_existingWorkers)) {
          return new RoomOpenHelper.ValidationResult(false, "workers(com.example.workermanager.data.entity.WorkerEntity).\n"
                  + " Expected:\n" + _infoWorkers + "\n"
                  + " Found:\n" + _existingWorkers);
        }
        final HashMap<String, TableInfo.Column> _columnsAttendance = new HashMap<String, TableInfo.Column>(16);
        _columnsAttendance.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("workerId", new TableInfo.Column("workerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("overtimeHours", new TableInfo.Column("overtimeHours", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("overtimeAmount", new TableInfo.Column("overtimeAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAttendance = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysAttendance.add(new TableInfo.ForeignKey("workers", "CASCADE", "NO ACTION", Arrays.asList("workerId"), Arrays.asList("id")));
        _foreignKeysAttendance.add(new TableInfo.ForeignKey("sites", "CASCADE", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAttendance = new HashSet<TableInfo.Index>(1);
        _indicesAttendance.add(new TableInfo.Index("index_attendance_workerId_siteId_date", true, Arrays.asList("workerId", "siteId", "date"), Arrays.asList("ASC", "ASC", "ASC")));
        final TableInfo _infoAttendance = new TableInfo("attendance", _columnsAttendance, _foreignKeysAttendance, _indicesAttendance);
        final TableInfo _existingAttendance = TableInfo.read(db, "attendance");
        if (!_infoAttendance.equals(_existingAttendance)) {
          return new RoomOpenHelper.ValidationResult(false, "attendance(com.example.workermanager.data.entity.AttendanceEntity).\n"
                  + " Expected:\n" + _infoAttendance + "\n"
                  + " Found:\n" + _existingAttendance);
        }
        final HashMap<String, TableInfo.Column> _columnsPayments = new HashMap<String, TableInfo.Column>(15);
        _columnsPayments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("workerId", new TableInfo.Column("workerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("method", new TableInfo.Column("method", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("paymentType", new TableInfo.Column("paymentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPayments = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysPayments.add(new TableInfo.ForeignKey("workers", "CASCADE", "NO ACTION", Arrays.asList("workerId"), Arrays.asList("id")));
        _foreignKeysPayments.add(new TableInfo.ForeignKey("sites", "CASCADE", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPayments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPayments = new TableInfo("payments", _columnsPayments, _foreignKeysPayments, _indicesPayments);
        final TableInfo _existingPayments = TableInfo.read(db, "payments");
        if (!_infoPayments.equals(_existingPayments)) {
          return new RoomOpenHelper.ValidationResult(false, "payments(com.example.workermanager.data.entity.PaymentEntity).\n"
                  + " Expected:\n" + _infoPayments + "\n"
                  + " Found:\n" + _existingPayments);
        }
        final HashMap<String, TableInfo.Column> _columnsSites = new HashMap<String, TableInfo.Column>(12);
        _columnsSites.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("address", new TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("manager", new TableInfo.Column("manager", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSites = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSites = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSites = new TableInfo("sites", _columnsSites, _foreignKeysSites, _indicesSites);
        final TableInfo _existingSites = TableInfo.read(db, "sites");
        if (!_infoSites.equals(_existingSites)) {
          return new RoomOpenHelper.ValidationResult(false, "sites(com.example.workermanager.data.entity.SiteEntity).\n"
                  + " Expected:\n" + _infoSites + "\n"
                  + " Found:\n" + _existingSites);
        }
        final HashMap<String, TableInfo.Column> _columnsWorkerSiteHistory = new HashMap<String, TableInfo.Column>(10);
        _columnsWorkerSiteHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("workerId", new TableInfo.Column("workerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("endDate", new TableInfo.Column("endDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkerSiteHistory = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysWorkerSiteHistory.add(new TableInfo.ForeignKey("workers", "CASCADE", "NO ACTION", Arrays.asList("workerId"), Arrays.asList("id")));
        _foreignKeysWorkerSiteHistory.add(new TableInfo.ForeignKey("sites", "CASCADE", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesWorkerSiteHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWorkerSiteHistory = new TableInfo("worker_site_history", _columnsWorkerSiteHistory, _foreignKeysWorkerSiteHistory, _indicesWorkerSiteHistory);
        final TableInfo _existingWorkerSiteHistory = TableInfo.read(db, "worker_site_history");
        if (!_infoWorkerSiteHistory.equals(_existingWorkerSiteHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "worker_site_history(com.example.workermanager.data.entity.WorkerSiteHistoryEntity).\n"
                  + " Expected:\n" + _infoWorkerSiteHistory + "\n"
                  + " Found:\n" + _existingWorkerSiteHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsContracts = new HashMap<String, TableInfo.Column>(14);
        _columnsContracts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("clientName", new TableInfo.Column("clientName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("mobile", new TableInfo.Column("mobile", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("siteId", new TableInfo.Column("siteId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("totalAmount", new TableInfo.Column("totalAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContracts = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysContracts.add(new TableInfo.ForeignKey("sites", "SET NULL", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesContracts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoContracts = new TableInfo("contracts", _columnsContracts, _foreignKeysContracts, _indicesContracts);
        final TableInfo _existingContracts = TableInfo.read(db, "contracts");
        if (!_infoContracts.equals(_existingContracts)) {
          return new RoomOpenHelper.ValidationResult(false, "contracts(com.example.workermanager.data.entity.ContractEntity).\n"
                  + " Expected:\n" + _infoContracts + "\n"
                  + " Found:\n" + _existingContracts);
        }
        final HashMap<String, TableInfo.Column> _columnsClientPayments = new HashMap<String, TableInfo.Column>(14);
        _columnsClientPayments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("contractId", new TableInfo.Column("contractId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("paymentDate", new TableInfo.Column("paymentDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("method", new TableInfo.Column("method", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("isEdited", new TableInfo.Column("isEdited", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysClientPayments = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysClientPayments.add(new TableInfo.ForeignKey("contracts", "CASCADE", "NO ACTION", Arrays.asList("contractId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesClientPayments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoClientPayments = new TableInfo("client_payments", _columnsClientPayments, _foreignKeysClientPayments, _indicesClientPayments);
        final TableInfo _existingClientPayments = TableInfo.read(db, "client_payments");
        if (!_infoClientPayments.equals(_existingClientPayments)) {
          return new RoomOpenHelper.ValidationResult(false, "client_payments(com.example.workermanager.data.entity.ClientPaymentEntity).\n"
                  + " Expected:\n" + _infoClientPayments + "\n"
                  + " Found:\n" + _existingClientPayments);
        }
        final HashMap<String, TableInfo.Column> _columnsSiteAttendance = new HashMap<String, TableInfo.Column>(8);
        _columnsSiteAttendance.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteAttendance.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteAttendance.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteAttendance.put("isPresent", new TableInfo.Column("isPresent", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteAttendance.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteAttendance.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteAttendance.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteAttendance.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSiteAttendance = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysSiteAttendance.add(new TableInfo.ForeignKey("sites", "CASCADE", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSiteAttendance = new HashSet<TableInfo.Index>(1);
        _indicesSiteAttendance.add(new TableInfo.Index("index_site_attendance_siteId_date", true, Arrays.asList("siteId", "date"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoSiteAttendance = new TableInfo("site_attendance", _columnsSiteAttendance, _foreignKeysSiteAttendance, _indicesSiteAttendance);
        final TableInfo _existingSiteAttendance = TableInfo.read(db, "site_attendance");
        if (!_infoSiteAttendance.equals(_existingSiteAttendance)) {
          return new RoomOpenHelper.ValidationResult(false, "site_attendance(com.example.workermanager.data.entity.SiteAttendanceEntity).\n"
                  + " Expected:\n" + _infoSiteAttendance + "\n"
                  + " Found:\n" + _existingSiteAttendance);
        }
        final HashMap<String, TableInfo.Column> _columnsSiteExpenses = new HashMap<String, TableInfo.Column>(19);
        _columnsSiteExpenses.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("expenseDate", new TableInfo.Column("expenseDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("quantity", new TableInfo.Column("quantity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("rate", new TableInfo.Column("rate", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("paidBy", new TableInfo.Column("paidBy", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("paymentMethod", new TableInfo.Column("paymentMethod", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("receiptPath", new TableInfo.Column("receiptPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpenses.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSiteExpenses = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysSiteExpenses.add(new TableInfo.ForeignKey("sites", "CASCADE", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSiteExpenses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSiteExpenses = new TableInfo("site_expenses", _columnsSiteExpenses, _foreignKeysSiteExpenses, _indicesSiteExpenses);
        final TableInfo _existingSiteExpenses = TableInfo.read(db, "site_expenses");
        if (!_infoSiteExpenses.equals(_existingSiteExpenses)) {
          return new RoomOpenHelper.ValidationResult(false, "site_expenses(com.example.workermanager.data.entity.SiteExpenseEntity).\n"
                  + " Expected:\n" + _infoSiteExpenses + "\n"
                  + " Found:\n" + _existingSiteExpenses);
        }
        final HashMap<String, TableInfo.Column> _columnsSiteExpensePayments = new HashMap<String, TableInfo.Column>(14);
        _columnsSiteExpensePayments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("expenseId", new TableInfo.Column("expenseId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("paymentDate", new TableInfo.Column("paymentDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("paidBy", new TableInfo.Column("paidBy", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("paymentMethod", new TableInfo.Column("paymentMethod", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSiteExpensePayments.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSiteExpensePayments = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysSiteExpensePayments.add(new TableInfo.ForeignKey("site_expenses", "CASCADE", "NO ACTION", Arrays.asList("expenseId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSiteExpensePayments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSiteExpensePayments = new TableInfo("site_expense_payments", _columnsSiteExpensePayments, _foreignKeysSiteExpensePayments, _indicesSiteExpensePayments);
        final TableInfo _existingSiteExpensePayments = TableInfo.read(db, "site_expense_payments");
        if (!_infoSiteExpensePayments.equals(_existingSiteExpensePayments)) {
          return new RoomOpenHelper.ValidationResult(false, "site_expense_payments(com.example.workermanager.data.entity.SiteExpensePaymentEntity).\n"
                  + " Expected:\n" + _infoSiteExpensePayments + "\n"
                  + " Found:\n" + _existingSiteExpensePayments);
        }
        final HashMap<String, TableInfo.Column> _columnsMaterials = new HashMap<String, TableInfo.Column>(12);
        _columnsMaterials.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("minStockLevel", new TableInfo.Column("minStockLevel", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterials.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMaterials = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMaterials = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMaterials = new TableInfo("materials", _columnsMaterials, _foreignKeysMaterials, _indicesMaterials);
        final TableInfo _existingMaterials = TableInfo.read(db, "materials");
        if (!_infoMaterials.equals(_existingMaterials)) {
          return new RoomOpenHelper.ValidationResult(false, "materials(com.example.workermanager.data.entity.MaterialEntity).\n"
                  + " Expected:\n" + _infoMaterials + "\n"
                  + " Found:\n" + _existingMaterials);
        }
        final HashMap<String, TableInfo.Column> _columnsMaterialTransactions = new HashMap<String, TableInfo.Column>(23);
        _columnsMaterialTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("materialId", new TableInfo.Column("materialId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("transactionType", new TableInfo.Column("transactionType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("transactionDate", new TableInfo.Column("transactionDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("quantity", new TableInfo.Column("quantity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("ratePerUnit", new TableInfo.Column("ratePerUnit", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("totalAmount", new TableInfo.Column("totalAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("supplierName", new TableInfo.Column("supplierName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("supplierPhone", new TableInfo.Column("supplierPhone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("invoiceNumber", new TableInfo.Column("invoiceNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("workDescription", new TableInfo.Column("workDescription", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("targetSiteId", new TableInfo.Column("targetSiteId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("linkedExpenseId", new TableInfo.Column("linkedExpenseId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMaterialTransactions.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMaterialTransactions = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysMaterialTransactions.add(new TableInfo.ForeignKey("materials", "CASCADE", "NO ACTION", Arrays.asList("materialId"), Arrays.asList("id")));
        _foreignKeysMaterialTransactions.add(new TableInfo.ForeignKey("sites", "CASCADE", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesMaterialTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMaterialTransactions = new TableInfo("material_transactions", _columnsMaterialTransactions, _foreignKeysMaterialTransactions, _indicesMaterialTransactions);
        final TableInfo _existingMaterialTransactions = TableInfo.read(db, "material_transactions");
        if (!_infoMaterialTransactions.equals(_existingMaterialTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "material_transactions(com.example.workermanager.data.entity.MaterialTransactionEntity).\n"
                  + " Expected:\n" + _infoMaterialTransactions + "\n"
                  + " Found:\n" + _existingMaterialTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsContractorMaterials = new HashMap<String, TableInfo.Column>(14);
        _columnsContractorMaterials.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("minStockLevel", new TableInfo.Column("minStockLevel", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterials.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContractorMaterials = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContractorMaterials = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoContractorMaterials = new TableInfo("contractor_materials", _columnsContractorMaterials, _foreignKeysContractorMaterials, _indicesContractorMaterials);
        final TableInfo _existingContractorMaterials = TableInfo.read(db, "contractor_materials");
        if (!_infoContractorMaterials.equals(_existingContractorMaterials)) {
          return new RoomOpenHelper.ValidationResult(false, "contractor_materials(com.example.workermanager.data.entity.ContractorMaterialEntity).\n"
                  + " Expected:\n" + _infoContractorMaterials + "\n"
                  + " Found:\n" + _existingContractorMaterials);
        }
        final HashMap<String, TableInfo.Column> _columnsContractorMaterialTransactions = new HashMap<String, TableInfo.Column>(15);
        _columnsContractorMaterialTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("materialId", new TableInfo.Column("materialId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("siteId", new TableInfo.Column("siteId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("transactionType", new TableInfo.Column("transactionType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("transactionDate", new TableInfo.Column("transactionDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("quantity", new TableInfo.Column("quantity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("reasonOrNotes", new TableInfo.Column("reasonOrNotes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("syncOrigin", new TableInfo.Column("syncOrigin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContractorMaterialTransactions.put("isPendingSync", new TableInfo.Column("isPendingSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContractorMaterialTransactions = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysContractorMaterialTransactions.add(new TableInfo.ForeignKey("contractor_materials", "CASCADE", "NO ACTION", Arrays.asList("materialId"), Arrays.asList("id")));
        _foreignKeysContractorMaterialTransactions.add(new TableInfo.ForeignKey("sites", "SET NULL", "NO ACTION", Arrays.asList("siteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesContractorMaterialTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoContractorMaterialTransactions = new TableInfo("contractor_material_transactions", _columnsContractorMaterialTransactions, _foreignKeysContractorMaterialTransactions, _indicesContractorMaterialTransactions);
        final TableInfo _existingContractorMaterialTransactions = TableInfo.read(db, "contractor_material_transactions");
        if (!_infoContractorMaterialTransactions.equals(_existingContractorMaterialTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "contractor_material_transactions(com.example.workermanager.data.entity.ContractorMaterialTransactionEntity).\n"
                  + " Expected:\n" + _infoContractorMaterialTransactions + "\n"
                  + " Found:\n" + _existingContractorMaterialTransactions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "1f628c3601912f7b530b903c6f288533", "5ce508dc5a621b2648de0aace27c02e7");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "workers","attendance","payments","sites","worker_site_history","contracts","client_payments","site_attendance","site_expenses","site_expense_payments","materials","material_transactions","contractor_materials","contractor_material_transactions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `workers`");
      _db.execSQL("DELETE FROM `attendance`");
      _db.execSQL("DELETE FROM `payments`");
      _db.execSQL("DELETE FROM `sites`");
      _db.execSQL("DELETE FROM `worker_site_history`");
      _db.execSQL("DELETE FROM `contracts`");
      _db.execSQL("DELETE FROM `client_payments`");
      _db.execSQL("DELETE FROM `site_attendance`");
      _db.execSQL("DELETE FROM `site_expenses`");
      _db.execSQL("DELETE FROM `site_expense_payments`");
      _db.execSQL("DELETE FROM `materials`");
      _db.execSQL("DELETE FROM `material_transactions`");
      _db.execSQL("DELETE FROM `contractor_materials`");
      _db.execSQL("DELETE FROM `contractor_material_transactions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WorkerDao.class, WorkerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AttendanceDao.class, AttendanceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PaymentDao.class, PaymentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SiteDao.class, SiteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WorkerSiteHistoryDao.class, WorkerSiteHistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContractDao.class, ContractDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ClientPaymentDao.class, ClientPaymentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SiteAttendanceDao.class, SiteAttendanceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SiteExpenseDao.class, SiteExpenseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SiteExpensePaymentDao.class, SiteExpensePaymentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MaterialDao.class, MaterialDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MaterialTransactionDao.class, MaterialTransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContractorMaterialDao.class, ContractorMaterialDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContractorMaterialTransactionDao.class, ContractorMaterialTransactionDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WorkerDao workerDao() {
    if (_workerDao != null) {
      return _workerDao;
    } else {
      synchronized(this) {
        if(_workerDao == null) {
          _workerDao = new WorkerDao_Impl(this);
        }
        return _workerDao;
      }
    }
  }

  @Override
  public AttendanceDao attendanceDao() {
    if (_attendanceDao != null) {
      return _attendanceDao;
    } else {
      synchronized(this) {
        if(_attendanceDao == null) {
          _attendanceDao = new AttendanceDao_Impl(this);
        }
        return _attendanceDao;
      }
    }
  }

  @Override
  public PaymentDao paymentDao() {
    if (_paymentDao != null) {
      return _paymentDao;
    } else {
      synchronized(this) {
        if(_paymentDao == null) {
          _paymentDao = new PaymentDao_Impl(this);
        }
        return _paymentDao;
      }
    }
  }

  @Override
  public SiteDao siteDao() {
    if (_siteDao != null) {
      return _siteDao;
    } else {
      synchronized(this) {
        if(_siteDao == null) {
          _siteDao = new SiteDao_Impl(this);
        }
        return _siteDao;
      }
    }
  }

  @Override
  public WorkerSiteHistoryDao workerSiteHistoryDao() {
    if (_workerSiteHistoryDao != null) {
      return _workerSiteHistoryDao;
    } else {
      synchronized(this) {
        if(_workerSiteHistoryDao == null) {
          _workerSiteHistoryDao = new WorkerSiteHistoryDao_Impl(this);
        }
        return _workerSiteHistoryDao;
      }
    }
  }

  @Override
  public ContractDao contractDao() {
    if (_contractDao != null) {
      return _contractDao;
    } else {
      synchronized(this) {
        if(_contractDao == null) {
          _contractDao = new ContractDao_Impl(this);
        }
        return _contractDao;
      }
    }
  }

  @Override
  public ClientPaymentDao clientPaymentDao() {
    if (_clientPaymentDao != null) {
      return _clientPaymentDao;
    } else {
      synchronized(this) {
        if(_clientPaymentDao == null) {
          _clientPaymentDao = new ClientPaymentDao_Impl(this);
        }
        return _clientPaymentDao;
      }
    }
  }

  @Override
  public SiteAttendanceDao siteAttendanceDao() {
    if (_siteAttendanceDao != null) {
      return _siteAttendanceDao;
    } else {
      synchronized(this) {
        if(_siteAttendanceDao == null) {
          _siteAttendanceDao = new SiteAttendanceDao_Impl(this);
        }
        return _siteAttendanceDao;
      }
    }
  }

  @Override
  public SiteExpenseDao siteExpenseDao() {
    if (_siteExpenseDao != null) {
      return _siteExpenseDao;
    } else {
      synchronized(this) {
        if(_siteExpenseDao == null) {
          _siteExpenseDao = new SiteExpenseDao_Impl(this);
        }
        return _siteExpenseDao;
      }
    }
  }

  @Override
  public SiteExpensePaymentDao siteExpensePaymentDao() {
    if (_siteExpensePaymentDao != null) {
      return _siteExpensePaymentDao;
    } else {
      synchronized(this) {
        if(_siteExpensePaymentDao == null) {
          _siteExpensePaymentDao = new SiteExpensePaymentDao_Impl(this);
        }
        return _siteExpensePaymentDao;
      }
    }
  }

  @Override
  public MaterialDao materialDao() {
    if (_materialDao != null) {
      return _materialDao;
    } else {
      synchronized(this) {
        if(_materialDao == null) {
          _materialDao = new MaterialDao_Impl(this);
        }
        return _materialDao;
      }
    }
  }

  @Override
  public MaterialTransactionDao materialTransactionDao() {
    if (_materialTransactionDao != null) {
      return _materialTransactionDao;
    } else {
      synchronized(this) {
        if(_materialTransactionDao == null) {
          _materialTransactionDao = new MaterialTransactionDao_Impl(this);
        }
        return _materialTransactionDao;
      }
    }
  }

  @Override
  public ContractorMaterialDao contractorMaterialDao() {
    if (_contractorMaterialDao != null) {
      return _contractorMaterialDao;
    } else {
      synchronized(this) {
        if(_contractorMaterialDao == null) {
          _contractorMaterialDao = new ContractorMaterialDao_Impl(this);
        }
        return _contractorMaterialDao;
      }
    }
  }

  @Override
  public ContractorMaterialTransactionDao contractorMaterialTransactionDao() {
    if (_contractorMaterialTransactionDao != null) {
      return _contractorMaterialTransactionDao;
    } else {
      synchronized(this) {
        if(_contractorMaterialTransactionDao == null) {
          _contractorMaterialTransactionDao = new ContractorMaterialTransactionDao_Impl(this);
        }
        return _contractorMaterialTransactionDao;
      }
    }
  }
}

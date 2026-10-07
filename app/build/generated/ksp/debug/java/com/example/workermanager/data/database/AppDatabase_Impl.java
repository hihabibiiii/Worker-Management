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
import com.example.workermanager.data.dao.PaymentDao;
import com.example.workermanager.data.dao.PaymentDao_Impl;
import com.example.workermanager.data.dao.SiteDao;
import com.example.workermanager.data.dao.SiteDao_Impl;
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

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `workers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `workerType` TEXT NOT NULL, `dailyRate` REAL NOT NULL, `overtimeRate` REAL NOT NULL, `joiningDate` TEXT NOT NULL, `currentSiteId` INTEGER, `isActive` INTEGER NOT NULL, `notes` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workerId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, `date` TEXT NOT NULL, `status` TEXT NOT NULL, `overtimeHours` REAL NOT NULL, `overtimeAmount` REAL NOT NULL, `amount` REAL NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_workerId_siteId_date` ON `attendance` (`workerId`, `siteId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workerId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, `amount` REAL NOT NULL, `date` TEXT NOT NULL, `method` TEXT NOT NULL, `paymentType` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `manager` TEXT NOT NULL, `startDate` TEXT NOT NULL, `status` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `worker_site_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workerId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contracts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientName` TEXT NOT NULL, `mobile` TEXT NOT NULL, `siteId` INTEGER, `totalAmount` REAL NOT NULL, `startDate` TEXT NOT NULL, `notes` TEXT NOT NULL, `status` TEXT NOT NULL, FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `client_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contractId` INTEGER NOT NULL, `amount` REAL NOT NULL, `paymentDate` TEXT NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`contractId`) REFERENCES `contracts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dfc45e6b7e7654eba5a9dd410b726711')");
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
        final HashMap<String, TableInfo.Column> _columnsWorkers = new HashMap<String, TableInfo.Column>(10);
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
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWorkers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWorkers = new TableInfo("workers", _columnsWorkers, _foreignKeysWorkers, _indicesWorkers);
        final TableInfo _existingWorkers = TableInfo.read(db, "workers");
        if (!_infoWorkers.equals(_existingWorkers)) {
          return new RoomOpenHelper.ValidationResult(false, "workers(com.example.workermanager.data.entity.WorkerEntity).\n"
                  + " Expected:\n" + _infoWorkers + "\n"
                  + " Found:\n" + _existingWorkers);
        }
        final HashMap<String, TableInfo.Column> _columnsAttendance = new HashMap<String, TableInfo.Column>(10);
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
        final HashMap<String, TableInfo.Column> _columnsPayments = new HashMap<String, TableInfo.Column>(9);
        _columnsPayments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("workerId", new TableInfo.Column("workerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("method", new TableInfo.Column("method", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("paymentType", new TableInfo.Column("paymentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
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
        final HashMap<String, TableInfo.Column> _columnsSites = new HashMap<String, TableInfo.Column>(6);
        _columnsSites.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("address", new TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("manager", new TableInfo.Column("manager", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSites.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSites = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSites = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSites = new TableInfo("sites", _columnsSites, _foreignKeysSites, _indicesSites);
        final TableInfo _existingSites = TableInfo.read(db, "sites");
        if (!_infoSites.equals(_existingSites)) {
          return new RoomOpenHelper.ValidationResult(false, "sites(com.example.workermanager.data.entity.SiteEntity).\n"
                  + " Expected:\n" + _infoSites + "\n"
                  + " Found:\n" + _existingSites);
        }
        final HashMap<String, TableInfo.Column> _columnsWorkerSiteHistory = new HashMap<String, TableInfo.Column>(6);
        _columnsWorkerSiteHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("workerId", new TableInfo.Column("workerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("siteId", new TableInfo.Column("siteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("endDate", new TableInfo.Column("endDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkerSiteHistory.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
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
        final HashMap<String, TableInfo.Column> _columnsContracts = new HashMap<String, TableInfo.Column>(8);
        _columnsContracts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("clientName", new TableInfo.Column("clientName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("mobile", new TableInfo.Column("mobile", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("siteId", new TableInfo.Column("siteId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("totalAmount", new TableInfo.Column("totalAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContracts.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
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
        final HashMap<String, TableInfo.Column> _columnsClientPayments = new HashMap<String, TableInfo.Column>(7);
        _columnsClientPayments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("contractId", new TableInfo.Column("contractId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("paymentDate", new TableInfo.Column("paymentDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("method", new TableInfo.Column("method", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClientPayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
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
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "dfc45e6b7e7654eba5a9dd410b726711", "4b3b269b47c974e63938adc1771cf647");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "workers","attendance","payments","sites","worker_site_history","contracts","client_payments");
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
}

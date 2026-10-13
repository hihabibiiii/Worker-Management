package com.example.workermanager.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.workermanager.data.database.Converters;
import com.example.workermanager.data.entity.WorkerEntity;
import com.example.workermanager.data.entity.WorkerType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WorkerDao_Impl implements WorkerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkerEntity> __insertionAdapterOfWorkerEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<WorkerEntity> __deletionAdapterOfWorkerEntity;

  private final EntityDeletionOrUpdateAdapter<WorkerEntity> __updateAdapterOfWorkerEntity;

  private final SharedSQLiteStatement __preparedStmtOfSoftDeleteWorker;

  private final SharedSQLiteStatement __preparedStmtOfRestoreWorker;

  private final SharedSQLiteStatement __preparedStmtOfPermanentlyDeleteWorker;

  private final SharedSQLiteStatement __preparedStmtOfEmptyWorkerTrash;

  public WorkerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkerEntity = new EntityInsertionAdapter<WorkerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `workers` (`id`,`name`,`phone`,`workerType`,`dailyRate`,`overtimeRate`,`joiningDate`,`currentSiteId`,`isActive`,`notes`,`isDeleted`,`deletedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkerEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getPhone());
        final String _tmp = __converters.fromWorkerType(entity.getWorkerType());
        statement.bindString(4, _tmp);
        statement.bindDouble(5, entity.getDailyRate());
        statement.bindDouble(6, entity.getOvertimeRate());
        final String _tmp_1 = __converters.fromLocalDate(entity.getJoiningDate());
        if (_tmp_1 == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp_1);
        }
        if (entity.getCurrentSiteId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getCurrentSiteId());
        }
        final int _tmp_2 = entity.isActive() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        statement.bindString(10, entity.getNotes());
        final int _tmp_3 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp_3);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getDeletedAt());
        }
      }
    };
    this.__deletionAdapterOfWorkerEntity = new EntityDeletionOrUpdateAdapter<WorkerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `workers` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkerEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfWorkerEntity = new EntityDeletionOrUpdateAdapter<WorkerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `workers` SET `id` = ?,`name` = ?,`phone` = ?,`workerType` = ?,`dailyRate` = ?,`overtimeRate` = ?,`joiningDate` = ?,`currentSiteId` = ?,`isActive` = ?,`notes` = ?,`isDeleted` = ?,`deletedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkerEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getPhone());
        final String _tmp = __converters.fromWorkerType(entity.getWorkerType());
        statement.bindString(4, _tmp);
        statement.bindDouble(5, entity.getDailyRate());
        statement.bindDouble(6, entity.getOvertimeRate());
        final String _tmp_1 = __converters.fromLocalDate(entity.getJoiningDate());
        if (_tmp_1 == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp_1);
        }
        if (entity.getCurrentSiteId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getCurrentSiteId());
        }
        final int _tmp_2 = entity.isActive() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        statement.bindString(10, entity.getNotes());
        final int _tmp_3 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp_3);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getDeletedAt());
        }
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfSoftDeleteWorker = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE workers SET isDeleted = 1, deletedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRestoreWorker = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE workers SET isDeleted = 0, deletedAt = NULL WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfPermanentlyDeleteWorker = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workers WHERE id = ? AND isDeleted = 1";
        return _query;
      }
    };
    this.__preparedStmtOfEmptyWorkerTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workers WHERE isDeleted = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertWorker(final WorkerEntity worker,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWorkerEntity.insertAndReturnId(worker);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteWorker(final WorkerEntity worker,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWorkerEntity.handle(worker);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateWorker(final WorkerEntity worker,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWorkerEntity.handle(worker);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object softDeleteWorker(final long id, final long deletedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDeleteWorker.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, deletedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSoftDeleteWorker.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object restoreWorker(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRestoreWorker.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRestoreWorker.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object permanentlyDeleteWorker(final long id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPermanentlyDeleteWorker.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPermanentlyDeleteWorker.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object emptyWorkerTrash(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEmptyWorkerTrash.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfEmptyWorkerTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WorkerEntity>> getAllWorkers() {
    final String _sql = "SELECT * FROM workers WHERE isDeleted = 0 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workers"}, new Callable<List<WorkerEntity>>() {
      @Override
      @NonNull
      public List<WorkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfWorkerType = CursorUtil.getColumnIndexOrThrow(_cursor, "workerType");
          final int _cursorIndexOfDailyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRate");
          final int _cursorIndexOfOvertimeRate = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeRate");
          final int _cursorIndexOfJoiningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joiningDate");
          final int _cursorIndexOfCurrentSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSiteId");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<WorkerEntity> _result = new ArrayList<WorkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final WorkerType _tmpWorkerType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWorkerType);
            _tmpWorkerType = __converters.toWorkerType(_tmp);
            final double _tmpDailyRate;
            _tmpDailyRate = _cursor.getDouble(_cursorIndexOfDailyRate);
            final double _tmpOvertimeRate;
            _tmpOvertimeRate = _cursor.getDouble(_cursorIndexOfOvertimeRate);
            final LocalDate _tmpJoiningDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfJoiningDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfJoiningDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpJoiningDate = _tmp_2;
            }
            final Long _tmpCurrentSiteId;
            if (_cursor.isNull(_cursorIndexOfCurrentSiteId)) {
              _tmpCurrentSiteId = null;
            } else {
              _tmpCurrentSiteId = _cursor.getLong(_cursorIndexOfCurrentSiteId);
            }
            final boolean _tmpIsActive;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_3 != 0;
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _item = new WorkerEntity(_tmpId,_tmpName,_tmpPhone,_tmpWorkerType,_tmpDailyRate,_tmpOvertimeRate,_tmpJoiningDate,_tmpCurrentSiteId,_tmpIsActive,_tmpNotes,_tmpIsDeleted,_tmpDeletedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllWorkersRaw(final Continuation<? super List<WorkerEntity>> $completion) {
    final String _sql = "SELECT * FROM workers";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkerEntity>>() {
      @Override
      @NonNull
      public List<WorkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfWorkerType = CursorUtil.getColumnIndexOrThrow(_cursor, "workerType");
          final int _cursorIndexOfDailyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRate");
          final int _cursorIndexOfOvertimeRate = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeRate");
          final int _cursorIndexOfJoiningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joiningDate");
          final int _cursorIndexOfCurrentSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSiteId");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<WorkerEntity> _result = new ArrayList<WorkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final WorkerType _tmpWorkerType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWorkerType);
            _tmpWorkerType = __converters.toWorkerType(_tmp);
            final double _tmpDailyRate;
            _tmpDailyRate = _cursor.getDouble(_cursorIndexOfDailyRate);
            final double _tmpOvertimeRate;
            _tmpOvertimeRate = _cursor.getDouble(_cursorIndexOfOvertimeRate);
            final LocalDate _tmpJoiningDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfJoiningDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfJoiningDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpJoiningDate = _tmp_2;
            }
            final Long _tmpCurrentSiteId;
            if (_cursor.isNull(_cursorIndexOfCurrentSiteId)) {
              _tmpCurrentSiteId = null;
            } else {
              _tmpCurrentSiteId = _cursor.getLong(_cursorIndexOfCurrentSiteId);
            }
            final boolean _tmpIsActive;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_3 != 0;
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _item = new WorkerEntity(_tmpId,_tmpName,_tmpPhone,_tmpWorkerType,_tmpDailyRate,_tmpOvertimeRate,_tmpJoiningDate,_tmpCurrentSiteId,_tmpIsActive,_tmpNotes,_tmpIsDeleted,_tmpDeletedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<WorkerEntity> getWorkerById(final long workerId) {
    final String _sql = "SELECT * FROM workers WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, workerId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workers"}, new Callable<WorkerEntity>() {
      @Override
      @Nullable
      public WorkerEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfWorkerType = CursorUtil.getColumnIndexOrThrow(_cursor, "workerType");
          final int _cursorIndexOfDailyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRate");
          final int _cursorIndexOfOvertimeRate = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeRate");
          final int _cursorIndexOfJoiningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joiningDate");
          final int _cursorIndexOfCurrentSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSiteId");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final WorkerEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final WorkerType _tmpWorkerType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWorkerType);
            _tmpWorkerType = __converters.toWorkerType(_tmp);
            final double _tmpDailyRate;
            _tmpDailyRate = _cursor.getDouble(_cursorIndexOfDailyRate);
            final double _tmpOvertimeRate;
            _tmpOvertimeRate = _cursor.getDouble(_cursorIndexOfOvertimeRate);
            final LocalDate _tmpJoiningDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfJoiningDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfJoiningDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpJoiningDate = _tmp_2;
            }
            final Long _tmpCurrentSiteId;
            if (_cursor.isNull(_cursorIndexOfCurrentSiteId)) {
              _tmpCurrentSiteId = null;
            } else {
              _tmpCurrentSiteId = _cursor.getLong(_cursorIndexOfCurrentSiteId);
            }
            final boolean _tmpIsActive;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_3 != 0;
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _result = new WorkerEntity(_tmpId,_tmpName,_tmpPhone,_tmpWorkerType,_tmpDailyRate,_tmpOvertimeRate,_tmpJoiningDate,_tmpCurrentSiteId,_tmpIsActive,_tmpNotes,_tmpIsDeleted,_tmpDeletedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WorkerEntity>> getWorkersBySite(final long siteId) {
    final String _sql = "SELECT * FROM workers WHERE currentSiteId = ? AND isDeleted = 0 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, siteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workers"}, new Callable<List<WorkerEntity>>() {
      @Override
      @NonNull
      public List<WorkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfWorkerType = CursorUtil.getColumnIndexOrThrow(_cursor, "workerType");
          final int _cursorIndexOfDailyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRate");
          final int _cursorIndexOfOvertimeRate = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeRate");
          final int _cursorIndexOfJoiningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joiningDate");
          final int _cursorIndexOfCurrentSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSiteId");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<WorkerEntity> _result = new ArrayList<WorkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final WorkerType _tmpWorkerType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWorkerType);
            _tmpWorkerType = __converters.toWorkerType(_tmp);
            final double _tmpDailyRate;
            _tmpDailyRate = _cursor.getDouble(_cursorIndexOfDailyRate);
            final double _tmpOvertimeRate;
            _tmpOvertimeRate = _cursor.getDouble(_cursorIndexOfOvertimeRate);
            final LocalDate _tmpJoiningDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfJoiningDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfJoiningDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpJoiningDate = _tmp_2;
            }
            final Long _tmpCurrentSiteId;
            if (_cursor.isNull(_cursorIndexOfCurrentSiteId)) {
              _tmpCurrentSiteId = null;
            } else {
              _tmpCurrentSiteId = _cursor.getLong(_cursorIndexOfCurrentSiteId);
            }
            final boolean _tmpIsActive;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_3 != 0;
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _item = new WorkerEntity(_tmpId,_tmpName,_tmpPhone,_tmpWorkerType,_tmpDailyRate,_tmpOvertimeRate,_tmpJoiningDate,_tmpCurrentSiteId,_tmpIsActive,_tmpNotes,_tmpIsDeleted,_tmpDeletedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WorkerEntity>> getDeletedWorkers() {
    final String _sql = "SELECT * FROM workers WHERE isDeleted = 1 ORDER BY deletedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workers"}, new Callable<List<WorkerEntity>>() {
      @Override
      @NonNull
      public List<WorkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfWorkerType = CursorUtil.getColumnIndexOrThrow(_cursor, "workerType");
          final int _cursorIndexOfDailyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRate");
          final int _cursorIndexOfOvertimeRate = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeRate");
          final int _cursorIndexOfJoiningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joiningDate");
          final int _cursorIndexOfCurrentSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSiteId");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<WorkerEntity> _result = new ArrayList<WorkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final WorkerType _tmpWorkerType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWorkerType);
            _tmpWorkerType = __converters.toWorkerType(_tmp);
            final double _tmpDailyRate;
            _tmpDailyRate = _cursor.getDouble(_cursorIndexOfDailyRate);
            final double _tmpOvertimeRate;
            _tmpOvertimeRate = _cursor.getDouble(_cursorIndexOfOvertimeRate);
            final LocalDate _tmpJoiningDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfJoiningDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfJoiningDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpJoiningDate = _tmp_2;
            }
            final Long _tmpCurrentSiteId;
            if (_cursor.isNull(_cursorIndexOfCurrentSiteId)) {
              _tmpCurrentSiteId = null;
            } else {
              _tmpCurrentSiteId = _cursor.getLong(_cursorIndexOfCurrentSiteId);
            }
            final boolean _tmpIsActive;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_3 != 0;
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _item = new WorkerEntity(_tmpId,_tmpName,_tmpPhone,_tmpWorkerType,_tmpDailyRate,_tmpOvertimeRate,_tmpJoiningDate,_tmpCurrentSiteId,_tmpIsActive,_tmpNotes,_tmpIsDeleted,_tmpDeletedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WorkerEntity>> searchWorkers(final String query) {
    final String _sql = "SELECT * FROM workers WHERE isDeleted = 0 AND (name LIKE '%' || ? || '%' OR phone LIKE '%' || ? || '%')";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workers"}, new Callable<List<WorkerEntity>>() {
      @Override
      @NonNull
      public List<WorkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfWorkerType = CursorUtil.getColumnIndexOrThrow(_cursor, "workerType");
          final int _cursorIndexOfDailyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRate");
          final int _cursorIndexOfOvertimeRate = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeRate");
          final int _cursorIndexOfJoiningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joiningDate");
          final int _cursorIndexOfCurrentSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSiteId");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<WorkerEntity> _result = new ArrayList<WorkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final WorkerType _tmpWorkerType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWorkerType);
            _tmpWorkerType = __converters.toWorkerType(_tmp);
            final double _tmpDailyRate;
            _tmpDailyRate = _cursor.getDouble(_cursorIndexOfDailyRate);
            final double _tmpOvertimeRate;
            _tmpOvertimeRate = _cursor.getDouble(_cursorIndexOfOvertimeRate);
            final LocalDate _tmpJoiningDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfJoiningDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfJoiningDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpJoiningDate = _tmp_2;
            }
            final Long _tmpCurrentSiteId;
            if (_cursor.isNull(_cursorIndexOfCurrentSiteId)) {
              _tmpCurrentSiteId = null;
            } else {
              _tmpCurrentSiteId = _cursor.getLong(_cursorIndexOfCurrentSiteId);
            }
            final boolean _tmpIsActive;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_3 != 0;
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _item = new WorkerEntity(_tmpId,_tmpName,_tmpPhone,_tmpWorkerType,_tmpDailyRate,_tmpOvertimeRate,_tmpJoiningDate,_tmpCurrentSiteId,_tmpIsActive,_tmpNotes,_tmpIsDeleted,_tmpDeletedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WorkerEntity>> getActiveWorkers() {
    final String _sql = "SELECT * FROM workers WHERE isActive = 1 AND isDeleted = 0 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workers"}, new Callable<List<WorkerEntity>>() {
      @Override
      @NonNull
      public List<WorkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfWorkerType = CursorUtil.getColumnIndexOrThrow(_cursor, "workerType");
          final int _cursorIndexOfDailyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRate");
          final int _cursorIndexOfOvertimeRate = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeRate");
          final int _cursorIndexOfJoiningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joiningDate");
          final int _cursorIndexOfCurrentSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "currentSiteId");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<WorkerEntity> _result = new ArrayList<WorkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkerEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final WorkerType _tmpWorkerType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWorkerType);
            _tmpWorkerType = __converters.toWorkerType(_tmp);
            final double _tmpDailyRate;
            _tmpDailyRate = _cursor.getDouble(_cursorIndexOfDailyRate);
            final double _tmpOvertimeRate;
            _tmpOvertimeRate = _cursor.getDouble(_cursorIndexOfOvertimeRate);
            final LocalDate _tmpJoiningDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfJoiningDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfJoiningDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpJoiningDate = _tmp_2;
            }
            final Long _tmpCurrentSiteId;
            if (_cursor.isNull(_cursorIndexOfCurrentSiteId)) {
              _tmpCurrentSiteId = null;
            } else {
              _tmpCurrentSiteId = _cursor.getLong(_cursorIndexOfCurrentSiteId);
            }
            final boolean _tmpIsActive;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_3 != 0;
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _item = new WorkerEntity(_tmpId,_tmpName,_tmpPhone,_tmpWorkerType,_tmpDailyRate,_tmpOvertimeRate,_tmpJoiningDate,_tmpCurrentSiteId,_tmpIsActive,_tmpNotes,_tmpIsDeleted,_tmpDeletedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

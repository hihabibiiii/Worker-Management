package com.example.workermanager.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.workermanager.data.database.Converters;
import com.example.workermanager.data.entity.AttendanceEntity;
import com.example.workermanager.data.entity.AttendanceStatus;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.IllegalStateException;
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
public final class AttendanceDao_Impl implements AttendanceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AttendanceEntity> __insertionAdapterOfAttendanceEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<AttendanceEntity> __updateAdapterOfAttendanceEntity;

  public AttendanceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAttendanceEntity = new EntityInsertionAdapter<AttendanceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `attendance` (`id`,`workerId`,`siteId`,`date`,`status`,`overtimeHours`,`overtimeAmount`,`amount`,`note`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AttendanceEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getWorkerId());
        statement.bindLong(3, entity.getSiteId());
        final String _tmp = __converters.fromLocalDate(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.fromAttendanceStatus(entity.getStatus());
        statement.bindString(5, _tmp_1);
        statement.bindDouble(6, entity.getOvertimeHours());
        statement.bindDouble(7, entity.getOvertimeAmount());
        statement.bindDouble(8, entity.getAmount());
        statement.bindString(9, entity.getNote());
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__updateAdapterOfAttendanceEntity = new EntityDeletionOrUpdateAdapter<AttendanceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `attendance` SET `id` = ?,`workerId` = ?,`siteId` = ?,`date` = ?,`status` = ?,`overtimeHours` = ?,`overtimeAmount` = ?,`amount` = ?,`note` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AttendanceEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getWorkerId());
        statement.bindLong(3, entity.getSiteId());
        final String _tmp = __converters.fromLocalDate(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.fromAttendanceStatus(entity.getStatus());
        statement.bindString(5, _tmp_1);
        statement.bindDouble(6, entity.getOvertimeHours());
        statement.bindDouble(7, entity.getOvertimeAmount());
        statement.bindDouble(8, entity.getAmount());
        statement.bindString(9, entity.getNote());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getId());
      }
    };
  }

  @Override
  public Object insertAttendance(final AttendanceEntity attendance,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAttendanceEntity.insert(attendance);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAttendance(final AttendanceEntity attendance,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAttendanceEntity.handle(attendance);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AttendanceEntity>> getAllAttendance() {
    final String _sql = "SELECT * FROM attendance ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<List<AttendanceEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOvertimeHours = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeHours");
          final int _cursorIndexOfOvertimeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeAmount");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEntity> _result = new ArrayList<AttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpWorkerId;
            _tmpWorkerId = _cursor.getLong(_cursorIndexOfWorkerId);
            final long _tmpSiteId;
            _tmpSiteId = _cursor.getLong(_cursorIndexOfSiteId);
            final LocalDate _tmpDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_1;
            }
            final AttendanceStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toAttendanceStatus(_tmp_2);
            final double _tmpOvertimeHours;
            _tmpOvertimeHours = _cursor.getDouble(_cursorIndexOfOvertimeHours);
            final double _tmpOvertimeAmount;
            _tmpOvertimeAmount = _cursor.getDouble(_cursorIndexOfOvertimeAmount);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEntity(_tmpId,_tmpWorkerId,_tmpSiteId,_tmpDate,_tmpStatus,_tmpOvertimeHours,_tmpOvertimeAmount,_tmpAmount,_tmpNote,_tmpCreatedAt);
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
  public Flow<List<AttendanceEntity>> getAttendanceByDate(final LocalDate date) {
    final String _sql = "SELECT * FROM attendance WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromLocalDate(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<List<AttendanceEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOvertimeHours = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeHours");
          final int _cursorIndexOfOvertimeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeAmount");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEntity> _result = new ArrayList<AttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpWorkerId;
            _tmpWorkerId = _cursor.getLong(_cursorIndexOfWorkerId);
            final long _tmpSiteId;
            _tmpSiteId = _cursor.getLong(_cursorIndexOfSiteId);
            final LocalDate _tmpDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_2;
            }
            final AttendanceStatus _tmpStatus;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toAttendanceStatus(_tmp_3);
            final double _tmpOvertimeHours;
            _tmpOvertimeHours = _cursor.getDouble(_cursorIndexOfOvertimeHours);
            final double _tmpOvertimeAmount;
            _tmpOvertimeAmount = _cursor.getDouble(_cursorIndexOfOvertimeAmount);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEntity(_tmpId,_tmpWorkerId,_tmpSiteId,_tmpDate,_tmpStatus,_tmpOvertimeHours,_tmpOvertimeAmount,_tmpAmount,_tmpNote,_tmpCreatedAt);
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
  public Flow<List<AttendanceEntity>> getAttendanceBySiteAndDate(final long siteId,
      final LocalDate date) {
    final String _sql = "SELECT * FROM attendance WHERE siteId = ? AND date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, siteId);
    _argIndex = 2;
    final String _tmp = __converters.fromLocalDate(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<List<AttendanceEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOvertimeHours = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeHours");
          final int _cursorIndexOfOvertimeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeAmount");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEntity> _result = new ArrayList<AttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpWorkerId;
            _tmpWorkerId = _cursor.getLong(_cursorIndexOfWorkerId);
            final long _tmpSiteId;
            _tmpSiteId = _cursor.getLong(_cursorIndexOfSiteId);
            final LocalDate _tmpDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_2 = __converters.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_2;
            }
            final AttendanceStatus _tmpStatus;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toAttendanceStatus(_tmp_3);
            final double _tmpOvertimeHours;
            _tmpOvertimeHours = _cursor.getDouble(_cursorIndexOfOvertimeHours);
            final double _tmpOvertimeAmount;
            _tmpOvertimeAmount = _cursor.getDouble(_cursorIndexOfOvertimeAmount);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEntity(_tmpId,_tmpWorkerId,_tmpSiteId,_tmpDate,_tmpStatus,_tmpOvertimeHours,_tmpOvertimeAmount,_tmpAmount,_tmpNote,_tmpCreatedAt);
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
  public Flow<List<AttendanceEntity>> getAttendanceByWorker(final long workerId) {
    final String _sql = "SELECT * FROM attendance WHERE workerId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, workerId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<List<AttendanceEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOvertimeHours = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeHours");
          final int _cursorIndexOfOvertimeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeAmount");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEntity> _result = new ArrayList<AttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpWorkerId;
            _tmpWorkerId = _cursor.getLong(_cursorIndexOfWorkerId);
            final long _tmpSiteId;
            _tmpSiteId = _cursor.getLong(_cursorIndexOfSiteId);
            final LocalDate _tmpDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_1;
            }
            final AttendanceStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toAttendanceStatus(_tmp_2);
            final double _tmpOvertimeHours;
            _tmpOvertimeHours = _cursor.getDouble(_cursorIndexOfOvertimeHours);
            final double _tmpOvertimeAmount;
            _tmpOvertimeAmount = _cursor.getDouble(_cursorIndexOfOvertimeAmount);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEntity(_tmpId,_tmpWorkerId,_tmpSiteId,_tmpDate,_tmpStatus,_tmpOvertimeHours,_tmpOvertimeAmount,_tmpAmount,_tmpNote,_tmpCreatedAt);
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
  public Flow<List<AttendanceEntity>> getAttendanceBySite(final long siteId) {
    final String _sql = "SELECT * FROM attendance WHERE siteId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, siteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<List<AttendanceEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOvertimeHours = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeHours");
          final int _cursorIndexOfOvertimeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeAmount");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEntity> _result = new ArrayList<AttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpWorkerId;
            _tmpWorkerId = _cursor.getLong(_cursorIndexOfWorkerId);
            final long _tmpSiteId;
            _tmpSiteId = _cursor.getLong(_cursorIndexOfSiteId);
            final LocalDate _tmpDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_1;
            }
            final AttendanceStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toAttendanceStatus(_tmp_2);
            final double _tmpOvertimeHours;
            _tmpOvertimeHours = _cursor.getDouble(_cursorIndexOfOvertimeHours);
            final double _tmpOvertimeAmount;
            _tmpOvertimeAmount = _cursor.getDouble(_cursorIndexOfOvertimeAmount);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEntity(_tmpId,_tmpWorkerId,_tmpSiteId,_tmpDate,_tmpStatus,_tmpOvertimeHours,_tmpOvertimeAmount,_tmpAmount,_tmpNote,_tmpCreatedAt);
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
  public Flow<List<AttendanceEntity>> getAttendanceInRange(final LocalDate startDate,
      final LocalDate endDate) {
    final String _sql = "SELECT * FROM attendance WHERE date BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromLocalDate(startDate);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    _argIndex = 2;
    final String _tmp_1 = __converters.fromLocalDate(endDate);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp_1);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<List<AttendanceEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOvertimeHours = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeHours");
          final int _cursorIndexOfOvertimeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeAmount");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEntity> _result = new ArrayList<AttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpWorkerId;
            _tmpWorkerId = _cursor.getLong(_cursorIndexOfWorkerId);
            final long _tmpSiteId;
            _tmpSiteId = _cursor.getLong(_cursorIndexOfSiteId);
            final LocalDate _tmpDate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_3 = __converters.toLocalDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_3;
            }
            final AttendanceStatus _tmpStatus;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toAttendanceStatus(_tmp_4);
            final double _tmpOvertimeHours;
            _tmpOvertimeHours = _cursor.getDouble(_cursorIndexOfOvertimeHours);
            final double _tmpOvertimeAmount;
            _tmpOvertimeAmount = _cursor.getDouble(_cursorIndexOfOvertimeAmount);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEntity(_tmpId,_tmpWorkerId,_tmpSiteId,_tmpDate,_tmpStatus,_tmpOvertimeHours,_tmpOvertimeAmount,_tmpAmount,_tmpNote,_tmpCreatedAt);
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
  public Flow<Double> getTotalEarnedByWorker(final long workerId) {
    final String _sql = "SELECT SUM(amount) FROM attendance WHERE workerId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, workerId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
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
  public Flow<Double> getTotalEarnedBySite(final long siteId) {
    final String _sql = "SELECT SUM(amount) FROM attendance WHERE siteId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, siteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
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
  public Flow<Double> getTodayTotalWage(final LocalDate date) {
    final String _sql = "SELECT SUM(amount) FROM attendance WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromLocalDate(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp_1;
            if (_cursor.isNull(0)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getDouble(0);
            }
            _result = _tmp_1;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

package com.example.workermanager.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.workermanager.data.database.Converters;
import com.example.workermanager.data.entity.SiteAttendanceEntity;
import java.lang.Class;
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
public final class SiteAttendanceDao_Impl implements SiteAttendanceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SiteAttendanceEntity> __insertionAdapterOfSiteAttendanceEntity;

  private final Converters __converters = new Converters();

  public SiteAttendanceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSiteAttendanceEntity = new EntityInsertionAdapter<SiteAttendanceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `site_attendance` (`id`,`siteId`,`date`,`isPresent`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SiteAttendanceEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSiteId());
        final String _tmp = __converters.fromLocalDate(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        final int _tmp_1 = entity.isPresent() ? 1 : 0;
        statement.bindLong(4, _tmp_1);
      }
    };
  }

  @Override
  public Object insertSiteAttendance(final SiteAttendanceEntity siteAttendance,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSiteAttendanceEntity.insert(siteAttendance);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SiteAttendanceEntity>> getSiteAttendance(final long siteId) {
    final String _sql = "SELECT * FROM site_attendance WHERE siteId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, siteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"site_attendance"}, new Callable<List<SiteAttendanceEntity>>() {
      @Override
      @NonNull
      public List<SiteAttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfIsPresent = CursorUtil.getColumnIndexOrThrow(_cursor, "isPresent");
          final List<SiteAttendanceEntity> _result = new ArrayList<SiteAttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SiteAttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
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
            final boolean _tmpIsPresent;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPresent);
            _tmpIsPresent = _tmp_2 != 0;
            _item = new SiteAttendanceEntity(_tmpId,_tmpSiteId,_tmpDate,_tmpIsPresent);
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
  public Object getSiteAttendanceByDate(final long siteId, final LocalDate date,
      final Continuation<? super SiteAttendanceEntity> $completion) {
    final String _sql = "SELECT * FROM site_attendance WHERE siteId = ? AND date = ? LIMIT 1";
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
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SiteAttendanceEntity>() {
      @Override
      @Nullable
      public SiteAttendanceEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfIsPresent = CursorUtil.getColumnIndexOrThrow(_cursor, "isPresent");
          final SiteAttendanceEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
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
            final boolean _tmpIsPresent;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPresent);
            _tmpIsPresent = _tmp_3 != 0;
            _result = new SiteAttendanceEntity(_tmpId,_tmpSiteId,_tmpDate,_tmpIsPresent);
          } else {
            _result = null;
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
  public Flow<List<SiteAttendanceEntity>> getAllSiteAttendanceByDate(final LocalDate date) {
    final String _sql = "SELECT * FROM site_attendance WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromLocalDate(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"site_attendance"}, new Callable<List<SiteAttendanceEntity>>() {
      @Override
      @NonNull
      public List<SiteAttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfIsPresent = CursorUtil.getColumnIndexOrThrow(_cursor, "isPresent");
          final List<SiteAttendanceEntity> _result = new ArrayList<SiteAttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SiteAttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
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
            final boolean _tmpIsPresent;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPresent);
            _tmpIsPresent = _tmp_3 != 0;
            _item = new SiteAttendanceEntity(_tmpId,_tmpSiteId,_tmpDate,_tmpIsPresent);
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
  public Object getAllSiteAttendance(
      final Continuation<? super List<SiteAttendanceEntity>> $completion) {
    final String _sql = "SELECT * FROM site_attendance";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SiteAttendanceEntity>>() {
      @Override
      @NonNull
      public List<SiteAttendanceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSiteId = CursorUtil.getColumnIndexOrThrow(_cursor, "siteId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfIsPresent = CursorUtil.getColumnIndexOrThrow(_cursor, "isPresent");
          final List<SiteAttendanceEntity> _result = new ArrayList<SiteAttendanceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SiteAttendanceEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
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
            final boolean _tmpIsPresent;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsPresent);
            _tmpIsPresent = _tmp_2 != 0;
            _item = new SiteAttendanceEntity(_tmpId,_tmpSiteId,_tmpDate,_tmpIsPresent);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

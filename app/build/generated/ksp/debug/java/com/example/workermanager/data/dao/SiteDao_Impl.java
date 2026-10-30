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
import com.example.workermanager.data.entity.SiteEntity;
import com.example.workermanager.data.entity.SiteStatus;
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
public final class SiteDao_Impl implements SiteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SiteEntity> __insertionAdapterOfSiteEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<SiteEntity> __deletionAdapterOfSiteEntity;

  private final EntityDeletionOrUpdateAdapter<SiteEntity> __updateAdapterOfSiteEntity;

  private final SharedSQLiteStatement __preparedStmtOfSoftDeleteSite;

  private final SharedSQLiteStatement __preparedStmtOfRestoreSite;

  private final SharedSQLiteStatement __preparedStmtOfMarkPendingPermanentDelete;

  private final SharedSQLiteStatement __preparedStmtOfPermanentlyDeleteSite;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySyncId;

  private final SharedSQLiteStatement __preparedStmtOfEmptySiteTrash;

  public SiteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSiteEntity = new EntityInsertionAdapter<SiteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sites` (`id`,`name`,`address`,`manager`,`startDate`,`status`,`isDeleted`,`deletedAt`,`syncOrigin`,`syncId`,`updatedAt`,`isPendingSync`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SiteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getAddress());
        statement.bindString(4, entity.getManager());
        final String _tmp = __converters.fromLocalDate(entity.getStartDate());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp);
        }
        final String _tmp_1 = __converters.fromSiteStatus(entity.getStatus());
        statement.bindString(6, _tmp_1);
        final int _tmp_2 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getDeletedAt());
        }
        statement.bindLong(9, entity.getSyncOrigin());
        statement.bindString(10, entity.getSyncId());
        statement.bindLong(11, entity.getUpdatedAt());
        final int _tmp_3 = entity.isPendingSync() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
      }
    };
    this.__deletionAdapterOfSiteEntity = new EntityDeletionOrUpdateAdapter<SiteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sites` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SiteEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSiteEntity = new EntityDeletionOrUpdateAdapter<SiteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sites` SET `id` = ?,`name` = ?,`address` = ?,`manager` = ?,`startDate` = ?,`status` = ?,`isDeleted` = ?,`deletedAt` = ?,`syncOrigin` = ?,`syncId` = ?,`updatedAt` = ?,`isPendingSync` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SiteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getAddress());
        statement.bindString(4, entity.getManager());
        final String _tmp = __converters.fromLocalDate(entity.getStartDate());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp);
        }
        final String _tmp_1 = __converters.fromSiteStatus(entity.getStatus());
        statement.bindString(6, _tmp_1);
        final int _tmp_2 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getDeletedAt());
        }
        statement.bindLong(9, entity.getSyncOrigin());
        statement.bindString(10, entity.getSyncId());
        statement.bindLong(11, entity.getUpdatedAt());
        final int _tmp_3 = entity.isPendingSync() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfSoftDeleteSite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sites SET isDeleted = 1, deletedAt = ?, updatedAt = ?, isPendingSync = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRestoreSite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sites SET isDeleted = 0, deletedAt = NULL, updatedAt = ?, isPendingSync = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkPendingPermanentDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sites SET deletedAt = -1, updatedAt = ?, isPendingSync = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfPermanentlyDeleteSite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sites WHERE id = ? AND isDeleted = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBySyncId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sites WHERE syncId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfEmptySiteTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sites WHERE isDeleted = 1 AND syncOrigin = 0";
        return _query;
      }
    };
  }

  @Override
  public Object insertSite(final SiteEntity site, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSiteEntity.insertAndReturnId(site);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSite(final SiteEntity site, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSiteEntity.handle(site);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSite(final SiteEntity site, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSiteEntity.handle(site);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object softDeleteSite(final long id, final long deletedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDeleteSite.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, deletedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, deletedAt);
        _argIndex = 3;
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
          __preparedStmtOfSoftDeleteSite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object restoreSite(final long id, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRestoreSite.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, updatedAt);
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
          __preparedStmtOfRestoreSite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markPendingPermanentDelete(final long id, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkPendingPermanentDelete.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, updatedAt);
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
          __preparedStmtOfMarkPendingPermanentDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object permanentlyDeleteSite(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPermanentlyDeleteSite.acquire();
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
          __preparedStmtOfPermanentlyDeleteSite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBySyncId(final String syncId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySyncId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, syncId);
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
          __preparedStmtOfDeleteBySyncId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object emptySiteTrash(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEmptySiteTrash.acquire();
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
          __preparedStmtOfEmptySiteTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SiteEntity>> getAllSites() {
    final String _sql = "SELECT * FROM sites WHERE isDeleted = 0 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sites"}, new Callable<List<SiteEntity>>() {
      @Override
      @NonNull
      public List<SiteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<SiteEntity> _result = new ArrayList<SiteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SiteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpManager;
            _tmpManager = _cursor.getString(_cursorIndexOfManager);
            final LocalDate _tmpStartDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_1;
            }
            final SiteStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toSiteStatus(_tmp_2);
            final boolean _tmpIsDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_3 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_4 != 0;
            _item = new SiteEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpStartDate,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Object getAllSitesRaw(final Continuation<? super List<SiteEntity>> $completion) {
    final String _sql = "SELECT * FROM sites";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SiteEntity>>() {
      @Override
      @NonNull
      public List<SiteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<SiteEntity> _result = new ArrayList<SiteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SiteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpManager;
            _tmpManager = _cursor.getString(_cursorIndexOfManager);
            final LocalDate _tmpStartDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_1;
            }
            final SiteStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toSiteStatus(_tmp_2);
            final boolean _tmpIsDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_3 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_4 != 0;
            _item = new SiteEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpStartDate,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Flow<SiteEntity> getSiteById(final long siteId) {
    final String _sql = "SELECT * FROM sites WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, siteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sites"}, new Callable<SiteEntity>() {
      @Override
      @Nullable
      public SiteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final SiteEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpManager;
            _tmpManager = _cursor.getString(_cursorIndexOfManager);
            final LocalDate _tmpStartDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_1;
            }
            final SiteStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toSiteStatus(_tmp_2);
            final boolean _tmpIsDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_3 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_4 != 0;
            _result = new SiteEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpStartDate,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Object getSiteBySyncId(final String syncId,
      final Continuation<? super SiteEntity> $completion) {
    final String _sql = "SELECT * FROM sites WHERE syncId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, syncId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SiteEntity>() {
      @Override
      @Nullable
      public SiteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final SiteEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpManager;
            _tmpManager = _cursor.getString(_cursorIndexOfManager);
            final LocalDate _tmpStartDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_1;
            }
            final SiteStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toSiteStatus(_tmp_2);
            final boolean _tmpIsDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_3 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_4 != 0;
            _result = new SiteEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpStartDate,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Object getAllSitesList(final Continuation<? super List<SiteEntity>> $completion) {
    final String _sql = "SELECT * FROM sites WHERE isDeleted = 0 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SiteEntity>>() {
      @Override
      @NonNull
      public List<SiteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<SiteEntity> _result = new ArrayList<SiteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SiteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpManager;
            _tmpManager = _cursor.getString(_cursorIndexOfManager);
            final LocalDate _tmpStartDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_1;
            }
            final SiteStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toSiteStatus(_tmp_2);
            final boolean _tmpIsDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_3 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_4 != 0;
            _item = new SiteEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpStartDate,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Object getSiteByName(final String name,
      final Continuation<? super SiteEntity> $completion) {
    final String _sql = "SELECT * FROM sites WHERE name = ? AND isDeleted = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, name);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SiteEntity>() {
      @Override
      @Nullable
      public SiteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final SiteEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpManager;
            _tmpManager = _cursor.getString(_cursorIndexOfManager);
            final LocalDate _tmpStartDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_1;
            }
            final SiteStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toSiteStatus(_tmp_2);
            final boolean _tmpIsDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_3 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_4 != 0;
            _result = new SiteEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpStartDate,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Flow<List<SiteEntity>> getDeletedSites() {
    final String _sql = "SELECT * FROM sites WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sites"}, new Callable<List<SiteEntity>>() {
      @Override
      @NonNull
      public List<SiteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<SiteEntity> _result = new ArrayList<SiteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SiteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpManager;
            _tmpManager = _cursor.getString(_cursorIndexOfManager);
            final LocalDate _tmpStartDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_1;
            }
            final SiteStatus _tmpStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toSiteStatus(_tmp_2);
            final boolean _tmpIsDeleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_3 != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_4 != 0;
            _item = new SiteEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpStartDate,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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

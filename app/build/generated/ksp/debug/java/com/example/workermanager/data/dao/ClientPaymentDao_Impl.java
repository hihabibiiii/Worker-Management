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
import com.example.workermanager.data.entity.ClientPaymentEntity;
import com.example.workermanager.data.entity.ClientPaymentMethod;
import java.lang.Class;
import java.lang.Double;
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
public final class ClientPaymentDao_Impl implements ClientPaymentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ClientPaymentEntity> __insertionAdapterOfClientPaymentEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<ClientPaymentEntity> __deletionAdapterOfClientPaymentEntity;

  private final EntityDeletionOrUpdateAdapter<ClientPaymentEntity> __updateAdapterOfClientPaymentEntity;

  private final SharedSQLiteStatement __preparedStmtOfSoftDeleteClientPayment;

  private final SharedSQLiteStatement __preparedStmtOfRestoreClientPayment;

  private final SharedSQLiteStatement __preparedStmtOfMarkPendingPermanentDelete;

  private final SharedSQLiteStatement __preparedStmtOfPermanentlyDeleteClientPayment;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySyncId;

  private final SharedSQLiteStatement __preparedStmtOfEmptyClientPaymentTrash;

  public ClientPaymentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfClientPaymentEntity = new EntityInsertionAdapter<ClientPaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `client_payments` (`id`,`contractId`,`amount`,`paymentDate`,`method`,`note`,`createdAt`,`isDeleted`,`deletedAt`,`isEdited`,`syncOrigin`,`syncId`,`updatedAt`,`isPendingSync`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ClientPaymentEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getContractId());
        statement.bindDouble(3, entity.getAmount());
        final String _tmp = __converters.fromLocalDate(entity.getPaymentDate());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.fromClientPaymentMethod(entity.getMethod());
        statement.bindString(5, _tmp_1);
        statement.bindString(6, entity.getNote());
        statement.bindLong(7, entity.getCreatedAt());
        final int _tmp_2 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getDeletedAt());
        }
        final int _tmp_3 = entity.isEdited() ? 1 : 0;
        statement.bindLong(10, _tmp_3);
        statement.bindLong(11, entity.getSyncOrigin());
        statement.bindString(12, entity.getSyncId());
        statement.bindLong(13, entity.getUpdatedAt());
        final int _tmp_4 = entity.isPendingSync() ? 1 : 0;
        statement.bindLong(14, _tmp_4);
      }
    };
    this.__deletionAdapterOfClientPaymentEntity = new EntityDeletionOrUpdateAdapter<ClientPaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `client_payments` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ClientPaymentEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfClientPaymentEntity = new EntityDeletionOrUpdateAdapter<ClientPaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `client_payments` SET `id` = ?,`contractId` = ?,`amount` = ?,`paymentDate` = ?,`method` = ?,`note` = ?,`createdAt` = ?,`isDeleted` = ?,`deletedAt` = ?,`isEdited` = ?,`syncOrigin` = ?,`syncId` = ?,`updatedAt` = ?,`isPendingSync` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ClientPaymentEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getContractId());
        statement.bindDouble(3, entity.getAmount());
        final String _tmp = __converters.fromLocalDate(entity.getPaymentDate());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.fromClientPaymentMethod(entity.getMethod());
        statement.bindString(5, _tmp_1);
        statement.bindString(6, entity.getNote());
        statement.bindLong(7, entity.getCreatedAt());
        final int _tmp_2 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getDeletedAt());
        }
        final int _tmp_3 = entity.isEdited() ? 1 : 0;
        statement.bindLong(10, _tmp_3);
        statement.bindLong(11, entity.getSyncOrigin());
        statement.bindString(12, entity.getSyncId());
        statement.bindLong(13, entity.getUpdatedAt());
        final int _tmp_4 = entity.isPendingSync() ? 1 : 0;
        statement.bindLong(14, _tmp_4);
        statement.bindLong(15, entity.getId());
      }
    };
    this.__preparedStmtOfSoftDeleteClientPayment = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE client_payments SET isDeleted = 1, deletedAt = ?, updatedAt = ?, isPendingSync = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRestoreClientPayment = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE client_payments SET isDeleted = 0, deletedAt = NULL, updatedAt = ?, isPendingSync = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkPendingPermanentDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE client_payments SET deletedAt = -1, updatedAt = ?, isPendingSync = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfPermanentlyDeleteClientPayment = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM client_payments WHERE id = ? AND isDeleted = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBySyncId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM client_payments WHERE syncId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfEmptyClientPaymentTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM client_payments WHERE isDeleted = 1 AND syncOrigin = 0";
        return _query;
      }
    };
  }

  @Override
  public Object insertPayment(final ClientPaymentEntity payment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfClientPaymentEntity.insert(payment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePayment(final ClientPaymentEntity payment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfClientPaymentEntity.handle(payment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePayment(final ClientPaymentEntity payment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfClientPaymentEntity.handle(payment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object softDeleteClientPayment(final long id, final long deletedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDeleteClientPayment.acquire();
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
          __preparedStmtOfSoftDeleteClientPayment.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object restoreClientPayment(final long id, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRestoreClientPayment.acquire();
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
          __preparedStmtOfRestoreClientPayment.release(_stmt);
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
  public Object permanentlyDeleteClientPayment(final long id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPermanentlyDeleteClientPayment.acquire();
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
          __preparedStmtOfPermanentlyDeleteClientPayment.release(_stmt);
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
  public Object emptyClientPaymentTrash(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEmptyClientPaymentTrash.acquire();
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
          __preparedStmtOfEmptyClientPaymentTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ClientPaymentEntity>> getPaymentsByContract(final long contractId) {
    final String _sql = "SELECT * FROM client_payments WHERE contractId = ? AND isDeleted = 0 ORDER BY paymentDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, contractId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"client_payments"}, new Callable<List<ClientPaymentEntity>>() {
      @Override
      @NonNull
      public List<ClientPaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContractId = CursorUtil.getColumnIndexOrThrow(_cursor, "contractId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPaymentDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentDate");
          final int _cursorIndexOfMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "method");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<ClientPaymentEntity> _result = new ArrayList<ClientPaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ClientPaymentEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpContractId;
            _tmpContractId = _cursor.getLong(_cursorIndexOfContractId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final LocalDate _tmpPaymentDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPaymentDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPaymentDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpPaymentDate = _tmp_1;
            }
            final ClientPaymentMethod _tmpMethod;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfMethod);
            _tmpMethod = __converters.toClientPaymentMethod(_tmp_2);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
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
            final boolean _tmpIsEdited;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp_4 != 0;
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_5 != 0;
            _item = new ClientPaymentEntity(_tmpId,_tmpContractId,_tmpAmount,_tmpPaymentDate,_tmpMethod,_tmpNote,_tmpCreatedAt,_tmpIsDeleted,_tmpDeletedAt,_tmpIsEdited,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Object getAllClientPaymentsRaw(
      final Continuation<? super List<ClientPaymentEntity>> $completion) {
    final String _sql = "SELECT * FROM client_payments";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ClientPaymentEntity>>() {
      @Override
      @NonNull
      public List<ClientPaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContractId = CursorUtil.getColumnIndexOrThrow(_cursor, "contractId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPaymentDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentDate");
          final int _cursorIndexOfMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "method");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<ClientPaymentEntity> _result = new ArrayList<ClientPaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ClientPaymentEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpContractId;
            _tmpContractId = _cursor.getLong(_cursorIndexOfContractId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final LocalDate _tmpPaymentDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPaymentDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPaymentDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpPaymentDate = _tmp_1;
            }
            final ClientPaymentMethod _tmpMethod;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfMethod);
            _tmpMethod = __converters.toClientPaymentMethod(_tmp_2);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
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
            final boolean _tmpIsEdited;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp_4 != 0;
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_5 != 0;
            _item = new ClientPaymentEntity(_tmpId,_tmpContractId,_tmpAmount,_tmpPaymentDate,_tmpMethod,_tmpNote,_tmpCreatedAt,_tmpIsDeleted,_tmpDeletedAt,_tmpIsEdited,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Object getClientPaymentBySyncId(final String syncId,
      final Continuation<? super ClientPaymentEntity> $completion) {
    final String _sql = "SELECT * FROM client_payments WHERE syncId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, syncId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ClientPaymentEntity>() {
      @Override
      @Nullable
      public ClientPaymentEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContractId = CursorUtil.getColumnIndexOrThrow(_cursor, "contractId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPaymentDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentDate");
          final int _cursorIndexOfMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "method");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final ClientPaymentEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpContractId;
            _tmpContractId = _cursor.getLong(_cursorIndexOfContractId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final LocalDate _tmpPaymentDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPaymentDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPaymentDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpPaymentDate = _tmp_1;
            }
            final ClientPaymentMethod _tmpMethod;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfMethod);
            _tmpMethod = __converters.toClientPaymentMethod(_tmp_2);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
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
            final boolean _tmpIsEdited;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp_4 != 0;
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_5 != 0;
            _result = new ClientPaymentEntity(_tmpId,_tmpContractId,_tmpAmount,_tmpPaymentDate,_tmpMethod,_tmpNote,_tmpCreatedAt,_tmpIsDeleted,_tmpDeletedAt,_tmpIsEdited,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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
  public Flow<Double> getTotalReceivedByContract(final long contractId) {
    final String _sql = "SELECT SUM(amount) FROM client_payments WHERE contractId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, contractId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"client_payments"}, new Callable<Double>() {
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
  public Flow<Double> getTotalReceivedAll() {
    final String _sql = "SELECT SUM(amount) FROM client_payments WHERE isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"client_payments"}, new Callable<Double>() {
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
  public Flow<List<ClientPaymentEntity>> getDeletedClientPayments() {
    final String _sql = "SELECT * FROM client_payments WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"client_payments"}, new Callable<List<ClientPaymentEntity>>() {
      @Override
      @NonNull
      public List<ClientPaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContractId = CursorUtil.getColumnIndexOrThrow(_cursor, "contractId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPaymentDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentDate");
          final int _cursorIndexOfMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "method");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfSyncOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "syncOrigin");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "syncId");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<ClientPaymentEntity> _result = new ArrayList<ClientPaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ClientPaymentEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpContractId;
            _tmpContractId = _cursor.getLong(_cursorIndexOfContractId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final LocalDate _tmpPaymentDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPaymentDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPaymentDate);
            }
            final LocalDate _tmp_1 = __converters.toLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpPaymentDate = _tmp_1;
            }
            final ClientPaymentMethod _tmpMethod;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfMethod);
            _tmpMethod = __converters.toClientPaymentMethod(_tmp_2);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
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
            final boolean _tmpIsEdited;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp_4 != 0;
            final int _tmpSyncOrigin;
            _tmpSyncOrigin = _cursor.getInt(_cursorIndexOfSyncOrigin);
            final String _tmpSyncId;
            _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsPendingSync;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_5 != 0;
            _item = new ClientPaymentEntity(_tmpId,_tmpContractId,_tmpAmount,_tmpPaymentDate,_tmpMethod,_tmpNote,_tmpCreatedAt,_tmpIsDeleted,_tmpDeletedAt,_tmpIsEdited,_tmpSyncOrigin,_tmpSyncId,_tmpUpdatedAt,_tmpIsPendingSync);
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

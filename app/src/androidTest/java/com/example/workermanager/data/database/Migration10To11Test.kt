package com.example.workermanager.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration10To11Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate10To11_preservesAllExistingRecordsAndSetsLegacySyncOrigin() {
        var db = helper.createDatabase(TEST_DB, 10).apply {
            execSQL("""
                INSERT INTO sites (id, name, address, manager, startDate, status, isDeleted, deletedAt)
                VALUES (42, 'Site Alpha', '123 Main St', 'Manager John', '2024-01-15', 'ACTIVE', 0, NULL)
            """)
            execSQL("""
                INSERT INTO workers (id, name, phone, workerType, dailyRate, overtimeRate, joiningDate, currentSiteId, isActive, notes, photoPath, isDeleted, deletedAt)
                VALUES (101, 'Worker Bob', '9876543210', 'LABOUR', 500.0, 100.0, '2024-01-15', 42, 1, 'Good worker', NULL, 0, NULL)
            """)
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 11, true, AppDatabase.MIGRATION_10_11)

        val siteCursor = db.query("SELECT * FROM sites WHERE id = 42")
        assertTrue(siteCursor.moveToFirst())
        assertEquals(42L, siteCursor.getLong(siteCursor.getColumnIndexOrThrow("id")))
        assertEquals("Site Alpha", siteCursor.getString(siteCursor.getColumnIndexOrThrow("name")))
        assertEquals(0, siteCursor.getInt(siteCursor.getColumnIndexOrThrow("syncOrigin")))
        assertEquals("", siteCursor.getString(siteCursor.getColumnIndexOrThrow("syncId")))
        assertEquals(0L, siteCursor.getLong(siteCursor.getColumnIndexOrThrow("updatedAt")))
        assertEquals(0, siteCursor.getInt(siteCursor.getColumnIndexOrThrow("isPendingSync")))
        siteCursor.close()

        val workerCursor = db.query("SELECT * FROM workers WHERE id = 101")
        assertTrue(workerCursor.moveToFirst())
        assertEquals(101L, workerCursor.getLong(workerCursor.getColumnIndexOrThrow("id")))
        assertEquals("Worker Bob", workerCursor.getString(workerCursor.getColumnIndexOrThrow("name")))
        assertEquals(0, workerCursor.getInt(workerCursor.getColumnIndexOrThrow("syncOrigin")))
        assertEquals("", workerCursor.getString(workerCursor.getColumnIndexOrThrow("syncId")))
        assertEquals(0L, workerCursor.getLong(workerCursor.getColumnIndexOrThrow("updatedAt")))
        assertEquals(0, workerCursor.getInt(workerCursor.getColumnIndexOrThrow("isPendingSync")))
        workerCursor.close()
    }
}

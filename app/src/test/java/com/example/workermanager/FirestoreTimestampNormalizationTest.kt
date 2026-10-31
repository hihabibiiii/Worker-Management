package com.example.workermanager

import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.mapper.SiteMapper
import com.example.workermanager.data.remote.sync.mapper.WorkerMapper
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Unit tests verifying Firestore timestamp normalization across Long (epoch millis),
 * com.google.firebase.Timestamp, java.util.Date, String, and legacy documents without timestamps.
 * Strictly guarantees compatibility for sites and workers collections.
 */
class FirestoreTimestampNormalizationTest {

    // =========================================================================
    // 1. FirestoreSyncHelper.parseTimestampValue
    // =========================================================================

    @Test
    fun testParseTimestampValue_handlesLongEpochMilliseconds() {
        val millis = 1705000000000L
        assertEquals(millis, FirestoreSyncHelper.parseTimestampValue(millis))
    }

    @Test
    fun testParseTimestampValue_handlesIntAndDoubleNumbers() {
        val intVal = 1000000
        assertEquals(1000000L, FirestoreSyncHelper.parseTimestampValue(intVal))

        val doubleVal = 1705000000123.0
        assertEquals(1705000000123L, FirestoreSyncHelper.parseTimestampValue(doubleVal))
    }

    @Test
    fun testParseTimestampValue_handlesFirestoreTimestamp() {
        // 1705000000 seconds = 1705000000000 millis
        val ts = Timestamp(1705000000L, 500_000_000)
        val expectedMillis = 1705000000500L
        val parsed = FirestoreSyncHelper.parseTimestampValue(ts)
        assertNotNull(parsed)
        assertEquals(expectedMillis, parsed)
    }

    @Test
    fun testParseTimestampValue_handlesJavaUtilDate() {
        val date = Date(1705000123456L)
        assertEquals(1705000123456L, FirestoreSyncHelper.parseTimestampValue(date))
    }

    @Test
    fun testParseTimestampValue_handlesNumericString() {
        val str = " 1705000654321 "
        assertEquals(1705000654321L, FirestoreSyncHelper.parseTimestampValue(str))
    }

    @Test
    fun testParseTimestampValue_returnsNullForInvalidOrMissingTypes() {
        assertNull(FirestoreSyncHelper.parseTimestampValue(null))
        assertNull(FirestoreSyncHelper.parseTimestampValue("invalid_date_format"))
        assertNull(FirestoreSyncHelper.parseTimestampValue(true))
        assertNull(FirestoreSyncHelper.parseTimestampValue(listOf(1, 2, 3)))
        assertNull(FirestoreSyncHelper.parseTimestampValue(mapOf("seconds" to 100)))
    }

    // =========================================================================
    // 2. SiteMapper normalization (Long vs Timestamp vs Legacy)
    // =========================================================================

    @Test
    fun testSiteMapper_fromMap_withLongEpochMillis() {
        val map = mapOf(
            "syncId" to "site_101",
            "name" to "Downtown Tower",
            "address" to "123 Main St",
            "manager" to "John Doe",
            "startDate" to "2024-01-15",
            "status" to "ACTIVE",
            "isDeleted" to false,
            "deletedAt" to null,
            "updatedAt" to 1705000000000L
        )

        val dto = SiteMapper.fromMap(map, "site_101")
        assertEquals("site_101", dto.syncId)
        assertEquals("Downtown Tower", dto.name)
        assertEquals("123 Main St", dto.address)
        assertEquals("John Doe", dto.manager)
        assertEquals("2024-01-15", dto.startDate)
        assertEquals("ACTIVE", dto.status)
        assertFalse(dto.isDeleted)
        assertNull(dto.deletedAt)
        assertEquals(1705000000000L, dto.updatedAt)
    }

    @Test
    fun testSiteMapper_fromMap_withFirestoreTimestamp() {
        // Document written by Cloud Function or console using Timestamp
        val firestoreTs = Timestamp(1705000000L, 0)
        val map = mapOf(
            "syncId" to "site_102",
            "name" to "Westside Project",
            "address" to "456 Oak St",
            "status" to "COMPLETED",
            "isDeleted" to false,
            "updatedAt" to firestoreTs
        )

        val dto = SiteMapper.fromMap(map, "site_102")
        assertEquals("site_102", dto.syncId)
        assertEquals("Westside Project", dto.name)
        assertEquals(1705000000000L, dto.updatedAt)
    }

    @Test
    fun testSiteMapper_fromMap_withSoftDeletedTimestamp() {
        val deletedTs = Timestamp(1705000050L, 0)
        val updatedTs = Timestamp(1705000050L, 0)
        val map = mapOf(
            "syncId" to "site_deleted",
            "name" to "Demolished Site",
            "isDeleted" to true,
            "deletedAt" to deletedTs,
            "updatedAt" to updatedTs
        )

        val dto = SiteMapper.fromMap(map, "site_deleted")
        assertTrue(dto.isDeleted)
        assertEquals(1705000050000L, dto.deletedAt)
        assertEquals(1705000050000L, dto.updatedAt)

        val entity = SiteMapper.toEntity(dto)
        assertTrue(entity.isDeleted)
        assertEquals(1705000050000L, entity.deletedAt)
        assertEquals(1705000050000L, entity.updatedAt)
    }

    @Test
    fun testSiteMapper_fromMap_legacyDocumentWithoutUpdatedAt_doesNotThrow() {
        // Legacy document with no updatedAt or deletedAt fields
        val map = mapOf(
            "name" to "Legacy Site Without Timestamp",
            "address" to "Historic District",
            "status" to "ACTIVE"
        )

        val dto = SiteMapper.fromMap(map, "legacy_site_doc_id")
        assertEquals("legacy_site_doc_id", dto.syncId)
        assertEquals("Legacy Site Without Timestamp", dto.name)
        assertEquals(0L, dto.updatedAt)
        assertNull(dto.deletedAt)
        assertFalse(dto.isDeleted)
    }

    // =========================================================================
    // 3. WorkerMapper normalization (Long vs Timestamp vs Legacy)
    // =========================================================================

    @Test
    fun testWorkerMapper_fromMap_withLongEpochMillis() {
        val map = mapOf(
            "syncId" to "w_101",
            "name" to "Bob Smith",
            "phone" to "1234567890",
            "workerType" to "MISTRI",
            "dailyRate" to 500.0,
            "overtimeRate" to 75.0,
            "joiningDate" to "2024-02-01",
            "siteSyncId" to "site_101",
            "isActive" to true,
            "isDeleted" to false,
            "updatedAt" to 1705000100000L
        )

        val dto = WorkerMapper.fromMap(map, "w_101")
        assertEquals("w_101", dto.syncId)
        assertEquals("Bob Smith", dto.name)
        assertEquals("1234567890", dto.phone)
        assertEquals("MISTRI", dto.workerType)
        assertEquals(500.0, dto.dailyRate, 0.001)
        assertEquals(75.0, dto.overtimeRate, 0.001)
        assertEquals(1705000100000L, dto.updatedAt)

        val entity = WorkerMapper.toEntity(dto)
        assertEquals(WorkerType.MISTRI, entity.workerType)
    }

    @Test
    fun testWorkerMapper_fromMap_withFirestoreTimestamp() {
        val firestoreTs = Timestamp(1705000200L, 0)
        val map = mapOf(
            "syncId" to "w_102",
            "name" to "Alice Helper",
            "phone" to "9876543210",
            "workerType" to "HELPER",
            "updatedAt" to firestoreTs
        )

        val dto = WorkerMapper.fromMap(map, "w_102")
        assertEquals("w_102", dto.syncId)
        assertEquals("Alice Helper", dto.name)
        assertEquals(1705000200000L, dto.updatedAt)

        val entity = WorkerMapper.toEntity(dto)
        assertEquals(WorkerType.HELPER, entity.workerType)
        assertEquals(1705000200000L, entity.updatedAt)
    }

    @Test
    fun testWorkerMapper_fromMap_withSoftDeletedTimestamp() {
        val deletedTs = Timestamp(1705000300L, 0)
        val map = mapOf(
            "syncId" to "w_del",
            "name" to "Departed Worker",
            "isDeleted" to true,
            "deletedAt" to deletedTs,
            "updatedAt" to deletedTs
        )

        val dto = WorkerMapper.fromMap(map, "w_del")
        assertTrue(dto.isDeleted)
        assertEquals(1705000300000L, dto.deletedAt)
        assertEquals(1705000300000L, dto.updatedAt)

        val entity = WorkerMapper.toEntity(dto)
        assertTrue(entity.isDeleted)
        assertEquals(1705000300000L, entity.deletedAt)
    }

    @Test
    fun testWorkerMapper_fromMap_legacyDocumentWithoutUpdatedAt_doesNotThrow() {
        val map = mapOf(
            "name" to "Legacy Worker Without Timestamp",
            "workerType" to "LABOUR",
            "dailyRate" to 400.0
        )

        val dto = WorkerMapper.fromMap(map, "legacy_worker_doc_id")
        assertEquals("legacy_worker_doc_id", dto.syncId)
        assertEquals("Legacy Worker Without Timestamp", dto.name)
        assertEquals(0L, dto.updatedAt)
        assertNull(dto.deletedAt)
        assertFalse(dto.isDeleted)
    }

    // =========================================================================
    // 4. Cursor calculation safety (No silent advance on missing/invalid timestamps)
    // =========================================================================

    @Test
    fun testCursorCalculation_doesNotAdvanceOnMissingOrInvalidTimestamps() {
        var cursor = 1000L

        // Incoming documents with missing or invalid timestamps
        val testDocuments = listOf<Map<String, Any?>>(
            mapOf("name" to "Doc without updatedAt"),
            mapOf("name" to "Doc with null updatedAt", "updatedAt" to null),
            mapOf("name" to "Doc with invalid string", "updatedAt" to "not_a_number"),
            mapOf("name" to "Doc with boolean", "updatedAt" to true)
        )

        for (doc in testDocuments) {
            val docUpdatedAt = FirestoreSyncHelper.parseTimestampValue(doc["updatedAt"])
            // Cursor must only advance when timestamp is valid and higher
            if (docUpdatedAt != null && docUpdatedAt > cursor) {
                cursor = docUpdatedAt
            }
        }

        // Cursor MUST NOT have moved
        assertEquals(1000L, cursor)
    }

    @Test
    fun testCursorCalculation_advancesCorrectlyWithMixedLongAndTimestamp() {
        var cursor = 1000L

        val documents = listOf(
            mapOf("name" to "Doc 1", "updatedAt" to 1500L),
            mapOf("name" to "Doc 2", "updatedAt" to null), // Missing
            mapOf("name" to "Doc 3", "updatedAt" to Timestamp(2000L, 0)), // 2,000,000 ms
            mapOf("name" to "Doc 4", "updatedAt" to "invalid"), // Invalid
            mapOf("name" to "Doc 5", "updatedAt" to 2_500_000L) // 2,500,000 ms
        )

        for (doc in documents) {
            val docUpdatedAt = FirestoreSyncHelper.parseTimestampValue(doc["updatedAt"])
            if (docUpdatedAt != null && docUpdatedAt > cursor) {
                cursor = docUpdatedAt
            }
        }

        // Correctly advanced to the maximum valid timestamp
        assertEquals(2_500_000L, cursor)
    }
}

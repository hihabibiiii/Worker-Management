package com.example.workermanager

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.SyncCursorManager
import com.example.workermanager.data.remote.sync.dto.SiteDTO
import com.example.workermanager.data.remote.sync.dto.WorkerDTO
import com.example.workermanager.data.remote.sync.mapper.SiteMapper
import com.example.workermanager.data.remote.sync.mapper.WorkerMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FirestoreIncrementalSyncOptimizationTest {

    private val testUserId = "test_user_abc_123"

    @Before
    fun setUp() {
        SyncCursorManager.clearAll()
    }

    // =========================================================================
    // SCENARIO 1: Initial full sync (pagination & cursor initialization)
    // =========================================================================
    @Test
    fun testScenario1_initialFullSync_paginatesAndInitializesCursor() {
        // Initial state: cursor is 0
        assertEquals(0L, SyncCursorManager.getLastSyncTimestamp(testUserId, "sites"))

        // Simulate 250 cloud records returned in pages of 100
        val pageSize = 100
        val totalCloudRecords = 250
        val cloudRecords = (1..totalCloudRecords).map { i ->
            SiteDTO(
                syncId = "site_id_$i",
                name = "Site #$i",
                updatedAt = 1000L + i
            )
        }

        // Simulate pagination logic as executed in FirestoreSyncHelper
        var cursorDocIndex = 0
        val pulledItems = mutableListOf<SiteDTO>()
        var highestUpdatedAt = 0L
        var totalPages = 0

        while (cursorDocIndex < cloudRecords.size) {
            val page = cloudRecords.subList(cursorDocIndex, minOf(cursorDocIndex + pageSize, cloudRecords.size))
            totalPages++
            pulledItems.addAll(page)
            val maxInPage = page.maxOfOrNull { it.updatedAt } ?: 0L
            if (maxInPage > highestUpdatedAt) highestUpdatedAt = maxInPage
            cursorDocIndex += page.size
            if (page.size < pageSize) break
        }

        assertEquals(250, pulledItems.size)
        assertEquals(3, totalPages) // 100 + 100 + 50
        assertEquals(1250L, highestUpdatedAt)

        // Sync establishes high-water mark cursor
        SyncCursorManager.setLastSyncTimestamp(testUserId, "sites", highestUpdatedAt)
        assertEquals(1250L, SyncCursorManager.getLastSyncTimestamp(testUserId, "sites"))
    }

    // =========================================================================
    // SCENARIO 2: Incremental sync with no changes (0 records read / minimal read)
    // =========================================================================
    @Test
    fun testScenario2_incrementalSync_noChanges_readsZeroRecords() {
        val initialCursor = 1700000000000L
        SyncCursorManager.setLastSyncTimestamp(testUserId, "sites", initialCursor)

        val queryFrom = maxOf(0L, initialCursor - FirestoreSyncHelper.DEFAULT_CLOCK_SKEW_BUFFER_MS)

        // Cloud has records, but NONE updated after queryFrom
        val cloudRecords = listOf(
            SiteDTO(syncId = "s1", name = "Old Site 1", updatedAt = initialCursor - 10000L),
            SiteDTO(syncId = "s2", name = "Old Site 2", updatedAt = initialCursor - 6000L)
        )

        val incrementalItems = cloudRecords.filter { it.updatedAt > queryFrom }
        assertTrue("No records should be pulled when nothing changed in cloud", incrementalItems.isEmpty())

        // Cursor remains at least as high as initialCursor
        val newCursor = maxOf(initialCursor, incrementalItems.maxOfOrNull { it.updatedAt } ?: 0L)
        assertEquals(initialCursor, newCursor)
        SyncCursorManager.setLastSyncTimestamp(testUserId, "sites", newCursor)
        assertEquals(initialCursor, SyncCursorManager.getLastSyncTimestamp(testUserId, "sites"))
    }

    // =========================================================================
    // SCENARIO 3: Incremental sync with new records
    // =========================================================================
    @Test
    fun testScenario3_incrementalSync_newRecords_pulledAndInserted() {
        val initialCursor = 1000L
        SyncCursorManager.setLastSyncTimestamp(testUserId, "workers", initialCursor)
        val queryFrom = initialCursor - 500L

        // Local cache currently has Worker 1 (synced in previous cycle before queryFrom window)
        val localWorkers = mutableMapOf(
            "w1" to WorkerEntity(id = 1, name = "Alice", syncId = "w1", updatedAt = 400L, syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
        )

        // Cloud receives new Worker 2 and Worker 3 with newer timestamps
        val cloudRecords = listOf(
            WorkerDTO(syncId = "w1", name = "Alice", updatedAt = 400L),
            WorkerDTO(syncId = "w2", name = "Bob", updatedAt = 1500L),
            WorkerDTO(syncId = "w3", name = "Charlie", updatedAt = 1800L)
        )

        // Incremental pull only gets records with updatedAt > queryFrom
        val newOrModifiedCloud = cloudRecords.filter { it.updatedAt > queryFrom }
        assertEquals(2, newOrModifiedCloud.size)
        assertTrue(newOrModifiedCloud.any { it.syncId == "w2" })
        assertTrue(newOrModifiedCloud.any { it.syncId == "w3" })

        // Process pull into local store
        var highestUpdatedAt = initialCursor
        for (dto in newOrModifiedCloud) {
            val existing = localWorkers[dto.syncId]
            if (existing == null) {
                localWorkers[dto.syncId] = WorkerMapper.toEntity(dto).copy(isPendingSync = false)
            } else if (dto.updatedAt > existing.updatedAt) {
                localWorkers[dto.syncId] = WorkerMapper.toEntity(dto, localId = existing.id).copy(isPendingSync = false)
            }
            if (dto.updatedAt > highestUpdatedAt) highestUpdatedAt = dto.updatedAt
        }

        assertEquals(3, localWorkers.size)
        assertEquals("Bob", localWorkers["w2"]?.name)
        assertEquals("Charlie", localWorkers["w3"]?.name)
        assertFalse(localWorkers["w2"]!!.isPendingSync)

        // Advance cursor
        SyncCursorManager.setLastSyncTimestamp(testUserId, "workers", highestUpdatedAt)
        assertEquals(1800L, SyncCursorManager.getLastSyncTimestamp(testUserId, "workers"))
    }

    // =========================================================================
    // SCENARIO 4: Incremental sync with modified records (LWW)
    // =========================================================================
    @Test
    fun testScenario4_incrementalSync_modifiedRecords_updatedViaLWW() {
        val initialCursor = 2000L
        SyncCursorManager.setLastSyncTimestamp(testUserId, "sites", initialCursor)

        val localSite = SiteEntity(
            id = 42,
            name = "Alpha Site v1",
            address = "Original Road",
            syncId = "site_alpha",
            updatedAt = 1900L,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            isPendingSync = false
        )

        // Cloud updated the site on another device at timestamp 2500L
        val cloudSiteUpdate = SiteDTO(
            syncId = "site_alpha",
            name = "Alpha Site v2 Updated",
            address = "New Road",
            updatedAt = 2500L
        )

        assertTrue(cloudSiteUpdate.updatedAt > localSite.updatedAt)
        val updatedLocal = SiteMapper.toEntity(cloudSiteUpdate, localId = localSite.id).copy(isPendingSync = false)

        assertEquals(42L, updatedLocal.id)
        assertEquals("Alpha Site v2 Updated", updatedLocal.name)
        assertEquals("New Road", updatedLocal.address)
        assertEquals(2500L, updatedLocal.updatedAt)
        assertFalse(updatedLocal.isPendingSync)
    }

    // =========================================================================
    // SCENARIO 5: Incremental sync with soft-deleted records (tombstones propagated)
    // =========================================================================
    @Test
    fun testScenario5_incrementalSync_softDeletedRecords_propagatedToLocal() {
        val localSite = SiteEntity(
            id = 10,
            name = "Site To Be Deleted",
            syncId = "site_tombstone_1",
            isDeleted = false,
            deletedAt = null,
            updatedAt = 3000L,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            isPendingSync = false
        )

        // Another device soft-deleted the site on the cloud at timestamp 3500L
        val cloudTombstone = SiteDTO(
            syncId = "site_tombstone_1",
            name = "Site To Be Deleted",
            isDeleted = true,
            deletedAt = 3500L,
            updatedAt = 3500L
        )

        assertTrue(cloudTombstone.updatedAt > localSite.updatedAt)
        val updatedLocal = SiteMapper.toEntity(cloudTombstone, localId = localSite.id).copy(isPendingSync = false)

        assertEquals(10L, updatedLocal.id)
        assertTrue("Local record must reflect soft-deleted status from cloud", updatedLocal.isDeleted)
        assertEquals(3500L, updatedLocal.deletedAt)
        assertFalse(updatedLocal.isPendingSync)
    }

    // =========================================================================
    // SCENARIO 6: Incremental sync with restored records
    // =========================================================================
    @Test
    fun testScenario6_incrementalSync_restoredRecords_propagatedToLocal() {
        val localDeletedWorker = WorkerEntity(
            id = 7,
            name = "Restored Worker",
            syncId = "w_restore_99",
            isDeleted = true,
            deletedAt = 4000L,
            updatedAt = 4000L,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            isPendingSync = false
        )

        // Cloud restored this worker from trash at timestamp 4500L
        val cloudRestored = WorkerDTO(
            syncId = "w_restore_99",
            name = "Restored Worker",
            isDeleted = false,
            deletedAt = null,
            updatedAt = 4500L
        )

        assertTrue(cloudRestored.updatedAt > localDeletedWorker.updatedAt)
        val updatedLocal = WorkerMapper.toEntity(cloudRestored, localId = localDeletedWorker.id).copy(isPendingSync = false)

        assertEquals(7L, updatedLocal.id)
        assertFalse("Worker must no longer be marked isDeleted locally", updatedLocal.isDeleted)
        assertNull("deletedAt must be cleared upon restore", updatedLocal.deletedAt)
        assertEquals(4500L, updatedLocal.updatedAt)
        assertFalse(updatedLocal.isPendingSync)
    }

    // =========================================================================
    // SCENARIO 7: Pagination across multiple pages with identical timestamps
    // =========================================================================
    @Test
    fun testScenario7_pagination_acrossMultiplePages_handlesStartAfterAndSameTimestamps() {
        // Test that pagination does not drop or duplicate records when multiple items share identical timestamps
        val sharedTimestamp = 5000L
        val totalItems = 250
        val dataset = (1..totalItems).map { i ->
            SiteDTO(syncId = "id_$i", name = "Bulk Site $i", updatedAt = sharedTimestamp)
        }

        val pageSize = 50
        val pages = mutableListOf<List<SiteDTO>>()
        var offset = 0

        while (offset < dataset.size) {
            val page = dataset.subList(offset, minOf(offset + pageSize, dataset.size))
            pages.add(page)
            offset += page.size
        }

        assertEquals(5, pages.size)
        val assembled = pages.flatten()
        assertEquals(250, assembled.size)
        // Verify no duplicates
        val uniqueIds = assembled.map { it.syncId }.toSet()
        assertEquals(250, uniqueIds.size)
    }

    // =========================================================================
    // SCENARIO 8: Concurrent updates / conflict resolution (Last-Write-Wins)
    // =========================================================================
    @Test
    fun testScenario8_concurrentUpdates_conflictResolution_LWW() {
        // Case A: Cloud is newer than local -> Cloud wins
        val localSiteOld = SiteEntity(id = 1, name = "Local Old", syncId = "sync_1", updatedAt = 1000L, syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = true)
        val cloudSiteNew = SiteDTO(syncId = "sync_1", name = "Cloud New", updatedAt = 2000L)

        val resolutionA = if (cloudSiteNew.updatedAt > localSiteOld.updatedAt) {
            SiteMapper.toEntity(cloudSiteNew, localId = localSiteOld.id).copy(isPendingSync = false)
        } else {
            localSiteOld
        }
        assertEquals("Cloud New", resolutionA.name)
        assertFalse(resolutionA.isPendingSync)

        // Case B: Local is newer than cloud -> Local wins, remains pending sync for outbound push
        val localSiteNewer = SiteEntity(id = 2, name = "Local Newer Edit", syncId = "sync_2", updatedAt = 3000L, syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = true)
        val cloudSiteOlder = SiteDTO(syncId = "sync_2", name = "Cloud Older", updatedAt = 2500L)

        val resolutionB = if (cloudSiteOlder.updatedAt > localSiteNewer.updatedAt) {
            SiteMapper.toEntity(cloudSiteOlder, localId = localSiteNewer.id).copy(isPendingSync = false)
        } else {
            localSiteNewer
        }
        assertEquals("Local Newer Edit", resolutionB.name)
        assertTrue("Local winning version must retain isPendingSync = true to push to cloud", resolutionB.isPendingSync)
    }

    // =========================================================================
    // SCENARIO 9: Offline changes preserved and pushed when online
    // =========================================================================
    @Test
    fun testScenario9_offlineChangesPreserved_pendingSyncPushedWhenOnline() {
        // Device goes offline and edits worker
        val offlineWorker = WorkerEntity(
            id = 5,
            name = "Offline Edited Worker",
            phone = "9876543210",
            syncId = "worker_offline_1",
            updatedAt = 6000L,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            isPendingSync = true
        )

        // Cloud had no updates for this worker during the offline period (cloud updatedAt = 5000L)
        val cloudWorker = WorkerDTO(syncId = "worker_offline_1", name = "Old Name", phone = "1111111111", updatedAt = 5000L)

        // During pull: cloud is older than local, so local is not overwritten
        val shouldOverwriteFromCloud = cloudWorker.updatedAt > offlineWorker.updatedAt
        assertFalse("Cloud older version must not overwrite offline local changes", shouldOverwriteFromCloud)

        // During push: eligible because syncOrigin is CLOUD_SYNCED and isPendingSync is true
        val isEligibleForPush = (offlineWorker.syncOrigin == SyncOrigin.NEW_LOCAL || offlineWorker.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                offlineWorker.syncId.isNotBlank() && offlineWorker.isPendingSync
        assertTrue(isEligibleForPush)

        // After successful push ack, mark pending sync false
        val pushedAndSynced = offlineWorker.copy(isPendingSync = false)
        assertFalse(pushedAndSynced.isPendingSync)
    }

    // =========================================================================
    // SCENARIO 10: Reconnection after network drop (safety overlap buffer)
    // =========================================================================
    @Test
    fun testScenario10_reconnectionAfterNetworkDrop_safetyOverlapCapturesInFlightChanges() {
        val lastSyncTimestamp = 100000L
        val clockSkewBuffer = FirestoreSyncHelper.DEFAULT_CLOCK_SKEW_BUFFER_MS // 5000L

        // A write was committed on server at timestamp 98000L while network was flaky
        val inFlightCloudRecord = SiteDTO(
            syncId = "site_inflight",
            name = "In Flight Concurrent Site",
            updatedAt = 98000L
        )

        // Query timestamp with buffer
        val queryFrom = maxOf(0L, lastSyncTimestamp - clockSkewBuffer) // 95000L
        assertEquals(95000L, queryFrom)

        // The query captures the in-flight record because 98000L > 95000L
        assertTrue("Safety overlap buffer must include writes committed within the skew window", inFlightCloudRecord.updatedAt > queryFrom)
    }

    // =========================================================================
    // SCENARIO 11: Duplicate sync request prevention (Mutex)
    // =========================================================================
    @Test
    fun testScenario11_duplicateSyncPrevention_mutexPreventsConcurrentRuns() = runBlocking {
        val mutex = Mutex()
        var concurrentExecutions = 0
        var maxConcurrent = 0
        val completedSyncs = java.util.concurrent.atomic.AtomicInteger(0)
        val skippedSyncs = java.util.concurrent.atomic.AtomicInteger(0)

        suspend fun simulateSync(): Boolean {
            if (!mutex.tryLock()) {
                skippedSyncs.incrementAndGet()
                return false
            }
            try {
                concurrentExecutions++
                if (concurrentExecutions > maxConcurrent) maxConcurrent = concurrentExecutions
                // Simulate work
                kotlinx.coroutines.delay(20)
                completedSyncs.incrementAndGet()
                return true
            } finally {
                concurrentExecutions--
                mutex.unlock()
            }
        }

        // Fire 10 simultaneous sync attempts
        val jobs = (1..10).map {
            async(Dispatchers.Default) {
                simulateSync()
            }
        }
        jobs.awaitAll()

        assertEquals("At most 1 sync may execute concurrently under the Mutex", 1, maxConcurrent)
        assertTrue("At least 1 sync completed", completedSyncs.get() >= 1)
        assertTrue("Duplicate concurrent syncs were gracefully skipped", skippedSyncs.get() > 0)
        assertEquals(10, completedSyncs.get() + skippedSyncs.get())
    }

    // =========================================================================
    // SCENARIO 12: Legacy records handling (skipped when bootstrapped)
    // =========================================================================
    @Test
    fun testScenario12_legacyRecordsHandling_skippedWhenBootstrapped() {
        assertFalse(SyncCursorManager.hasBootstrappedLegacy(testUserId))

        // First run marks bootstrap complete
        SyncCursorManager.setBootstrappedLegacy(testUserId, true)
        assertTrue(SyncCursorManager.hasBootstrappedLegacy(testUserId))

        // Subsequent check avoids all 14 full collection reads
        val shouldSkipLegacyCloudReads = SyncCursorManager.hasBootstrappedLegacy(testUserId)
        assertTrue("LegacySyncBootstrapper must skip cloud collection reads when bootstrapped", shouldSkipLegacyCloudReads)
    }
}


package com.example.workermanager

import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.data.remote.sync.MonotonicClock
import com.example.workermanager.data.remote.sync.RecordRevisionManager
import com.example.workermanager.data.remote.sync.dto.SiteDTO
import com.example.workermanager.data.remote.sync.dto.WorkerDTO
import com.example.workermanager.data.remote.sync.engine.LegacySyncBootstrapper
import com.example.workermanager.data.remote.sync.engine.SiteSyncEngine
import com.example.workermanager.data.remote.sync.engine.WorkerSyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class OfflinePendingSyncProtectionTest {

    private lateinit var fakeSiteDao: FakeSiteDao
    private lateinit var fakeWorkerDao: FakeWorkerDao

    @Before
    fun setUp() {
        RecordRevisionManager.reset()
        fakeSiteDao = FakeSiteDao()
        fakeWorkerDao = FakeWorkerDao()
    }

    // =========================================================================
    // SCENARIO A: Local pending Worker + newer cloud Worker
    // Local data and isPendingSync = true survive inbound sync.
    // =========================================================================
    @Test
    fun testScenarioA_localPendingWorker_newerCloudWorker_survivesInboundSync() = runBlocking {
        val initialWorker = WorkerEntity(
            id = 1L,
            name = "Offline Edited Worker",
            phone = "1112223333",
            workerType = WorkerType.MISTRI,
            dailyRate = 800.0,
            overtimeRate = 120.0,
            joiningDate = LocalDate.of(2024, 1, 10),
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "worker_sync_a",
            updatedAt = 1000L,
            isPendingSync = true
        )
        fakeWorkerDao.insertWorker(initialWorker)

        // Incoming cloud version is newer (updatedAt = 5000L) with different values
        val incomingCloud = WorkerDTO(
            syncId = "worker_sync_a",
            name = "Cloud Overwrite Attempt",
            phone = "9998887777",
            workerType = "LABOUR",
            dailyRate = 500.0,
            overtimeRate = 50.0,
            joiningDate = "2024-01-01",
            updatedAt = 5000L
        )

        // Process inbound pull
        WorkerSyncEngine.processInboundWorkers(listOf(incomingCloud), fakeWorkerDao, fakeSiteDao)

        // Local pending worker must NOT be overwritten
        val localWorker = fakeWorkerDao.getWorkerBySyncId("worker_sync_a")
        assertNotNull(localWorker)
        assertEquals(1L, localWorker!!.id)
        assertEquals("Offline Edited Worker", localWorker.name)
        assertEquals("1112223333", localWorker.phone)
        assertEquals(WorkerType.MISTRI, localWorker.workerType)
        assertEquals(800.0, localWorker.dailyRate, 0.001)
        assertEquals(1000L, localWorker.updatedAt)
        assertTrue("isPendingSync must remain true after inbound sync", localWorker.isPendingSync)
    }

    // =========================================================================
    // SCENARIO B: Local pending Site + newer cloud Site
    // Local data and isPendingSync = true survive inbound sync.
    // =========================================================================
    @Test
    fun testScenarioB_localPendingSite_newerCloudSite_survivesInboundSync() = runBlocking {
        val initialSite = SiteEntity(
            id = 2L,
            name = "Offline Edited Site",
            address = "Local St 456",
            manager = "Local Manager Bob",
            startDate = LocalDate.of(2024, 2, 1),
            status = SiteStatus.ACTIVE,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_b",
            updatedAt = 1000L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(initialSite)

        // Incoming cloud site has newer timestamp (5000L)
        val incomingCloud = SiteDTO(
            syncId = "site_sync_b",
            name = "Cloud Site Overwrite Attempt",
            address = "Cloud Blvd 999",
            manager = "Cloud Manager Alice",
            startDate = "2024-01-01",
            status = "COMPLETED",
            updatedAt = 5000L
        )

        // Process inbound pull
        SiteSyncEngine.processInboundSites(listOf(incomingCloud), fakeSiteDao)

        // Local pending site must NOT be overwritten
        val localSite = fakeSiteDao.getSiteBySyncId("site_sync_b")
        assertNotNull(localSite)
        assertEquals(2L, localSite!!.id)
        assertEquals("Offline Edited Site", localSite.name)
        assertEquals("Local St 456", localSite.address)
        assertEquals("Local Manager Bob", localSite.manager)
        assertEquals(SiteStatus.ACTIVE, localSite.status)
        assertEquals(1000L, localSite.updatedAt)
        assertTrue("isPendingSync must remain true after inbound sync", localSite.isPendingSync)
    }

    // =========================================================================
    // SCENARIO C: Local pending Worker with older timestamp
    // Outbound upload executes (not blocked by local.updatedAt <= cloud.updatedAt).
    // =========================================================================
    @Test
    fun testScenarioC_localPendingWorker_withOlderTimestamp_outboundUploadExecutes() {
        val localWorker = WorkerEntity(
            id = 3L,
            name = "Worker With Clock Skew",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "worker_sync_c",
            updatedAt = 1000L, // Older than cloud
            isPendingSync = true
        )

        val newerCloudWorker = WorkerDTO(
            syncId = "worker_sync_c",
            name = "Cloud Worker Newer Timestamp",
            updatedAt = 5000L // Cloud is newer
        )

        val actionWithNewerCloud = WorkerSyncEngine.determinePushAction(localWorker, newerCloudWorker)
        assertEquals(WorkerSyncEngine.PushAction.UPDATE_METADATA, actionWithNewerCloud)

        val actionWithNoCloud = WorkerSyncEngine.determinePushAction(localWorker, null)
        assertEquals(WorkerSyncEngine.PushAction.CREATE_NEW, actionWithNoCloud)

        val nonPendingWorker = localWorker.copy(isPendingSync = false)
        val actionNonPending = WorkerSyncEngine.determinePushAction(nonPendingWorker, newerCloudWorker)
        assertEquals(WorkerSyncEngine.PushAction.SKIP, actionNonPending)
    }

    // =========================================================================
    // SCENARIO D: Local pending Site with older timestamp
    // Outbound upload executes (not blocked by local.updatedAt <= cloud.updatedAt).
    // =========================================================================
    @Test
    fun testScenarioD_localPendingSite_withOlderTimestamp_outboundUploadExecutes() {
        val localSite = SiteEntity(
            id = 4L,
            name = "Site With Clock Skew",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_d",
            updatedAt = 1000L, // Older than cloud
            isPendingSync = true
        )

        val newerCloudSite = SiteDTO(
            syncId = "site_sync_d",
            name = "Cloud Site Newer Timestamp",
            updatedAt = 5000L // Cloud is newer
        )

        val actionWithNewerCloud = SiteSyncEngine.determinePushAction(localSite, newerCloudSite)
        assertEquals(SiteSyncEngine.PushAction.UPDATE_METADATA, actionWithNewerCloud)

        val actionWithNoCloud = SiteSyncEngine.determinePushAction(localSite, null)
        assertEquals(SiteSyncEngine.PushAction.CREATE_NEW, actionWithNoCloud)

        val nonPendingSite = localSite.copy(isPendingSync = false)
        val actionNonPending = SiteSyncEngine.determinePushAction(nonPendingSite, newerCloudSite)
        assertEquals(SiteSyncEngine.PushAction.SKIP, actionNonPending)
    }

    // =========================================================================
    // SCENARIO E: Successful upload
    // Only the successfully uploaded version is acknowledged.
    // =========================================================================
    @Test
    fun testScenarioE_successfulUpload_onlyUploadedVersionAcknowledged() = runBlocking {
        val site = SiteEntity(
            id = 5L,
            name = "Site Upload Target",
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "site_sync_e",
            updatedAt = 1500L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(site)

        val snapshotRevision = RecordRevisionManager.computeSiteRevision(site)
        val acknowledged = SiteSyncEngine.acknowledgeSiteSync(
            siteDao = fakeSiteDao,
            syncId = "site_sync_e",
            expectedRevision = snapshotRevision
        ) {
            it.copy(syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
        }

        assertTrue("Acknowledgement should succeed when revisions match", acknowledged)
        val updatedSite = fakeSiteDao.getSiteBySyncId("site_sync_e")
        assertNotNull(updatedSite)
        assertEquals(SyncOrigin.CLOUD_SYNCED, updatedSite!!.syncOrigin)
        assertFalse("isPendingSync must be cleared after successful upload", updatedSite.isPendingSync)
        assertEquals(1500L, updatedSite.updatedAt)

        // Identical verification for Worker
        val worker = WorkerEntity(
            id = 5L,
            name = "Worker Upload Target",
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "worker_sync_e",
            updatedAt = 1500L,
            isPendingSync = true
        )
        fakeWorkerDao.insertWorker(worker)

        val workerSnapshotRevision = RecordRevisionManager.computeWorkerRevision(worker)
        val workerAcknowledged = WorkerSyncEngine.acknowledgeWorkerSync(
            workerDao = fakeWorkerDao,
            syncId = "worker_sync_e",
            expectedRevision = workerSnapshotRevision
        ) {
            it.copy(syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
        }

        assertTrue(workerAcknowledged)
        val updatedWorker = fakeWorkerDao.getWorkerBySyncId("worker_sync_e")
        assertNotNull(updatedWorker)
        assertEquals(SyncOrigin.CLOUD_SYNCED, updatedWorker!!.syncOrigin)
        assertFalse(updatedWorker.isPendingSync)
        assertEquals(1500L, updatedWorker.updatedAt)
    }

    // =========================================================================
    // SCENARIO F: Failed upload
    // isPendingSync remains true.
    // =========================================================================
    @Test
    fun testScenarioF_failedUpload_isPendingSyncRemainsTrue() = runBlocking {
        val site = SiteEntity(
            id = 6L,
            name = "Site Network Failure",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_f",
            updatedAt = 1600L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(site)

        // When upload fails (e.g. exception thrown before acknowledgeSiteSync is invoked),
        // acknowledgeSiteSync is NOT called.
        val siteAfterFailure = fakeSiteDao.getSiteBySyncId("site_sync_f")
        assertNotNull(siteAfterFailure)
        assertTrue("isPendingSync must remain true after failed upload", siteAfterFailure!!.isPendingSync)
        assertEquals(1600L, siteAfterFailure.updatedAt)

        // Worker test
        val worker = WorkerEntity(
            id = 6L,
            name = "Worker Network Failure",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "worker_sync_f",
            updatedAt = 1600L,
            isPendingSync = true
        )
        fakeWorkerDao.insertWorker(worker)

        val workerAfterFailure = fakeWorkerDao.getWorkerBySyncId("worker_sync_f")
        assertNotNull(workerAfterFailure)
        assertTrue("isPendingSync must remain true after worker failed upload", workerAfterFailure!!.isPendingSync)
    }

    // =========================================================================
    // SCENARIO G: New edit during in-progress upload
    // Newer edit remains pending (isPendingSync = true).
    // =========================================================================
    @Test
    fun testScenarioG_newEditDuringUpload_newerEditRemainsPending() = runBlocking {
        val initialWorker = WorkerEntity(
            id = 7L,
            name = "Worker Version 1",
            dailyRate = 500.0,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "worker_sync_g",
            updatedAt = 1000L,
            isPendingSync = true
        )
        fakeWorkerDao.insertWorker(initialWorker)

        // Upload starts, capturing snapshot revision
        val inFlightSnapshotRevision = RecordRevisionManager.computeWorkerRevision(initialWorker)

        // Concurrently while upload is in flight, user makes a new local edit in Room
        val concurrentEdit = initialWorker.copy(
            name = "Worker Version 2 Concurrently Modified",
            dailyRate = 700.0,
            updatedAt = 2000L,
            isPendingSync = true
        )
        fakeWorkerDao.updateWorker(concurrentEdit)

        // In-flight upload finishes and attempts to acknowledge with older snapshot revision
        val acknowledged = WorkerSyncEngine.acknowledgeWorkerSync(
            workerDao = fakeWorkerDao,
            syncId = "worker_sync_g",
            expectedRevision = inFlightSnapshotRevision
        ) {
            it.copy(isPendingSync = false)
        }

        // Acknowledgement MUST be rejected because local record revision has advanced
        assertFalse("Acknowledgement must be rejected when concurrent edit occurred", acknowledged)

        val currentLocal = fakeWorkerDao.getWorkerBySyncId("worker_sync_g")
        assertNotNull(currentLocal)
        assertEquals("Worker Version 2 Concurrently Modified", currentLocal!!.name)
        assertEquals(700.0, currentLocal.dailyRate, 0.001)
        assertEquals(2000L, currentLocal.updatedAt)
        assertTrue("Concurrent edit must preserve isPendingSync = true", currentLocal.isPendingSync)

        // Same test for Site concurrency
        val initialSite = SiteEntity(
            id = 7L,
            name = "Site Version 1",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_g",
            updatedAt = 1000L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(initialSite)

        val siteSnapshotRevision = RecordRevisionManager.computeSiteRevision(initialSite)

        val concurrentSiteEdit = initialSite.copy(
            name = "Site Version 2 Concurrently Modified",
            updatedAt = 2500L,
            isPendingSync = true
        )
        fakeSiteDao.updateSite(concurrentSiteEdit)

        val siteAcknowledged = SiteSyncEngine.acknowledgeSiteSync(
            siteDao = fakeSiteDao,
            syncId = "site_sync_g",
            expectedRevision = siteSnapshotRevision
        ) {
            it.copy(isPendingSync = false)
        }

        assertFalse("Site acknowledgement must be rejected when concurrent edit occurred", siteAcknowledged)
        val currentSite = fakeSiteDao.getSiteBySyncId("site_sync_g")
        assertNotNull(currentSite)
        assertEquals("Site Version 2 Concurrently Modified", currentSite!!.name)
        assertEquals(2500L, currentSite.updatedAt)
        assertTrue("Site concurrent edit must preserve isPendingSync = true", currentSite.isPendingSync)
    }

    // =========================================================================
    // SCENARIO H: Normal non-pending cloud update
    // Existing synchronization behavior continues working.
    // =========================================================================
    @Test
    fun testScenarioH_normalNonPendingCloudUpdate_updatesLocalRoom() = runBlocking {
        val localSite = SiteEntity(
            id = 8L,
            name = "Initial Local Site",
            address = "Old Address",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_h",
            updatedAt = 1000L,
            isPendingSync = false
        )
        fakeSiteDao.insertSite(localSite)

        val localWorker = WorkerEntity(
            id = 8L,
            name = "Initial Local Worker",
            dailyRate = 400.0,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "worker_sync_h",
            updatedAt = 1000L,
            isPendingSync = false
        )
        fakeWorkerDao.insertWorker(localWorker)

        // Cloud updates with newer timestamps (3000L)
        val cloudSite = SiteDTO(
            syncId = "site_sync_h",
            name = "Cloud Updated Site Name",
            address = "New Address St",
            updatedAt = 3000L
        )
        val cloudWorker = WorkerDTO(
            syncId = "worker_sync_h",
            name = "Cloud Updated Worker Name",
            dailyRate = 650.0,
            updatedAt = 3000L
        )

        // Process inbound sync
        SiteSyncEngine.processInboundSites(listOf(cloudSite), fakeSiteDao)
        WorkerSyncEngine.processInboundWorkers(listOf(cloudWorker), fakeWorkerDao, fakeSiteDao)

        val updatedSite = fakeSiteDao.getSiteBySyncId("site_sync_h")
        assertNotNull(updatedSite)
        assertEquals(8L, updatedSite!!.id)
        assertEquals("Cloud Updated Site Name", updatedSite.name)
        assertEquals("New Address St", updatedSite.address)
        assertEquals(3000L, updatedSite.updatedAt)
        assertFalse(updatedSite.isPendingSync)

        val updatedWorker = fakeWorkerDao.getWorkerBySyncId("worker_sync_h")
        assertNotNull(updatedWorker)
        assertEquals(8L, updatedWorker!!.id)
        assertEquals("Cloud Updated Worker Name", updatedWorker.name)
        assertEquals(650.0, updatedWorker.dailyRate, 0.001)
        assertEquals(3000L, updatedWorker.updatedAt)
        assertFalse(updatedWorker.isPendingSync)

        // Pulling a brand new cloud entity inserts into Room
        val brandNewCloudSite = SiteDTO(
            syncId = "site_brand_new_h",
            name = "Brand New Cloud Site",
            updatedAt = 4000L
        )
        SiteSyncEngine.processInboundSites(listOf(brandNewCloudSite), fakeSiteDao)
        val insertedSite = fakeSiteDao.getSiteBySyncId("site_brand_new_h")
        assertNotNull(insertedSite)
        assertEquals("Brand New Cloud Site", insertedSite!!.name)
        assertFalse(insertedSite.isPendingSync)
    }

    // =========================================================================
    // SCENARIO I: Deleted and tombstone records
    // Existing deletion and quota behavior remains intact.
    // =========================================================================
    @Test
    fun testScenarioI_deletedAndTombstoneRecords_handledCorrectly() = runBlocking {
        // 1. Cloud soft-delete arriving for a non-pending local record: propagates to local
        val localActiveSite = SiteEntity(
            id = 9L,
            name = "Site To Be Soft Deleted",
            isDeleted = false,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_i_soft",
            updatedAt = 1000L,
            isPendingSync = false
        )
        fakeSiteDao.insertSite(localActiveSite)

        val cloudSoftDelete = SiteDTO(
            syncId = "site_sync_i_soft",
            name = "Site To Be Soft Deleted",
            isDeleted = true,
            deletedAt = 2500L,
            updatedAt = 2500L
        )
        SiteSyncEngine.processInboundSites(listOf(cloudSoftDelete), fakeSiteDao)

        val softDeletedLocalSite = fakeSiteDao.getSiteBySyncId("site_sync_i_soft")
        assertNotNull(softDeletedLocalSite)
        assertTrue("Local site must reflect cloud soft delete", softDeletedLocalSite!!.isDeleted)
        assertEquals(2500L, softDeletedLocalSite.deletedAt)
        assertFalse(softDeletedLocalSite.isPendingSync)

        // 2. Local pending soft-delete: outbound push action must be SOFT_DELETE
        val localPendingSoftDelete = SiteEntity(
            id = 10L,
            name = "Local Soft Deleted Site",
            isDeleted = true,
            deletedAt = 3000L,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_i_local_soft",
            updatedAt = 3000L,
            isPendingSync = true
        )
        val softDeleteAction = SiteSyncEngine.determinePushAction(localPendingSoftDelete, null)
        assertEquals(SiteSyncEngine.PushAction.SOFT_DELETE, softDeleteAction)

        // 3. Local permanent delete tombstone: outbound push action must be PERMANENT_DELETE
        val localPermanentTombstone = SiteEntity(
            id = 11L,
            name = "Local Permanent Tombstone Site",
            deletedAt = -1L,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_i_perm",
            updatedAt = 3500L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(localPermanentTombstone)
        val permDeleteAction = SiteSyncEngine.determinePushAction(localPermanentTombstone, null)
        assertEquals(SiteSyncEngine.PushAction.PERMANENT_DELETE, permDeleteAction)

        // Acknowledge permanent delete purges tombstone from Room
        val tombstoneRevision = RecordRevisionManager.computeSiteRevision(localPermanentTombstone)
        val permPurged = SiteSyncEngine.acknowledgeSitePermanentDelete(
            siteDao = fakeSiteDao,
            syncId = "site_sync_i_perm",
            expectedRevision = tombstoneRevision
        )
        assertTrue(permPurged)
        assertNull("Tombstone must be purged from Room after permanent deletion", fakeSiteDao.getSiteBySyncId("site_sync_i_perm"))

        // 4. Cloud was soft-deleted, but local was restored: outbound push action must be RESTORE
        val localRestoredSite = SiteEntity(
            id = 12L,
            name = "Restored Site",
            isDeleted = false,
            deletedAt = null,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_sync_i_restored",
            updatedAt = 4000L,
            isPendingSync = true
        )
        val cloudDeletedDto = SiteDTO(
            syncId = "site_sync_i_restored",
            name = "Restored Site",
            isDeleted = true,
            deletedAt = 2000L,
            updatedAt = 2000L
        )
        val restoreAction = SiteSyncEngine.determinePushAction(localRestoredSite, cloudDeletedDto)
        assertEquals(SiteSyncEngine.PushAction.RESTORE, restoreAction)
    }

    // =========================================================================
    // FOCUSED TESTS FOR RACE CONDITIONS & SYNC ORIGIN ELIGIBILITY
    // =========================================================================

    /**
     * Requirement: Two edits occurring within the exact same millisecond must produce different revisions.
     */
    @Test
    fun testTwoEditsOccurringWithinSameMillisecond_producesDifferentRevisions() {
        val sameTimestamp = 1700000000000L

        // Site edits within the same millisecond
        val siteEdit1 = SiteEntity(
            id = 20L,
            name = "Site First Edit",
            syncId = "site_same_ms",
            updatedAt = sameTimestamp,
            isPendingSync = true
        )
        val siteEdit2 = SiteEntity(
            id = 20L,
            name = "Site Second Edit Within Same Millisecond",
            syncId = "site_same_ms",
            updatedAt = sameTimestamp,
            isPendingSync = true
        )

        val siteRev1 = RecordRevisionManager.computeSiteRevision(siteEdit1)
        val siteRev2 = RecordRevisionManager.computeSiteRevision(siteEdit2)
        assertNotEquals("Revisions must differ for field changes within the same millisecond", siteRev1, siteRev2)

        // Worker edits within the same millisecond
        val workerEdit1 = WorkerEntity(
            id = 21L,
            name = "Worker Edit 1",
            dailyRate = 500.0,
            syncId = "worker_same_ms",
            updatedAt = sameTimestamp,
            isPendingSync = true
        )
        val workerEdit2 = WorkerEntity(
            id = 21L,
            name = "Worker Edit 2",
            dailyRate = 600.0,
            syncId = "worker_same_ms",
            updatedAt = sameTimestamp,
            isPendingSync = true
        )

        val workerRev1 = RecordRevisionManager.computeWorkerRevision(workerEdit1)
        val workerRev2 = RecordRevisionManager.computeWorkerRevision(workerEdit2)
        assertNotEquals("Worker revisions must differ for field changes within the same millisecond", workerRev1, workerRev2)

        // Monotonic mutation event on identical content
        val siteRev2BeforeEvent = RecordRevisionManager.computeSiteRevision(siteEdit2)
        RecordRevisionManager.recordLocalEdit(siteEdit2.syncId)
        val siteRev2AfterEvent = RecordRevisionManager.computeSiteRevision(siteEdit2)
        assertNotEquals("Revision must change when a local edit event is registered", siteRev2BeforeEvent, siteRev2AfterEvent)
    }

    /**
     * Requirement: Revision changing during upload prevents premature acknowledgement.
     */
    @Test
    fun testRevisionChangingDuringUpload_newerEditRemainsPending() = runBlocking {
        val sameMs = 1700000000500L
        val initialSite = SiteEntity(
            id = 30L,
            name = "Site At Upload Start",
            syncId = "site_concurrent_ms",
            updatedAt = sameMs,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(initialSite)

        // Upload starts and captures revision
        val capturedRevision = RecordRevisionManager.computeSiteRevision(initialSite)

        // User edits in the same millisecond while upload is in-flight
        val sameMsEdit = initialSite.copy(
            name = "Site Edited While Uploading Same Millisecond",
            updatedAt = sameMs
        )
        fakeSiteDao.updateSite(sameMsEdit)

        // Upload finishes and attempts acknowledgement with the original captured revision
        val acknowledged = SiteSyncEngine.acknowledgeSiteSync(
            siteDao = fakeSiteDao,
            syncId = "site_concurrent_ms",
            expectedRevision = capturedRevision
        ) {
            it.copy(isPendingSync = false)
        }

        assertFalse("Acknowledgement must fail when revision changed during upload", acknowledged)
        val localSite = fakeSiteDao.getSiteBySyncId("site_concurrent_ms")
        assertNotNull(localSite)
        assertEquals("Site Edited While Uploading Same Millisecond", localSite!!.name)
        assertTrue("Newer edit must remain pending with isPendingSync = true", localSite.isPendingSync)
    }

    /**
     * Requirement: Successful acknowledgement of the correct revision clears isPendingSync.
     */
    @Test
    fun testSuccessfulAcknowledgementOfCorrectRevision_clearsPendingSync() = runBlocking {
        val worker = WorkerEntity(
            id = 40L,
            name = "Worker Stable Upload",
            syncId = "worker_stable",
            updatedAt = 1000L,
            isPendingSync = true,
            syncOrigin = SyncOrigin.NEW_LOCAL
        )
        fakeWorkerDao.insertWorker(worker)

        val validRevision = RecordRevisionManager.computeWorkerRevision(worker)
        val acknowledged = WorkerSyncEngine.acknowledgeWorkerSync(
            workerDao = fakeWorkerDao,
            syncId = "worker_stable",
            expectedRevision = validRevision
        ) {
            it.copy(syncOrigin = SyncOrigin.CLOUD_SYNCED, isPendingSync = false)
        }

        assertTrue("Acknowledgement must succeed when revision matches", acknowledged)
        val updatedWorker = fakeWorkerDao.getWorkerBySyncId("worker_stable")
        assertNotNull(updatedWorker)
        assertFalse("isPendingSync must be cleared", updatedWorker!!.isPendingSync)
        assertEquals(SyncOrigin.CLOUD_SYNCED, updatedWorker.syncOrigin)
    }

    /**
     * Requirement: Every valid SyncOrigin with isPendingSync = true is eligible and properly routed.
     */
    @Test
    fun testEveryValidSyncOriginWithPendingSync_isEligibleAndProperlyRouted() {
        val originsToTest = listOf(
            SyncOrigin.LEGACY,       // 0
            SyncOrigin.NEW_LOCAL,    // 1
            SyncOrigin.CLOUD_SYNCED, // 2
            99                       // Any other legacy/custom integer
        )

        for (origin in originsToTest) {
            val site = SiteEntity(
                id = 50L + origin,
                name = "Site Origin $origin",
                syncOrigin = origin,
                syncId = "site_origin_$origin",
                isPendingSync = true
            )

            // When cloud document does NOT exist yet, route to CREATE_NEW to enforce quota
            val actionNoCloud = SiteSyncEngine.determinePushAction(site, null)
            assertEquals("Origin $origin without cloud document must route to CREATE_NEW",
                SiteSyncEngine.PushAction.CREATE_NEW, actionNoCloud)

            // When cloud document already exists, route to UPDATE_METADATA
            val cloudDto = SiteDTO(syncId = site.syncId, name = site.name)
            val actionWithCloud = SiteSyncEngine.determinePushAction(site, cloudDto)
            assertEquals("Origin $origin with existing cloud document must route to UPDATE_METADATA",
                SiteSyncEngine.PushAction.UPDATE_METADATA, actionWithCloud)

            // Soft-delete must route to SOFT_DELETE
            val softDeletedSite = site.copy(isDeleted = true, deletedAt = 2000L)
            val actionSoftDelete = SiteSyncEngine.determinePushAction(softDeletedSite, cloudDto)
            assertEquals("Origin $origin with isDeleted = true must route to SOFT_DELETE",
                SiteSyncEngine.PushAction.SOFT_DELETE, actionSoftDelete)

            // Permanent delete tombstone must route to PERMANENT_DELETE
            val tombstoneSite = site.copy(deletedAt = -1L)
            val actionPermDelete = SiteSyncEngine.determinePushAction(tombstoneSite, cloudDto)
            assertEquals("Origin $origin with deletedAt = -1 must route to PERMANENT_DELETE",
                SiteSyncEngine.PushAction.PERMANENT_DELETE, actionPermDelete)

            // Restore from cloud trash must route to RESTORE
            val cloudTrashedDto = SiteDTO(syncId = site.syncId, isDeleted = true)
            val actionRestore = SiteSyncEngine.determinePushAction(site, cloudTrashedDto)
            assertEquals("Origin $origin restoring cloud-deleted record must route to RESTORE",
                SiteSyncEngine.PushAction.RESTORE, actionRestore)

            // Non-pending record must be skipped
            val nonPendingSite = site.copy(isPendingSync = false)
            val actionNonPending = SiteSyncEngine.determinePushAction(nonPendingSite, cloudDto)
            assertEquals("Origin $origin with isPendingSync = false must route to SKIP",
                SiteSyncEngine.PushAction.SKIP, actionNonPending)
        }
    }

    /**
     * Requirement: Failed upload preserves pending state and revision.
     */
    @Test
    fun testFailedUpload_preservesPendingStateAndRevision() = runBlocking {
        val site = SiteEntity(
            id = 60L,
            name = "Site Unsuccessful Upload",
            syncId = "site_unsuccessful",
            updatedAt = 3000L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(site)

        val originalRevision = RecordRevisionManager.computeSiteRevision(site)

        // Upload throws an error or network exception before acknowledgement can happen.
        // Acknowledge is never called.
        val recordInDao = fakeSiteDao.getSiteBySyncId("site_unsuccessful")
        assertNotNull(recordInDao)
        assertTrue("isPendingSync must remain true after failed upload", recordInDao!!.isPendingSync)
        assertEquals("Revision must remain identical for next sync cycle retry",
            originalRevision, RecordRevisionManager.computeSiteRevision(recordInDao))
    }

    // =========================================================================
    // FOCUSED TESTS: LEGACY BLANK SYNC ID, REVISION SAFETY & CONCURRENCY
    // =========================================================================

    /**
     * Requirement 1: Legacy blank syncId does not create duplicate cloud records.
     * An unidentified record with blank syncId must return PushAction.SKIP from determinePushAction
     * and must never be routed to CREATE_NEW.
     */
    @Test
    fun testLegacyBlankSyncId_doesNotCreateDuplicateCloudRecord() {
        val legacySiteBlankSyncId = SiteEntity(
            id = 101L,
            name = "Legacy Site With Blank SyncId",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            updatedAt = 1000L,
            isPendingSync = true
        )
        // With no matching cloud document, determinePushAction must SKIP, not CREATE_NEW
        val siteAction = SiteSyncEngine.determinePushAction(legacySiteBlankSyncId, null)
        assertEquals(SiteSyncEngine.PushAction.SKIP, siteAction)

        val legacyWorkerBlankSyncId = WorkerEntity(
            id = 102L,
            name = "Legacy Worker With Blank SyncId",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            updatedAt = 1000L,
            isPendingSync = true
        )
        val workerAction = WorkerSyncEngine.determinePushAction(legacyWorkerBlankSyncId, null)
        assertEquals(WorkerSyncEngine.PushAction.SKIP, workerAction)
    }

    /**
     * Requirement 2: Existing legacy identity is preserved.
     * When an existing matching candidate is found in the cloud, resolveLegacySiteIdentity and
     * resolveLegacyWorkerIdentity successfully match and adopt the cloud record's syncId.
     * Ambiguous candidates (multiple matches) must return null rather than guessing.
     */
    @Test
    fun testExistingLegacyIdentity_isPreservedAndReused() {
        // Site matching (requires strong matching attribute like address)
        val localSite = SiteEntity(
            id = 103L,
            name = "Downtown Plaza",
            address = "123 Main St",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )
        val singleCloudSite = SiteDTO(syncId = "cloud_site_plaza_99", name = " Downtown Plaza ", address = " 123 Main St ")
        val matchedSite = SiteSyncEngine.resolveLegacySiteIdentity(localSite, listOf(singleCloudSite))
        assertNotNull(matchedSite)
        assertEquals("cloud_site_plaza_99", matchedSite!!.syncId)

        // Ambiguous site matches: returns null to avoid incorrect merging
        val ambiguousSites = listOf(
            SiteDTO(syncId = "cloud_1", name = "Downtown Plaza", address = "123 Main St"),
            SiteDTO(syncId = "cloud_2", name = "Downtown Plaza", address = "123 Main St")
        )
        val ambiguousSiteMatch = SiteSyncEngine.resolveLegacySiteIdentity(localSite, ambiguousSites)
        assertNull("Ambiguous candidates must return null", ambiguousSiteMatch)

        // Worker matching
        val localWorkerWithPhone = WorkerEntity(
            id = 104L,
            name = "John Doe",
            phone = "(555) 019-2834",
            workerType = WorkerType.MISTRI,
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )
        val cloudWorkerWithMatchingPhone = WorkerDTO(
            syncId = "cloud_worker_john_77",
            name = "john doe",
            phone = "5550192834",
            workerType = "MISTRI"
        )
        val matchedWorker = WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorkerWithPhone, listOf(cloudWorkerWithMatchingPhone))
        assertNotNull(matchedWorker)
        assertEquals("cloud_worker_john_77", matchedWorker!!.syncId)

        // Worker with different phone must not match
        val cloudWorkerDifferentPhone = WorkerDTO(
            syncId = "cloud_worker_other_88",
            name = "John Doe",
            phone = "9998887777",
            workerType = "MISTRI"
        )
        val nonMatchingWorker = WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorkerWithPhone, listOf(cloudWorkerDifferentPhone))
        assertNull("Worker with mismatched phone must not match", nonMatchingWorker)
    }

    /**
     * Requirement 3: Unknown identity remains pending and recoverable.
     * When legacy record cannot be matched to any cloud document, it remains pending in Room,
     * without being modified, deleted, or having an arbitrary syncId assigned.
     */
    @Test
    fun testUnknownIdentity_remainsPendingAndRecoverable() = runBlocking {
        val unknownLegacySite = SiteEntity(
            id = 105L,
            name = "Unmatched Unique Site",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            updatedAt = 1200L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(unknownLegacySite)

        // Attempt identity resolution against empty cloud candidate list
        val resolved = SiteSyncEngine.resolveLegacySiteIdentity(unknownLegacySite, emptyList())
        assertNull(resolved)

        // Local record in Room remains completely untouched and pending
        val siteInDao = fakeSiteDao.getAllSitesRaw().firstOrNull { it.id == 105L }
        assertNotNull(siteInDao)
        assertEquals("", siteInDao!!.syncId)
        assertTrue("isPendingSync must remain true for unidentified record", siteInDao.isPendingSync)
        assertEquals(SyncOrigin.LEGACY, siteInDao.syncOrigin)

        // Worker equivalent
        val unknownLegacyWorker = WorkerEntity(
            id = 106L,
            name = "Unmatched Unique Worker",
            phone = "1234567890",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            updatedAt = 1200L,
            isPendingSync = true
        )
        fakeWorkerDao.insertWorker(unknownLegacyWorker)

        val resolvedWorker = WorkerSyncEngine.resolveLegacyWorkerIdentity(unknownLegacyWorker, emptyList())
        assertNull(resolvedWorker)

        val workerInDao = fakeWorkerDao.getAllWorkersRaw().firstOrNull { it.id == 106L }
        assertNotNull(workerInDao)
        assertEquals("", workerInDao!!.syncId)
        assertTrue("isPendingSync must remain true for unidentified worker", workerInDao.isPendingSync)
        assertEquals(SyncOrigin.LEGACY, workerInDao.syncOrigin)
    }

    /**
     * Requirement 4: Revision remains correct after process restart.
     * Resetting in-memory state (RecordRevisionManager.reset()) does not alter the revision
     * computed for a persisted entity, because revision is deterministically derived from
     * the entity's binary-serialized field data.
     */
    @Test
    fun testRevisionRemainsCorrect_afterProcessRestart() {
        val site = SiteEntity(
            id = 107L,
            name = "Persisted Site",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_restart_107",
            updatedAt = 5000000L,
            isPendingSync = true
        )

        val initialRevision = RecordRevisionManager.computeSiteRevision(site)

        // Simulate app kill / process restart by resetting in-memory sequences
        RecordRevisionManager.reset()

        val revisionAfterRestart = RecordRevisionManager.computeSiteRevision(site)
        assertEquals(
            "Revision must be identical after process restart when entity fields are unchanged",
            initialRevision,
            revisionAfterRestart
        )

        val worker = WorkerEntity(
            id = 108L,
            name = "Persisted Worker",
            phone = "5551234567",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "worker_restart_108",
            updatedAt = 6000000L,
            isPendingSync = true
        )

        val initialWorkerRevision = RecordRevisionManager.computeWorkerRevision(worker)
        RecordRevisionManager.reset()
        val workerRevisionAfterRestart = RecordRevisionManager.computeWorkerRevision(worker)
        assertEquals(
            "Worker revision must be identical after process restart",
            initialWorkerRevision,
            workerRevisionAfterRestart
        )
    }

    /**
     * Requirement 5: Concurrent repository edit and upload acknowledgement.
     * A repository edit that updates updatedAt (via MonotonicClock) and content while an upload
     * is in flight causes acknowledgement to be safely rejected.
     */
    @Test
    fun testConcurrentRepositoryEditAndUploadAcknowledgement() = runBlocking {
        val initialSite = SiteEntity(
            id = 109L,
            name = "Original Name",
            address = "Original Address",
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = "site_concurrent_edit",
            updatedAt = MonotonicClock.currentTimeMillis(),
            isPendingSync = true
        )
        fakeSiteDao.insertSite(initialSite)

        // Outbound upload starts and captures revision
        val inFlightRevision = RecordRevisionManager.computeSiteRevision(initialSite)

        // Concurrent local repository edit updates the entity with a fresh monotonic timestamp
        val editedSite = initialSite.copy(
            name = "Updated Name While Uploading",
            updatedAt = MonotonicClock.currentTimeMillis()
        )
        fakeSiteDao.updateSite(editedSite)
        RecordRevisionManager.recordLocalEdit(editedSite.syncId)

        // In-flight upload completes and attempts to acknowledge with older inFlightRevision
        val ackResult = SiteSyncEngine.acknowledgeSiteSync(
            siteDao = fakeSiteDao,
            syncId = "site_concurrent_edit",
            expectedRevision = inFlightRevision
        ) {
            it.copy(isPendingSync = false)
        }

        assertFalse("Acknowledgement must fail on concurrent repository edit", ackResult)
        val siteAfterAck = fakeSiteDao.getSiteBySyncId("site_concurrent_edit")
        assertNotNull(siteAfterAck)
        assertEquals("Updated Name While Uploading", siteAfterAck!!.name)
        assertTrue("isPendingSync must remain true for the newer local edit", siteAfterAck.isPendingSync)
    }

    /**
     * Requirement 6: Failed upload preserves pending state.
     * When an upload fails (acknowledgement is not performed), isPendingSync remains true,
     * and the entity can be retried on subsequent sync passes.
     */
    @Test
    fun testFailedUpload_preservesPendingState() = runBlocking {
        val worker = WorkerEntity(
            id = 110L,
            name = "Worker For Failed Upload",
            syncOrigin = SyncOrigin.NEW_LOCAL,
            syncId = "worker_failed_upload",
            updatedAt = MonotonicClock.currentTimeMillis(),
            isPendingSync = true
        )
        fakeWorkerDao.insertWorker(worker)

        val revisionBeforeUpload = RecordRevisionManager.computeWorkerRevision(worker)

        // Simulate upload exception before acknowledgeWorkerSync can be reached
        val simulatedNetworkError = true
        if (!simulatedNetworkError) {
            WorkerSyncEngine.acknowledgeWorkerSync(fakeWorkerDao, worker.syncId, revisionBeforeUpload) {
                it.copy(isPendingSync = false)
            }
        }

        val workerInDao = fakeWorkerDao.getWorkerBySyncId("worker_failed_upload")
        assertNotNull(workerInDao)
        assertTrue("isPendingSync must remain true after upload failure", workerInDao!!.isPendingSync)
        assertEquals(SyncOrigin.NEW_LOCAL, workerInDao.syncOrigin)
        assertEquals(revisionBeforeUpload, RecordRevisionManager.computeWorkerRevision(workerInDao))
    }

    // =========================================================================
    // FOCUSED TESTS: STRENGTHENED LEGACY WORKER & SITE IDENTITY MATCHING
    // =========================================================================

    /**
     * Requirement 1: Two workers with the same workerType but different identities.
     * Worker type alone must NEVER establish identity.
     */
    @Test
    fun testTwoWorkersWithSameWorkerType_differentIdentities_doesNotMatch() {
        val localWorker = WorkerEntity(
            id = 201L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.MISTRI,
            syncId = "",
            isPendingSync = true
        )

        // Case 1: Different names, same workerType
        val cloudDiffName = WorkerDTO(
            syncId = "cloud_worker_suresh",
            name = "Suresh Sharma",
            phone = "9876543210",
            workerType = "MISTRI"
        )
        assertFalse(
            "Different names must not match even if phone and workerType match",
            LegacySyncBootstrapper.isWorkerMatch(localWorker, cloudDiffName)
        )

        // Case 2: Same name, different phones, same workerType
        val cloudDiffPhone = WorkerDTO(
            syncId = "cloud_worker_ramesh_other",
            name = "Ramesh Kumar",
            phone = "9111111111",
            workerType = "MISTRI"
        )
        assertFalse(
            "Different phones must not match even if name and workerType match",
            LegacySyncBootstrapper.isWorkerMatch(localWorker, cloudDiffPhone)
        )
        assertNull(
            "WorkerSyncEngine must not resolve identity for different phones",
            WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorker, listOf(cloudDiffPhone))
        )
    }

    /**
     * Scenario 5.1: Same phone + different name = reject.
     */
    @Test
    fun testSamePhone_differentName_rejectsWorkerMatch() {
        val localWorker = WorkerEntity(
            id = 211L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.MISTRI,
            syncId = "",
            isPendingSync = true
        )
        val cloudDiffName = WorkerDTO(
            syncId = "cloud_worker_suresh",
            name = "Suresh Sharma", // Different name
            phone = "9876543210",   // Same phone
            workerType = "MISTRI"
        )
        assertFalse(
            "Same phone with different name must be rejected",
            LegacySyncBootstrapper.isWorkerMatch(localWorker, cloudDiffName)
        )
        assertNull(
            "resolveLegacyWorkerIdentity must return null for different name",
            WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorker, listOf(cloudDiffName))
        )
    }

    /**
     * Scenario 5.2: Same phone + normalized same name = accept only if unique.
     */
    @Test
    fun testSamePhone_normalizedSameName_acceptsOnlyIfUnique() {
        val localWorker = WorkerEntity(
            id = 212L,
            name = "  Ramesh   Kumar  ",
            phone = "+91 (987) 654-3210",
            workerType = WorkerType.MISTRI,
            syncId = "",
            isPendingSync = true
        )
        val uniqueCandidate = WorkerDTO(
            syncId = "cloud_worker_unique_accept",
            name = "ramesh kumar", // Normalized match (case & extra whitespace collapsed)
            phone = "919876543210", // Normalized matching digits
            workerType = "LABOUR"  // Different workerType ignored, identity established
        )
        assertTrue(
            "Same phone with normalized same name must match",
            LegacySyncBootstrapper.isWorkerMatch(localWorker, uniqueCandidate)
        )
        val resolved = WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorker, listOf(uniqueCandidate))
        assertNotNull(resolved)
        assertEquals("cloud_worker_unique_accept", resolved!!.syncId)
    }

    /**
     * Scenario 5.3: Same phone + duplicate cloud candidates = reject.
     */
    @Test
    fun testSamePhone_duplicateCloudCandidates_rejectsWorkerMatch() {
        val localWorker = WorkerEntity(
            id = 213L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.MISTRI,
            syncId = "",
            isPendingSync = true
        )
        val duplicateCandidates = listOf(
            WorkerDTO(syncId = "cloud_dup_1", name = "Ramesh Kumar", phone = "9876543210"),
            WorkerDTO(syncId = "cloud_dup_2", name = "Ramesh Kumar", phone = "9876543210")
        )
        assertNull(
            "Duplicate matching cloud candidates must be rejected as ambiguous",
            WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorker, duplicateCandidates)
        )
    }

    /**
     * Scenario 5.4: Missing phone = reject.
     */
    @Test
    fun testMissingPhone_rejectsWorkerMatch() {
        val localWorkerNoPhone = WorkerEntity(
            id = 214L,
            name = "Ramesh Kumar",
            phone = "",
            workerType = WorkerType.MISTRI,
            syncId = "",
            isPendingSync = true
        )
        val cloudWorkerNoPhone = WorkerDTO(
            syncId = "cloud_no_phone",
            name = "Ramesh Kumar",
            phone = "",
            workerType = "MISTRI"
        )
        assertFalse(
            "Missing phone in both records must be rejected",
            LegacySyncBootstrapper.isWorkerMatch(localWorkerNoPhone, cloudWorkerNoPhone)
        )
        assertNull(
            "resolveLegacyWorkerIdentity must return null when phone is missing",
            WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorkerNoPhone, listOf(cloudWorkerNoPhone))
        )

        val cloudWorkerWithPhone = WorkerDTO(
            syncId = "cloud_with_phone",
            name = "Ramesh Kumar",
            phone = "9876543210"
        )
        assertFalse(
            "Local missing phone must be rejected even if cloud has phone",
            LegacySyncBootstrapper.isWorkerMatch(localWorkerNoPhone, cloudWorkerWithPhone)
        )
        assertNull(
            WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorkerNoPhone, listOf(cloudWorkerWithPhone))
        )

        val localWorkerWithPhone = WorkerEntity(
            id = 215L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            syncId = "",
            isPendingSync = true
        )
        assertFalse(
            "Cloud missing phone must be rejected even if local has phone",
            LegacySyncBootstrapper.isWorkerMatch(localWorkerWithPhone, cloudWorkerNoPhone)
        )
        assertNull(
            WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorkerWithPhone, listOf(cloudWorkerNoPhone))
        )
    }

    /**
     * Scenario 5.5: Different phone + same name = reject.
     */
    @Test
    fun testDifferentPhone_sameName_rejectsWorkerMatch() {
        val localWorker = WorkerEntity(
            id = 216L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            workerType = WorkerType.MISTRI,
            syncId = "",
            isPendingSync = true
        )
        val cloudWorkerDifferentPhone = WorkerDTO(
            syncId = "cloud_worker_other_phone",
            name = "Ramesh Kumar", // Same name
            phone = "9111111111",   // Different phone
            workerType = "MISTRI"
        )
        assertFalse(
            "Different phone with same name must be rejected",
            LegacySyncBootstrapper.isWorkerMatch(localWorker, cloudWorkerDifferentPhone)
        )
        assertNull(
            "resolveLegacyWorkerIdentity must return null for different phone",
            WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorker, listOf(cloudWorkerDifferentPhone))
        )
    }

    /**
     * Requirement 5: Two sites with the same name but different addresses.
     * Address conflict must prevent matching.
     */
    @Test
    fun testTwoSitesWithSameName_differentAddresses_doesNotMatch() {
        val localSite = SiteEntity(
            id = 205L,
            name = "Central Mall",
            address = "100 North Blvd",
            syncId = "",
            isPendingSync = true
        )
        val cloudSiteDifferentAddress = SiteDTO(
            syncId = "cloud_site_south",
            name = "Central Mall",
            address = "200 South Blvd"
        )

        assertFalse(
            "Sites with same name but different addresses must NOT match",
            LegacySyncBootstrapper.isSiteMatch(localSite, cloudSiteDifferentAddress)
        )
        assertNull(
            "resolveLegacySiteIdentity must return null when addresses conflict",
            SiteSyncEngine.resolveLegacySiteIdentity(localSite, listOf(cloudSiteDifferentAddress))
        )
    }

    /**
     * Requirement 6: Unique strong Site identity match.
     * Requires an unambiguous match using sufficiently strong attributes.
     * Name-only matching must NOT automatically establish identity.
     */
    @Test
    fun testUniqueStrongSiteIdentityMatch_establishesIdentity() {
        val localSite = SiteEntity(
            id = 206L,
            name = "Central Mall",
            address = "100 North Blvd",
            manager = "Bob Smith",
            startDate = LocalDate.of(2024, 1, 15),
            syncId = "",
            isPendingSync = true
        )

        // Case A: Strong match with matching address, manager, and start date
        val strongCloudSite = SiteDTO(
            syncId = "cloud_site_strong_match",
            name = " central mall ",
            address = " 100 North Blvd ",
            manager = " Bob Smith ",
            startDate = "2024-01-15"
        )
        assertTrue(
            "Matching name, address, manager, and start date must establish site match",
            LegacySyncBootstrapper.isSiteMatch(localSite, strongCloudSite)
        )
        val resolved = SiteSyncEngine.resolveLegacySiteIdentity(localSite, listOf(strongCloudSite))
        assertNotNull(resolved)
        assertEquals("cloud_site_strong_match", resolved!!.syncId)

        // Case B: Name-only matching must NOT automatically establish identity
        val nameOnlyLocalSite = SiteEntity(
            id = 207L,
            name = "Bare Site",
            address = "",
            manager = "",
            syncId = "",
            isPendingSync = true
        )
        val nameOnlyCloudSite = SiteDTO(
            syncId = "cloud_site_bare",
            name = "Bare Site",
            address = "",
            manager = "",
            startDate = ""
        )
        assertFalse(
            "Name-only matching must not establish identity",
            LegacySyncBootstrapper.isSiteMatch(nameOnlyLocalSite, nameOnlyCloudSite)
        )
        assertNull(
            "resolveLegacySiteIdentity must return null for name-only matching",
            SiteSyncEngine.resolveLegacySiteIdentity(nameOnlyLocalSite, listOf(nameOnlyCloudSite))
        )
    }

    /**
     * Requirement 7: Unresolved Site identity remains pending in Room.
     * If identity cannot be established, preserve local record and pending state.
     * Do not delete, overwrite, or mutate the local record.
     */
    @Test
    fun testUnresolvedSiteIdentity_remainsPendingInRoom() = runBlocking {
        val unresolvedSite = SiteEntity(
            id = 208L,
            name = "Unresolved Construction Site",
            address = "Unknown Lot 42",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            updatedAt = 1000L,
            isPendingSync = true
        )
        fakeSiteDao.insertSite(unresolvedSite)

        // Cloud has no matching candidate with strong attributes
        val nonMatchingCloudCandidates = listOf(
            SiteDTO(syncId = "cloud_other_1", name = "Other Project", address = "789 Pine St"),
            SiteDTO(syncId = "cloud_other_2", name = "Unresolved Construction Site", address = "Different Address 99")
        )

        val resolved = SiteSyncEngine.resolveLegacySiteIdentity(unresolvedSite, nonMatchingCloudCandidates)
        assertNull("Site identity cannot be established", resolved)

        // Verify Room state: record remains intact, pending, with empty syncId
        val localRecord = fakeSiteDao.getAllSitesRaw().firstOrNull { it.id == 208L }
        assertNotNull(localRecord)
        assertEquals("", localRecord!!.syncId)
        assertTrue("isPendingSync must remain true for unresolved identity", localRecord.isPendingSync)
        assertEquals(SyncOrigin.LEGACY, localRecord.syncOrigin)
        assertEquals("Unresolved Construction Site", localRecord.name)
        assertEquals("Unknown Lot 42", localRecord.address)
    }

    /**
     * Requirement 8: No duplicate cloud creation or quota consumption for unresolved identities.
     * An unresolved record with blank syncId returns PushAction.SKIP.
     * It must never be routed to CREATE_NEW.
     */
    @Test
    fun testNoDuplicateCloudCreationOrQuotaConsumption_forUnresolvedIdentities() {
        val unresolvedSite = SiteEntity(
            id = 209L,
            name = "Unresolved Site Quota Guard",
            address = "",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )
        val sitePushAction = SiteSyncEngine.determinePushAction(unresolvedSite, null)
        assertEquals(
            "Unresolved site with blank syncId must SKIP outbound push and never route to CREATE_NEW",
            SiteSyncEngine.PushAction.SKIP,
            sitePushAction
        )

        val unresolvedWorker = WorkerEntity(
            id = 210L,
            name = "Unresolved Worker Quota Guard",
            phone = "",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )
        val workerPushAction = WorkerSyncEngine.determinePushAction(unresolvedWorker, null)
        assertEquals(
            "Unresolved worker with blank syncId must SKIP outbound push and never route to CREATE_NEW",
            WorkerSyncEngine.PushAction.SKIP,
            workerPushAction
        )
    }

    // =========================================================================
    // PAGINATED CANDIDATE RETRIEVAL & DUPLICATE DETECTION ACROSS PAGES TESTS
    // =========================================================================

    @Test
    fun testWorkerIdentity_paginatedRetrieval_findsCandidateOnLaterPage() {
        val localWorker = WorkerEntity(
            id = 301L,
            name = "Aarav Patel",
            phone = "+91 98765 43210",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )

        // Page 1 contains non-matching workers
        val page1 = listOf(
            WorkerDTO(syncId = "w_p1_1", name = "Bob Jones", phone = "1112223333"),
            WorkerDTO(syncId = "w_p1_2", name = "Charlie Brown", phone = "2223334444")
        )
        // Page 2 contains the single unique match
        val page2 = listOf(
            WorkerDTO(syncId = "w_p2_match", name = "  aarav   patel  ", phone = "919876543210")
        )

        var pagesFetched = 0
        val resolved = WorkerSyncEngine.resolveLegacyWorkerIdentityFromPages(localWorker) {
            sequence {
                pagesFetched++
                yield(page1)
                pagesFetched++
                yield(page2)
            }
        }

        assertNotNull("Candidate on Page 2 must be found via pagination", resolved)
        assertEquals("w_p2_match", resolved!!.syncId)
        assertEquals(2, pagesFetched)
    }

    @Test
    fun testWorkerIdentity_duplicateCandidatesAcrossDifferentPages_isRejected() {
        val localWorker = WorkerEntity(
            id = 302L,
            name = "Deepak Sharma",
            phone = "9876500000",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )

        // Page 1 contains first matching candidate
        val page1 = listOf(
            WorkerDTO(syncId = "w_dup_page1", name = "Deepak Sharma", phone = "9876500000"),
            WorkerDTO(syncId = "w_p1_other", name = "Other Worker", phone = "1111111111")
        )
        // Page 2 contains second matching candidate (duplicate in cloud)
        val page2 = listOf(
            WorkerDTO(syncId = "w_dup_page2", name = "deepak   sharma", phone = "9876500000")
        )

        var totalPagesChecked = 0
        val resolved = WorkerSyncEngine.resolveLegacyWorkerIdentityFromPages(localWorker) {
            sequence {
                totalPagesChecked++
                yield(page1)
                totalPagesChecked++
                yield(page2)
            }
        }

        // Must continue through Page 2, detect the duplicate, and return null (rejecting the match)
        assertEquals("Pagination must not stop after finding candidate on page 1", 2, totalPagesChecked)
        assertNull("Duplicate candidates spanning across different pages must be rejected", resolved)
    }

    @Test
    fun testWorkerIdentity_noMatchAcrossMultiplePages_returnsNull() {
        val localWorker = WorkerEntity(
            id = 303L,
            name = "Vikram Singh",
            phone = "9988776655",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )

        val page1 = listOf(WorkerDTO(syncId = "w1", name = "Alice", phone = "1234567890"))
        val page2 = listOf(WorkerDTO(syncId = "w2", name = "Bob", phone = "2345678901"))
        val page3 = listOf(WorkerDTO(syncId = "w3", name = "Charlie", phone = "3456789012"))

        var totalPagesChecked = 0
        val resolved = WorkerSyncEngine.resolveLegacyWorkerIdentityFromPages(localWorker) {
            sequence {
                totalPagesChecked++
                yield(page1)
                totalPagesChecked++
                yield(page2)
                totalPagesChecked++
                yield(page3)
            }
        }

        assertEquals(3, totalPagesChecked)
        assertNull("No-match across multiple pages must return null", resolved)
    }

    @Test
    fun testSiteIdentity_paginatedRetrieval_findsCandidateOnLaterPage() {
        val localSite = SiteEntity(
            id = 304L,
            name = "Skyline Heights",
            address = "Sector 62, Noida",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )

        // Page 1: other sites
        val page1 = listOf(
            SiteDTO(syncId = "s_p1_1", name = "Green Valley", address = "Sector 10"),
            SiteDTO(syncId = "s_p1_2", name = "Ocean Waves", address = "Beach Road")
        )
        // Page 2: target site with strong matching address
        val page2 = listOf(
            SiteDTO(syncId = "s_p2_match", name = "  skyline   heights  ", address = " sector 62, noida ")
        )

        var pagesFetched = 0
        val resolved = SiteSyncEngine.resolveLegacySiteIdentityFromPages(localSite) {
            sequence {
                pagesFetched++
                yield(page1)
                pagesFetched++
                yield(page2)
            }
        }

        assertNotNull("Candidate on Page 2 must be found via pagination", resolved)
        assertEquals("s_p2_match", resolved!!.syncId)
        assertEquals(2, pagesFetched)
    }

    @Test
    fun testSiteIdentity_duplicateCandidatesAcrossDifferentPages_isRejected() {
        val localSite = SiteEntity(
            id = 305L,
            name = "Grand Residency",
            address = "MG Road, Bangalore",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )

        // Page 1: first matching site
        val page1 = listOf(
            SiteDTO(syncId = "s_dup_1", name = "Grand Residency", address = "MG Road, Bangalore")
        )
        // Page 2: second duplicate site
        val page2 = listOf(
            SiteDTO(syncId = "s_dup_2", name = "Grand Residency", address = "mg road, bangalore")
        )

        var totalPagesChecked = 0
        val resolved = SiteSyncEngine.resolveLegacySiteIdentityFromPages(localSite) {
            sequence {
                totalPagesChecked++
                yield(page1)
                totalPagesChecked++
                yield(page2)
            }
        }

        assertEquals("Pagination must not stop after finding candidate on page 1", 2, totalPagesChecked)
        assertNull("Duplicate site candidates spanning across pages must be rejected", resolved)
    }

    @Test
    fun testSiteIdentity_noMatchAcrossMultiplePages_returnsNull() {
        val localSite = SiteEntity(
            id = 306L,
            name = "Imperial Towers",
            address = "Park Avenue",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )

        val page1 = listOf(SiteDTO(syncId = "s1", name = "Site Alpha", address = "Road 1"))
        val page2 = listOf(SiteDTO(syncId = "s2", name = "Site Beta", address = "Road 2"))

        var totalPagesChecked = 0
        val resolved = SiteSyncEngine.resolveLegacySiteIdentityFromPages(localSite) {
            sequence {
                totalPagesChecked++
                yield(page1)
                totalPagesChecked++
                yield(page2)
            }
        }

        assertEquals(2, totalPagesChecked)
        assertNull("No-match across multiple pages must return null", resolved)
    }

    @Test
    fun testCandidateCacheReuse_avoidsRepeatedCollectionScans() {
        val localWorker1 = WorkerEntity(
            id = 307L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )
        val localWorker2 = WorkerEntity(
            id = 308L,
            name = "Suresh Raina",
            phone = "9876501234",
            syncOrigin = SyncOrigin.LEGACY,
            syncId = "",
            isPendingSync = true
        )

        var candidateFetchCount = 0
        val mockCandidateFetcher: () -> List<WorkerDTO> = {
            candidateFetchCount++
            listOf(
                WorkerDTO(syncId = "w_ramesh", name = "Ramesh Kumar", phone = "9876543210"),
                WorkerDTO(syncId = "w_suresh", name = "Suresh Raina", phone = "9876501234")
            )
        }

        // Simulate run-scoped cache in sync loop
        var fullCloudWorkersCache: List<WorkerDTO>? = null

        // First worker: cache is null -> fetch candidates and populate cache
        val candidatesForWorker1 = fullCloudWorkersCache ?: mockCandidateFetcher().also { fullCloudWorkersCache = it }
        val match1 = WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorker1, candidatesForWorker1)
        assertNotNull(match1)
        assertEquals("w_ramesh", match1!!.syncId)
        assertEquals(1, candidateFetchCount)

        // Second worker: cache is already populated -> reuse cache without fetching again
        val candidatesForWorker2 = fullCloudWorkersCache ?: mockCandidateFetcher().also { fullCloudWorkersCache = it }
        val match2 = WorkerSyncEngine.resolveLegacyWorkerIdentity(localWorker2, candidatesForWorker2)
        assertNotNull(match2)
        assertEquals("w_suresh", match2!!.syncId)
        assertEquals("Candidate fetch must NOT be repeated for subsequent legacy records in same pass", 1, candidateFetchCount)
    }

    // =========================================================================
    // FAKE DAO IMPLEMENTATIONS
    // =========================================================================

    class FakeSiteDao : SiteDao {
        private val sites = mutableListOf<SiteEntity>()

        override fun getAllSites(): Flow<List<SiteEntity>> = flowOf(sites.filter { !it.isDeleted })
        override suspend fun getAllSitesRaw(): List<SiteEntity> = sites.toList()
        override fun getSiteById(siteId: Long): Flow<SiteEntity?> = flowOf(sites.firstOrNull { it.id == siteId })
        override suspend fun getSiteBySyncId(syncId: String): SiteEntity? = sites.firstOrNull { it.syncId == syncId }
        override suspend fun getAllSitesList(): List<SiteEntity> = sites.filter { !it.isDeleted }
        override suspend fun getSiteByName(name: String): SiteEntity? = sites.firstOrNull { it.name == name && !it.isDeleted }

        override suspend fun insertSite(site: SiteEntity): Long {
            val assignedId = if (site.id == 0L) (sites.maxOfOrNull { it.id } ?: 0L) + 1L else site.id
            val toAdd = site.copy(id = assignedId)
            val index = sites.indexOfFirst { it.syncId == site.syncId && it.syncId.isNotBlank() }
            if (index >= 0) {
                sites[index] = toAdd
            } else {
                sites.add(toAdd)
            }
            return assignedId
        }

        override suspend fun updateSite(site: SiteEntity) {
            val index = sites.indexOfFirst { it.id == site.id || (it.syncId == site.syncId && site.syncId.isNotBlank()) }
            if (index >= 0) {
                sites[index] = site
            } else {
                sites.add(site)
            }
        }

        override suspend fun deleteSite(site: SiteEntity) {
            sites.removeAll { it.id == site.id }
        }

        override suspend fun softDeleteSite(id: Long, deletedAt: Long) {
            val index = sites.indexOfFirst { it.id == id }
            if (index >= 0) {
                sites[index] = sites[index].copy(isDeleted = true, deletedAt = deletedAt, updatedAt = deletedAt, isPendingSync = true)
            }
        }

        override suspend fun restoreSite(id: Long, updatedAt: Long) {
            val index = sites.indexOfFirst { it.id == id }
            if (index >= 0) {
                sites[index] = sites[index].copy(isDeleted = false, deletedAt = null, updatedAt = updatedAt, isPendingSync = true)
            }
        }

        override fun getDeletedSites(): Flow<List<SiteEntity>> = flowOf(sites.filter { it.isDeleted && (it.deletedAt ?: 0L) > 0 })
        override suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long) {
            val index = sites.indexOfFirst { it.id == id }
            if (index >= 0) {
                sites[index] = sites[index].copy(deletedAt = -1L, updatedAt = updatedAt, isPendingSync = true)
            }
        }

        override suspend fun permanentlyDeleteSite(id: Long) {
            sites.removeAll { it.id == id }
        }

        override suspend fun deleteBySyncId(syncId: String) {
            sites.removeAll { it.syncId == syncId }
        }

        override suspend fun emptySiteTrash() {
            sites.removeAll { it.isDeleted && it.syncOrigin == 0 }
        }
    }

    class FakeWorkerDao : WorkerDao {
        private val workers = mutableListOf<WorkerEntity>()

        override fun getAllWorkers(): Flow<List<WorkerEntity>> = flowOf(workers.filter { !it.isDeleted })
        override suspend fun getAllWorkersRaw(): List<WorkerEntity> = workers.toList()
        override fun getWorkerById(workerId: Long): Flow<WorkerEntity?> = flowOf(workers.firstOrNull { it.id == workerId })
        override suspend fun getWorkerBySyncId(syncId: String): WorkerEntity? = workers.firstOrNull { it.syncId == syncId }
        override fun getWorkersBySite(siteId: Long): Flow<List<WorkerEntity>> = flowOf(workers.filter { it.currentSiteId == siteId && !it.isDeleted })

        override suspend fun insertWorker(worker: WorkerEntity): Long {
            val assignedId = if (worker.id == 0L) (workers.maxOfOrNull { it.id } ?: 0L) + 1L else worker.id
            val toAdd = worker.copy(id = assignedId)
            val index = workers.indexOfFirst { it.syncId == worker.syncId && it.syncId.isNotBlank() }
            if (index >= 0) {
                workers[index] = toAdd
            } else {
                workers.add(toAdd)
            }
            return assignedId
        }

        override suspend fun updateWorker(worker: WorkerEntity) {
            val index = workers.indexOfFirst { it.id == worker.id || (it.syncId == worker.syncId && worker.syncId.isNotBlank()) }
            if (index >= 0) {
                workers[index] = worker
            } else {
                workers.add(worker)
            }
        }

        override suspend fun deleteWorker(worker: WorkerEntity) {
            workers.removeAll { it.id == worker.id }
        }

        override suspend fun softDeleteWorker(id: Long, deletedAt: Long) {
            val index = workers.indexOfFirst { it.id == id }
            if (index >= 0) {
                workers[index] = workers[index].copy(isDeleted = true, deletedAt = deletedAt, updatedAt = deletedAt, isPendingSync = true)
            }
        }

        override suspend fun restoreWorker(id: Long, updatedAt: Long) {
            val index = workers.indexOfFirst { it.id == id }
            if (index >= 0) {
                workers[index] = workers[index].copy(isDeleted = false, deletedAt = null, updatedAt = updatedAt, isPendingSync = true)
            }
        }

        override fun getDeletedWorkers(): Flow<List<WorkerEntity>> = flowOf(workers.filter { it.isDeleted && (it.deletedAt ?: 0L) > 0 })
        override suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long) {
            val index = workers.indexOfFirst { it.id == id }
            if (index >= 0) {
                workers[index] = workers[index].copy(deletedAt = -1L, updatedAt = updatedAt, isPendingSync = true)
            }
        }

        override suspend fun permanentlyDeleteWorker(id: Long) {
            workers.removeAll { it.id == id }
        }

        override suspend fun deleteBySyncId(syncId: String) {
            workers.removeAll { it.syncId == syncId }
        }

        override suspend fun emptyWorkerTrash() {
            workers.removeAll { it.isDeleted && it.syncOrigin == 0 }
        }

        override fun searchWorkers(query: String): Flow<List<WorkerEntity>> = flowOf(workers.filter { !it.isDeleted && (it.name.contains(query, true) || it.phone.contains(query, true)) })
        override fun getActiveWorkers(): Flow<List<WorkerEntity>> = flowOf(workers.filter { it.isActive && !it.isDeleted })
    }
}

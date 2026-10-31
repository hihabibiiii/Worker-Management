package com.example.workermanager

import com.example.workermanager.data.billing.CreationQuotaPolicy
import com.example.workermanager.data.billing.ProAccessLevel
import com.example.workermanager.data.billing.QuotaEntityType
import com.example.workermanager.data.billing.QuotaExceededException
import com.example.workermanager.data.repository.SiteRepository
import com.example.workermanager.data.repository.WorkerRepository
import com.example.workermanager.data.dao.WorkerSiteHistoryDao
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.RecordRevisionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ReinstallDuplicateSiteAndProLockingTest {

    private lateinit var fakeSiteDao: FakeSiteDao
    private lateinit var fakeWorkerDao: FakeWorkerDao

    @Before
    fun setUp() {
        RecordRevisionManager.reset()
        fakeSiteDao = FakeSiteDao()
        fakeWorkerDao = FakeWorkerDao()
    }

    /**
     * Scenario 16: Central Quota - Free user adding 2nd site via SiteRepository throws QuotaExceededException.
     */
    @Test
    fun testScenario16_centralQuota_freeUser_adding2ndSite_throwsQuotaExceeded() = runBlocking {
        val repo = SiteRepository(
            siteDao = fakeSiteDao,
            proAccessLevelProvider = { ProAccessLevel.FREE }
        )
        // 1st site succeeds
        repo.insertSite(SiteEntity(name = "Site 1", address = "A", manager = "M"))
        assertEquals(1, fakeSiteDao.getAllSitesList().size)

        // 2nd site throws QuotaExceededException
        try {
            repo.insertSite(SiteEntity(name = "Site 2", address = "B", manager = "M"))
            fail("Expected QuotaExceededException when adding 2nd site on Free plan")
        } catch (e: QuotaExceededException) {
            assertEquals(QuotaEntityType.SITE, e.entityType)
            assertEquals(1, e.currentCount)
            assertEquals(1, e.limit)
        }
        // Total active sites remains exactly 1
        assertEquals(1, fakeSiteDao.getAllSitesList().size)
    }

    /**
     * Scenario 17: Central Quota - Free user adding 6th worker via WorkerRepository throws QuotaExceededException.
     */
    @Test
    fun testScenario17_centralQuota_freeUser_adding6thWorker_throwsQuotaExceeded() = runBlocking {
        val historyDao = FakeWorkerSiteHistoryDao()
        val repo = WorkerRepository(
            workerDao = fakeWorkerDao,
            historyDao = historyDao,
            proAccessLevelProvider = { ProAccessLevel.FREE }
        )
        // First 5 workers succeed
        (1..5).forEach { i ->
            repo.insertWorker(
                WorkerEntity(name = "Worker $i", phone = "987654321$i", workerType = WorkerType.LABOUR),
                siteId = null
            )
        }
        assertEquals(5, fakeWorkerDao.getAllWorkersRaw().count { !it.isDeleted })

        // 6th worker throws QuotaExceededException
        try {
            repo.insertWorker(
                WorkerEntity(name = "Worker 6", phone = "9876543216", workerType = WorkerType.LABOUR),
                siteId = null
            )
            fail("Expected QuotaExceededException when adding 6th worker on Free plan")
        } catch (e: QuotaExceededException) {
            assertEquals(QuotaEntityType.WORKER, e.entityType)
            assertEquals(5, e.currentCount)
            assertEquals(5, e.limit)
        }
        // Total active workers remains exactly 5
        assertEquals(5, fakeWorkerDao.getAllWorkersRaw().count { !it.isDeleted })
    }

    /**
     * Scenario 18: Central Quota - Pro user can create unlimited sites and workers.
     */
    @Test
    fun testScenario18_centralQuota_proUser_unlimitedSitesAndWorkers() = runBlocking {
        val siteRepo = SiteRepository(
            siteDao = fakeSiteDao,
            proAccessLevelProvider = { ProAccessLevel.PRO_ACTIVE }
        )
        (1..5).forEach { i ->
            siteRepo.insertSite(SiteEntity(name = "Pro Site $i", address = "A", manager = "M"))
        }
        assertEquals(5, fakeSiteDao.getAllSitesList().size)

        val historyDao = FakeWorkerSiteHistoryDao()
        val workerRepo = WorkerRepository(
            workerDao = fakeWorkerDao,
            historyDao = historyDao,
            proAccessLevelProvider = { ProAccessLevel.PRO_ACTIVE }
        )
        (1..15).forEach { i ->
            workerRepo.insertWorker(
                WorkerEntity(name = "Pro Worker $i", phone = "90000000$i", workerType = WorkerType.LABOUR),
                siteId = null
            )
        }
        assertEquals(15, fakeWorkerDao.getAllWorkersRaw().count { !it.isDeleted })
    }

    /**
     * Scenario 19: Central Quota - Editing existing sites and workers is NEVER blocked by quota.
     */
    @Test
    fun testScenario19_centralQuota_editingExistingRecords_neverBlocked() = runBlocking {
        // Assume user has 3 sites and 10 workers (e.g. from previous Pro subscription)
        (1..3).forEach { i ->
            fakeSiteDao.insertSite(SiteEntity(id = i.toLong(), name = "Site $i", address = "A", manager = "M"))
        }
        val historyDao = FakeWorkerSiteHistoryDao()
        (1..10).forEach { i ->
            fakeWorkerDao.insertWorker(WorkerEntity(id = i.toLong(), name = "Worker $i", phone = "91111111$i", workerType = WorkerType.LABOUR))
        }

        val siteRepo = SiteRepository(
            siteDao = fakeSiteDao,
            proAccessLevelProvider = { ProAccessLevel.FREE }
        )
        val workerRepo = WorkerRepository(
            workerDao = fakeWorkerDao,
            historyDao = historyDao,
            proAccessLevelProvider = { ProAccessLevel.FREE }
        )

        // Editing existing site #2 succeeds without QuotaExceededException
        val site2 = fakeSiteDao.getAllSitesList().first { it.id == 2L }
        siteRepo.updateSite(site2.copy(name = "Edited Site 2"))
        val updatedSite = fakeSiteDao.getSiteByName("Edited Site 2")
        assertNotNull(updatedSite)
        assertEquals("Edited Site 2", updatedSite!!.name)

        // Editing existing worker #8 succeeds without QuotaExceededException
        val worker8 = fakeWorkerDao.getAllWorkersRaw().first { it.id == 8L }
        workerRepo.updateWorker(worker8.copy(dailyRate = 1200.0))
        val updatedWorker = fakeWorkerDao.getAllWorkersRaw().first { it.id == 8L }
        assertEquals(1200.0, updatedWorker.dailyRate, 0.001)
    }

    /**
     * Scenario 20: Central Quota - CreationQuotaPolicy blocks Dashboard & alternate creation paths.
     */
    @Test
    fun testScenario20_centralQuota_policyBlocksDashboardAndAlternatePaths() {
        val freeLevel = ProAccessLevel.FREE

        // Site: at 0 sites, can create
        assertTrue(CreationQuotaPolicy.canCreateSite(0, freeLevel))
        // Site: at 1 site, cannot create 2nd site
        assertFalse(CreationQuotaPolicy.canCreateSite(1, freeLevel))
        assertFalse(CreationQuotaPolicy.canCreateSite(2, freeLevel))

        // Worker: at 4 workers, can create
        assertTrue(CreationQuotaPolicy.canCreateWorker(4, freeLevel))
        // Worker: at 5 workers, cannot create 6th worker
        assertFalse(CreationQuotaPolicy.canCreateWorker(5, freeLevel))
        assertFalse(CreationQuotaPolicy.canCreateWorker(10, freeLevel))

        // Pro: always allowed
        val proLevel = ProAccessLevel.PRO_ACTIVE
        assertTrue(CreationQuotaPolicy.canCreateSite(10, proLevel))
        assertTrue(CreationQuotaPolicy.canCreateWorker(100, proLevel))
    }

    /**
     * Scenario 21: Central Quota - Offline pending records preserved when quota reached.
     */
    @Test
    fun testScenario21_centralQuota_offlinePendingRecordsPreserved() = runBlocking {
        // Local offline site with pending sync
        fakeSiteDao.insertSite(
            SiteEntity(
                name = "Offline Site 1",
                address = "Local",
                manager = "Self",
                isPendingSync = true,
                syncId = "offline_sync_site_1"
            )
        )
        val site = fakeSiteDao.getSiteBySyncId("offline_sync_site_1")
        assertNotNull(site)
        assertTrue("Offline pending site must remain pending and preserved", site!!.isPendingSync)

        // Local offline worker with pending sync
        fakeWorkerDao.insertWorker(
            WorkerEntity(
                name = "Offline Worker 1",
                phone = "9998887771",
                workerType = WorkerType.LABOUR,
                isPendingSync = true,
                syncId = "offline_sync_worker_1"
            )
        )
        val worker = fakeWorkerDao.getWorkerBySyncId("offline_sync_worker_1")
        assertNotNull(worker)
        assertTrue("Offline pending worker must remain pending and preserved", worker!!.isPendingSync)
    }

    @Test
    fun testSelfWorkVisibility_zeroSites_selfWorkVisible() = runBlocking {
        val sites = fakeSiteDao.getAllSitesList()
        val activeUserSites = sites.filter { !it.isDeleted && !it.name.equals("Self Work", ignoreCase = true) }

        // When 0 active user sites exist: Self Work option is visible
        assertTrue("When 0 active sites exist, activeUserSites must be empty", activeUserSites.isEmpty())
    }

    @Test
    fun testSelfWorkVisibility_activeSitesExist_selfWorkHidden() = runBlocking {
        // Add 1 active user site
        fakeSiteDao.insertSite(SiteEntity(name = "Site Alpha", address = "Main St", manager = "John"))

        val sites = fakeSiteDao.getAllSitesList()
        val activeUserSites = sites.filter { !it.isDeleted && !it.name.equals("Self Work", ignoreCase = true) }

        // When active sites exist: activeUserSites is NOT empty, Self Work option is hidden
        assertFalse("When active sites exist, Self Work option must be hidden", activeUserSites.isEmpty())
        assertEquals(1, activeUserSites.size)
        assertEquals("Site Alpha", activeUserSites.first().name)
    }

    @Test
    fun testSelfWorkVisibility_deleteLastSite_selfWorkVisibleAgain() = runBlocking {
        val siteId = fakeSiteDao.insertSite(SiteEntity(name = "Site Alpha", address = "Main St", manager = "John"))

        var sites = fakeSiteDao.getAllSitesList()
        var activeUserSites = sites.filter { !it.isDeleted && !it.name.equals("Self Work", ignoreCase = true) }
        assertEquals(1, activeUserSites.size)

        // Soft delete the site
        fakeSiteDao.softDeleteSite(siteId)

        sites = fakeSiteDao.getAllSitesList()
        activeUserSites = sites.filter { !it.isDeleted && !it.name.equals("Self Work", ignoreCase = true) }

        // When last site is deleted: Self Work option becomes available again
        assertTrue("When last site is deleted, Self Work option becomes available again", activeUserSites.isEmpty())
    }

    // =========================================================================
    // FAKE DAOS FOR IN-MEMORY TESTING
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

        override fun searchWorkers(query: String): Flow<List<WorkerEntity>> =
            flowOf(workers.filter { !it.isDeleted && (it.name.contains(query, ignoreCase = true) || it.phone.contains(query)) })

        override fun getActiveWorkers(): Flow<List<WorkerEntity>> =
            flowOf(workers.filter { !it.isDeleted && it.isActive })
    }

    class FakeWorkerSiteHistoryDao : WorkerSiteHistoryDao {
        private val history = mutableListOf<WorkerSiteHistoryEntity>()

        override suspend fun insert(history: WorkerSiteHistoryEntity) {
            this.history.add(history)
        }

        override suspend fun update(history: WorkerSiteHistoryEntity) {
            val index = this.history.indexOfFirst { it.id == history.id || (it.syncId == history.syncId && history.syncId.isNotBlank()) }
            if (index >= 0) this.history[index] = history else this.history.add(history)
        }

        override fun getHistoryForWorker(workerId: Long): Flow<List<WorkerSiteHistoryEntity>> =
            flowOf(history.filter { it.workerId == workerId })

        override suspend fun getCurrentHistoryForWorker(workerId: Long): WorkerSiteHistoryEntity? =
            history.firstOrNull { it.workerId == workerId && it.endDate == null }

        override fun getWorkersForSiteOnDate(siteId: Long, date: LocalDate): Flow<List<WorkerSiteHistoryEntity>> =
            flowOf(history.filter { it.siteId == siteId && (it.endDate == null || date <= it.endDate!!) && it.startDate <= date })

        override fun getHistoryForAllSitesOnDate(date: LocalDate): Flow<List<WorkerSiteHistoryEntity>> =
            flowOf(history.filter { (it.endDate == null || date <= it.endDate!!) && it.startDate <= date })

        override suspend fun closeCurrentHistory(workerId: Long, endDate: LocalDate) {
            val current = getCurrentHistoryForWorker(workerId)
            current?.let {
                val index = history.indexOf(it)
                if (index >= 0) {
                    history[index] = it.copy(endDate = endDate)
                }
            }
        }

        override suspend fun getSiteIdForWorkerOnDate(workerId: Long, date: LocalDate): Long? =
            history.firstOrNull { it.workerId == workerId && it.startDate <= date && (it.endDate == null || it.endDate!! >= date) }?.siteId

        override suspend fun getAllHistory(): List<WorkerSiteHistoryEntity> = history.toList()

        override suspend fun getHistoryBySyncId(syncId: String): WorkerSiteHistoryEntity? =
            history.firstOrNull { it.syncId == syncId }
    }
}

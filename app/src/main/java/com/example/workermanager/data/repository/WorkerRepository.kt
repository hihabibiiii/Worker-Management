package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.dao.WorkerSiteHistoryDao
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerSiteHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class WorkerRepository(
    private val workerDao: WorkerDao,
    private val historyDao: WorkerSiteHistoryDao,
    private val siteDao: com.example.workermanager.data.dao.SiteDao? = null
) {
    val allWorkers: Flow<List<WorkerEntity>> = workerDao.getAllWorkers()
    val activeWorkers: Flow<List<WorkerEntity>> = workerDao.getActiveWorkers()

    fun getWorkerById(id: Long): Flow<WorkerEntity?> = workerDao.getWorkerById(id)
    fun getWorkersBySite(siteId: Long): Flow<List<WorkerEntity>> = workerDao.getWorkersBySite(siteId)
    fun searchWorkers(query: String): Flow<List<WorkerEntity>> = workerDao.searchWorkers(query)

    suspend fun insertWorker(worker: WorkerEntity, siteId: Long?): Long {
        val workerId = workerDao.insertWorker(worker)
        if (siteId != null) {
            val history = WorkerSiteHistoryEntity(
                workerId = workerId,
                siteId = siteId,
                startDate = LocalDate.now()
            )
            historyDao.insert(history)
            workerDao.updateWorker(worker.copy(id = workerId, currentSiteId = siteId))
        }
        return workerId
    }

    suspend fun updateWorker(worker: WorkerEntity) = workerDao.updateWorker(worker)

    suspend fun deleteWorker(worker: WorkerEntity) = workerDao.deleteWorker(worker)

    suspend fun transferWorker(workerId: Long, newSiteId: Long, transferDate: LocalDate) {
        // 1. Close current history
        historyDao.closeCurrentHistory(workerId, transferDate.minusDays(1))
        
        // 2. Create new history
        val newHistory = WorkerSiteHistoryEntity(
            workerId = workerId,
            siteId = newSiteId,
            startDate = transferDate
        )
        historyDao.insert(newHistory)
        
        // 3. Update worker currentSiteId
        val worker = workerDao.getWorkerById(workerId).first()
        worker?.let {
            workerDao.updateWorker(it.copy(currentSiteId = newSiteId))
        }
    }

    fun getWorkerSiteHistory(workerId: Long): Flow<List<WorkerSiteHistoryEntity>> = 
        historyDao.getHistoryForWorker(workerId)

    fun getWorkersForSiteOnDate(siteId: Long, date: LocalDate): Flow<List<WorkerSiteHistoryEntity>> =
        historyDao.getWorkersForSiteOnDate(siteId, date)

    suspend fun getSiteIdForWorkerOnDate(workerId: Long, date: LocalDate): Long? =
        historyDao.getSiteIdForWorkerOnDate(workerId, date)
}

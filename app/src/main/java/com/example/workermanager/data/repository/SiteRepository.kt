package com.example.workermanager.data.repository

import com.example.workermanager.data.billing.CreationQuotaPolicy
import com.example.workermanager.data.billing.ProAccessLevel
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.sync.MonotonicClock
import com.example.workermanager.data.remote.sync.RecordRevisionManager
import kotlinx.coroutines.flow.Flow

class SiteRepository(
    private val siteDao: SiteDao,
    private val onDataChanged: (() -> Unit)? = null,
    private val proAccessLevelProvider: (() -> ProAccessLevel)? = null
) {
    val allSites: Flow<List<SiteEntity>> = siteDao.getAllSites()

    fun getSiteById(id: Long): Flow<SiteEntity?> = siteDao.getSiteById(id)
    
    suspend fun getSiteByName(name: String): SiteEntity? = siteDao.getSiteByName(name)

    suspend fun canCreateSite(): Boolean {
        val level = proAccessLevelProvider?.invoke() ?: ProAccessLevel.FREE
        val activeCount = siteDao.getAllSitesList().size
        return CreationQuotaPolicy.canCreateSite(activeCount, level)
    }

    suspend fun ensureSelfWorkIfNoSitesExist(): Long? {
        val currentSites = siteDao.getAllSitesList()
        val existing = currentSites.find { it.name.equals("Self Work", ignoreCase = true) }
        if (existing != null) {
            return existing.id
        }
        val selfWorkSite = SiteEntity(
            name = "Self Work",
            address = "Self Work",
            manager = "Default",
            startDate = java.time.LocalDate.now()
        )
        val level = proAccessLevelProvider?.invoke() ?: ProAccessLevel.FREE
        CreationQuotaPolicy.validateSiteCreation(currentSites.size, level)
        val id = siteDao.insertSite(selfWorkSite)
        onDataChanged?.invoke()
        return id
    }
    
    suspend fun insertSite(site: SiteEntity): Long {
        val level = proAccessLevelProvider?.invoke() ?: ProAccessLevel.FREE
        val activeCount = siteDao.getAllSitesList().size
        CreationQuotaPolicy.validateSiteCreation(activeCount, level)
        val id = siteDao.insertSite(site)
        onDataChanged?.invoke()
        return id
    }
    
    suspend fun updateSite(site: SiteEntity) {
        val updated = site.copy(
            updatedAt = MonotonicClock.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (site.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else site.syncOrigin,
            syncId = if (site.syncId.isBlank()) java.util.UUID.randomUUID().toString() else site.syncId
        )
        RecordRevisionManager.recordLocalEdit(updated.syncId)
        siteDao.updateSite(updated)
        onDataChanged?.invoke()
    }
    
    suspend fun deleteSite(site: SiteEntity) {
        if (site.syncId.isNotBlank()) {
            RecordRevisionManager.recordLocalEdit(site.syncId)
        }
        siteDao.softDeleteSite(site.id)
        onDataChanged?.invoke()
    }

    suspend fun restoreSite(id: Long) {
        val site = siteDao.getAllSitesRaw().find { it.id == id }
        if (site != null && site.syncId.isNotBlank()) {
            RecordRevisionManager.recordLocalEdit(site.syncId)
        }
        siteDao.restoreSite(id)
        onDataChanged?.invoke()
    }

    fun getDeletedSites(): Flow<List<SiteEntity>> = siteDao.getDeletedSites()

    suspend fun permanentlyDeleteSite(id: Long) {
        val site = siteDao.getAllSitesRaw().find { it.id == id }
        if (site != null && site.syncId.isNotBlank()) {
            RecordRevisionManager.recordLocalEdit(site.syncId)
            siteDao.markPendingPermanentDelete(id)
        } else {
            siteDao.permanentlyDeleteSite(id)
        }
        onDataChanged?.invoke()
    }

    suspend fun emptySiteTrash() {
        val deleted = siteDao.getAllSitesRaw().filter { it.isDeleted }
        for (site in deleted) {
            if (site.syncId.isNotBlank()) {
                siteDao.markPendingPermanentDelete(site.id)
            } else {
                siteDao.permanentlyDeleteSite(site.id)
            }
        }
        onDataChanged?.invoke()
    }
}

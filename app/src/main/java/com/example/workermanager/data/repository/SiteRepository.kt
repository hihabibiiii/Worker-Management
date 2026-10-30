package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow

class SiteRepository(
    private val siteDao: SiteDao,
    private val onDataChanged: (() -> Unit)? = null
) {
    val allSites: Flow<List<SiteEntity>> = siteDao.getAllSites()

    fun getSiteById(id: Long): Flow<SiteEntity?> = siteDao.getSiteById(id)
    
    suspend fun getSiteByName(name: String): SiteEntity? = siteDao.getSiteByName(name)

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
        val id = siteDao.insertSite(selfWorkSite)
        onDataChanged?.invoke()
        return id
    }
    
    suspend fun insertSite(site: SiteEntity): Long {
        val id = siteDao.insertSite(site)
        onDataChanged?.invoke()
        return id
    }
    
    suspend fun updateSite(site: SiteEntity) {
        val updated = site.copy(
            updatedAt = System.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (site.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else site.syncOrigin,
            syncId = if (site.syncId.isBlank()) java.util.UUID.randomUUID().toString() else site.syncId
        )
        siteDao.updateSite(updated)
        onDataChanged?.invoke()
    }
    
    suspend fun deleteSite(site: SiteEntity) {
        siteDao.softDeleteSite(site.id)
        onDataChanged?.invoke()
    }

    suspend fun restoreSite(id: Long) {
        siteDao.restoreSite(id)
        onDataChanged?.invoke()
    }

    fun getDeletedSites(): Flow<List<SiteEntity>> = siteDao.getDeletedSites()

    suspend fun permanentlyDeleteSite(id: Long) {
        val site = siteDao.getAllSitesRaw().find { it.id == id }
        if (site != null && site.syncId.isNotBlank()) {
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

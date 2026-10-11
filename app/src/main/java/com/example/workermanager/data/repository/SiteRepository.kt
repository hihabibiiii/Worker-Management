package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

class SiteRepository(private val siteDao: SiteDao) {
    val allSites: Flow<List<SiteEntity>> = siteDao.getAllSites()

    fun getSiteById(id: Long): Flow<SiteEntity?> = siteDao.getSiteById(id)
    
    suspend fun getSiteByName(name: String): SiteEntity? = siteDao.getSiteByName(name)

    suspend fun ensureSelfWorkIfNoSitesExist(): Long? {
        val currentSites = siteDao.getAllSitesList()
        if (currentSites.isEmpty()) {
            val selfWorkSite = SiteEntity(
                name = "Self Work",
                address = "Self Work",
                manager = "Default",
                startDate = java.time.LocalDate.now()
            )
            return siteDao.insertSite(selfWorkSite)
        }
        return null
    }
    
    suspend fun insertSite(site: SiteEntity) = siteDao.insertSite(site)
    
    suspend fun updateSite(site: SiteEntity) = siteDao.updateSite(site)
    
    suspend fun deleteSite(site: SiteEntity) = siteDao.softDeleteSite(site.id)

    suspend fun restoreSite(id: Long) = siteDao.restoreSite(id)

    fun getDeletedSites(): Flow<List<SiteEntity>> = siteDao.getDeletedSites()

    suspend fun permanentlyDeleteSite(id: Long) = siteDao.permanentlyDeleteSite(id)

    suspend fun emptySiteTrash() = siteDao.emptySiteTrash()
}

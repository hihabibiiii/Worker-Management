package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

class SiteRepository(private val siteDao: SiteDao) {
    val allSites: Flow<List<SiteEntity>> = siteDao.getAllSites()

    fun getSiteById(id: Long): Flow<SiteEntity?> = siteDao.getSiteById(id)
    
    suspend fun getSiteByName(name: String): SiteEntity? = siteDao.getSiteByName(name)

    suspend fun ensureDefaultHomeSite(): SiteEntity {
        val existing = siteDao.getSiteByName("Home")
        if (existing != null) {
            return existing
        }
        val homeSite = SiteEntity(
            name = "Home",
            address = "Home",
            manager = "Default",
            startDate = java.time.LocalDate.now()
        )
        val id = siteDao.insertSite(homeSite)
        return homeSite.copy(id = id)
    }
    
    suspend fun insertSite(site: SiteEntity) = siteDao.insertSite(site)
    
    suspend fun updateSite(site: SiteEntity) = siteDao.updateSite(site)
    
    suspend fun deleteSite(site: SiteEntity) = siteDao.deleteSite(site)
}

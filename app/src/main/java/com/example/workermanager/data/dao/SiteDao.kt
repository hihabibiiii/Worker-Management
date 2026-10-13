package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites")
    suspend fun getAllSitesRaw(): List<SiteEntity>

    @Query("SELECT * FROM sites WHERE id = :siteId")
    fun getSiteById(siteId: Long): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllSitesList(): List<SiteEntity>

    @Query("SELECT * FROM sites WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getSiteByName(name: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("UPDATE sites SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteSite(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sites SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreSite(id: Long)

    @Query("SELECT * FROM sites WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedSites(): Flow<List<SiteEntity>>

    @Query("DELETE FROM sites WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteSite(id: Long)

    @Query("DELETE FROM sites WHERE isDeleted = 1")
    suspend fun emptySiteTrash()
}

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

    @Query("SELECT * FROM sites WHERE syncId = :syncId LIMIT 1")
    suspend fun getSiteBySyncId(syncId: String): SiteEntity?

    @Query("SELECT * FROM sites WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllSitesList(): List<SiteEntity>

    @Query("SELECT * FROM sites WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getSiteByName(name: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    /**
     * Idempotently inserts or updates a site by its stable syncId.
     * Prevents duplicate rows in Room when multiple sync paths or repeated syncs process the same record.
     */
    suspend fun upsertSiteBySyncId(site: SiteEntity): Long {
        if (site.syncId.isNotBlank()) {
            val existing = getSiteBySyncId(site.syncId)
            if (existing != null) {
                updateSite(site.copy(id = existing.id))
                return existing.id
            }
        }
        return insertSite(site)
    }

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("UPDATE sites SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteSite(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sites SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restoreSite(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM sites WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC")
    fun getDeletedSites(): Flow<List<SiteEntity>>

    @Query("UPDATE sites SET deletedAt = -1, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sites WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteSite(id: Long)

    @Query("DELETE FROM sites WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM sites WHERE isDeleted = 1 AND syncOrigin = 0")
    suspend fun emptySiteTrash()
}

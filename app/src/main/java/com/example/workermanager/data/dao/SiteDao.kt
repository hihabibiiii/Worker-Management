package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :siteId")
    fun getSiteById(siteId: Long): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSiteByName(name: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)
}

package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.SiteAttendanceEntity
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.sync.dto.SiteAttendanceDTO
import java.time.LocalDate

object SiteAttendanceMapper {
    fun toDTO(entity: SiteAttendanceEntity, siteSyncId: String): SiteAttendanceDTO {
        return SiteAttendanceDTO(
            syncId = entity.syncId,
            siteSyncId = siteSyncId,
            date = entity.date.toString(),
            isPresent = entity.isPresent,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: SiteAttendanceDTO, localId: Long = 0L, localSiteId: Long): SiteAttendanceEntity {
        val dateParsed = try { LocalDate.parse(dto.date) } catch (_: Exception) { LocalDate.now() }

        return SiteAttendanceEntity(
            id = localId,
            siteId = localSiteId,
            date = dateParsed,
            isPresent = dto.isPresent,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }
}

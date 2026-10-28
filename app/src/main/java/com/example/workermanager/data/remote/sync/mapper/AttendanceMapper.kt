package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.sync.dto.AttendanceDTO
import java.time.LocalDate

object AttendanceMapper {
    fun toDTO(entity: AttendanceEntity, workerSyncId: String, siteSyncId: String): AttendanceDTO {
        return AttendanceDTO(
            syncId = entity.syncId,
            workerSyncId = workerSyncId,
            siteSyncId = siteSyncId,
            date = entity.date.toString(),
            status = entity.status.name,
            overtimeHours = entity.overtimeHours,
            overtimeAmount = entity.overtimeAmount,
            amount = entity.amount,
            note = entity.note,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: AttendanceDTO, localId: Long = 0L, localWorkerId: Long, localSiteId: Long): AttendanceEntity {
        val statusEnum = try { AttendanceStatus.valueOf(dto.status) } catch (_: Exception) { AttendanceStatus.PRESENT }
        val dateParsed = try { LocalDate.parse(dto.date) } catch (_: Exception) { LocalDate.now() }

        return AttendanceEntity(
            id = localId,
            workerId = localWorkerId,
            siteId = localSiteId,
            date = dateParsed,
            status = statusEnum,
            overtimeHours = dto.overtimeHours,
            overtimeAmount = dto.overtimeAmount,
            amount = dto.amount,
            note = dto.note,
            createdAt = dto.createdAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }
}

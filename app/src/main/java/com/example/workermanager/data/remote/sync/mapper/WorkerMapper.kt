package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.data.remote.sync.dto.WorkerDTO
import java.time.LocalDate

object WorkerMapper {
    fun toDTO(entity: WorkerEntity, siteSyncId: String? = null): WorkerDTO {
        return WorkerDTO(
            syncId = entity.syncId,
            name = entity.name,
            phone = entity.phone,
            workerType = entity.workerType.name,
            dailyRate = entity.dailyRate,
            overtimeRate = entity.overtimeRate,
            joiningDate = entity.joiningDate.toString(),
            siteSyncId = siteSyncId,
            isActive = entity.isActive,
            notes = entity.notes,
            photoPath = entity.photoPath,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: WorkerDTO, localId: Long = 0L, localSiteId: Long? = null): WorkerEntity {
        val typeEnum = try {
            WorkerType.valueOf(dto.workerType)
        } catch (_: Exception) {
            WorkerType.LABOUR
        }
        val dateParsed = try {
            LocalDate.parse(dto.joiningDate)
        } catch (_: Exception) {
            LocalDate.now()
        }

        return WorkerEntity(
            id = localId,
            name = dto.name,
            phone = dto.phone,
            workerType = typeEnum,
            dailyRate = dto.dailyRate,
            overtimeRate = dto.overtimeRate,
            joiningDate = dateParsed,
            currentSiteId = localSiteId,
            isActive = dto.isActive,
            notes = dto.notes,
            photoPath = dto.photoPath,
            isDeleted = dto.isDeleted,
            deletedAt = dto.deletedAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }
}

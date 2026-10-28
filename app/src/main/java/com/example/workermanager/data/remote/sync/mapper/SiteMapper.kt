package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.sync.dto.SiteDTO
import java.time.LocalDate

object SiteMapper {
    fun toDTO(entity: SiteEntity): SiteDTO {
        return SiteDTO(
            syncId = entity.syncId,
            name = entity.name,
            address = entity.address,
            manager = entity.manager,
            startDate = entity.startDate.toString(),
            status = entity.status.name,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: SiteDTO, localId: Long = 0L): SiteEntity {
        val statusEnum = try {
            SiteStatus.valueOf(dto.status)
        } catch (_: Exception) {
            SiteStatus.ACTIVE
        }
        val dateParsed = try {
            LocalDate.parse(dto.startDate)
        } catch (_: Exception) {
            LocalDate.now()
        }

        return SiteEntity(
            id = localId,
            name = dto.name,
            address = dto.address,
            manager = dto.manager,
            startDate = dateParsed,
            status = statusEnum,
            isDeleted = dto.isDeleted,
            deletedAt = dto.deletedAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }
}

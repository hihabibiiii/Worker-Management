package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.ContractEntity
import com.example.workermanager.data.entity.ContractStatus
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.sync.dto.ContractDTO
import java.time.LocalDate

object ContractMapper {
    fun toDTO(entity: ContractEntity, siteSyncId: String? = null): ContractDTO {
        return ContractDTO(
            syncId = entity.syncId,
            clientName = entity.clientName,
            mobile = entity.mobile,
            siteSyncId = siteSyncId,
            totalAmount = entity.totalAmount,
            startDate = entity.startDate.toString(),
            notes = entity.notes,
            status = entity.status.name,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: ContractDTO, localId: Long = 0L, localSiteId: Long? = null): ContractEntity {
        val statusEnum = try { ContractStatus.valueOf(dto.status) } catch (_: Exception) { ContractStatus.ACTIVE }
        val dateParsed = try { LocalDate.parse(dto.startDate) } catch (_: Exception) { LocalDate.now() }

        return ContractEntity(
            id = localId,
            clientName = dto.clientName,
            mobile = dto.mobile,
            siteId = localSiteId,
            totalAmount = dto.totalAmount,
            startDate = dateParsed,
            notes = dto.notes,
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

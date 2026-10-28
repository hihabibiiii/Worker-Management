package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.dto.ContractorMaterialTransactionDTO
import java.time.LocalDate

object ContractorMaterialTransactionMapper {
    fun toDTO(
        entity: ContractorMaterialTransactionEntity,
        materialSyncId: String,
        siteSyncId: String? = null
    ): ContractorMaterialTransactionDTO {
        return ContractorMaterialTransactionDTO(
            syncId = entity.syncId,
            materialSyncId = materialSyncId,
            siteSyncId = siteSyncId,
            transactionType = entity.transactionType.name,
            transactionDate = entity.transactionDate.toString(),
            quantity = entity.quantity,
            unit = entity.unit,
            reasonOrNotes = entity.reasonOrNotes,
            createdAt = entity.createdAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(
        dto: ContractorMaterialTransactionDTO,
        localId: Long = 0L,
        localMaterialId: Long,
        localSiteId: Long? = null
    ): ContractorMaterialTransactionEntity {
        val typeEnum = try { ContractorMaterialTransactionType.valueOf(dto.transactionType) } catch (_: Exception) { ContractorMaterialTransactionType.ADDED }
        val dateParsed = try { LocalDate.parse(dto.transactionDate) } catch (_: Exception) { LocalDate.now() }

        return ContractorMaterialTransactionEntity(
            id = localId,
            materialId = localMaterialId,
            siteId = localSiteId,
            transactionType = typeEnum,
            transactionDate = dateParsed,
            quantity = dto.quantity,
            unit = dto.unit,
            reasonOrNotes = dto.reasonOrNotes,
            createdAt = dto.createdAt,
            isDeleted = dto.isDeleted,
            deletedAt = dto.deletedAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }
}

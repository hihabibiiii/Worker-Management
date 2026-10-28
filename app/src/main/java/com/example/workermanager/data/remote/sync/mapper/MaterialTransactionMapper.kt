package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.dto.MaterialTransactionDTO
import java.time.LocalDate

object MaterialTransactionMapper {
    fun toDTO(
        entity: MaterialTransactionEntity,
        materialSyncId: String,
        siteSyncId: String,
        targetSiteSyncId: String? = null,
        linkedExpenseSyncId: String? = null
    ): MaterialTransactionDTO {
        return MaterialTransactionDTO(
            syncId = entity.syncId,
            materialSyncId = materialSyncId,
            siteSyncId = siteSyncId,
            transactionType = entity.transactionType.name,
            transactionDate = entity.transactionDate.toString(),
            quantity = entity.quantity,
            unit = entity.unit,
            ratePerUnit = entity.ratePerUnit,
            totalAmount = entity.totalAmount,
            supplierName = entity.supplierName,
            supplierPhone = entity.supplierPhone,
            invoiceNumber = entity.invoiceNumber,
            workDescription = entity.workDescription,
            targetSiteSyncId = targetSiteSyncId,
            linkedExpenseSyncId = linkedExpenseSyncId,
            notes = entity.notes,
            createdAt = entity.createdAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(
        dto: MaterialTransactionDTO,
        localId: Long = 0L,
        localMaterialId: Long,
        localSiteId: Long,
        localTargetSiteId: Long? = null,
        localLinkedExpenseId: Long? = null
    ): MaterialTransactionEntity {
        val typeEnum = try { MaterialTransactionType.valueOf(dto.transactionType) } catch (_: Exception) { MaterialTransactionType.RECEIVED }
        val dateParsed = try { LocalDate.parse(dto.transactionDate) } catch (_: Exception) { LocalDate.now() }

        return MaterialTransactionEntity(
            id = localId,
            materialId = localMaterialId,
            siteId = localSiteId,
            transactionType = typeEnum,
            transactionDate = dateParsed,
            quantity = dto.quantity,
            unit = dto.unit,
            ratePerUnit = dto.ratePerUnit,
            totalAmount = dto.totalAmount,
            supplierName = dto.supplierName,
            supplierPhone = dto.supplierPhone,
            invoiceNumber = dto.invoiceNumber,
            workDescription = dto.workDescription,
            targetSiteId = localTargetSiteId,
            linkedExpenseId = localLinkedExpenseId,
            notes = dto.notes,
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

package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.dto.SiteExpenseDTO
import java.time.LocalDate

object SiteExpenseMapper {
    fun toDTO(entity: SiteExpenseEntity, siteSyncId: String): SiteExpenseDTO {
        return SiteExpenseDTO(
            syncId = entity.syncId,
            siteSyncId = siteSyncId,
            expenseDate = entity.expenseDate.toString(),
            category = entity.category.name,
            description = entity.description,
            quantity = entity.quantity,
            rate = entity.rate,
            amount = entity.amount,
            paidBy = entity.paidBy,
            paymentMethod = entity.paymentMethod.name,
            notes = entity.notes,
            receiptPath = entity.receiptPath,
            createdAt = entity.createdAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: SiteExpenseDTO, localId: Long = 0L, localSiteId: Long): SiteExpenseEntity {
        val catEnum = try { ExpenseCategory.valueOf(dto.category) } catch (_: Exception) { ExpenseCategory.OTHER }
        val methodEnum = try { ExpensePaymentMethod.valueOf(dto.paymentMethod) } catch (_: Exception) { ExpensePaymentMethod.CASH }
        val dateParsed = try { LocalDate.parse(dto.expenseDate) } catch (_: Exception) { LocalDate.now() }

        return SiteExpenseEntity(
            id = localId,
            siteId = localSiteId,
            expenseDate = dateParsed,
            category = catEnum,
            description = dto.description,
            quantity = dto.quantity,
            rate = dto.rate,
            amount = dto.amount,
            paidBy = dto.paidBy,
            paymentMethod = methodEnum,
            notes = dto.notes,
            receiptPath = dto.receiptPath,
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

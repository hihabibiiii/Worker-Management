package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.dto.SiteExpensePaymentDTO
import java.time.LocalDate

object SiteExpensePaymentMapper {
    fun toDTO(entity: SiteExpensePaymentEntity, expenseSyncId: String): SiteExpensePaymentDTO {
        return SiteExpensePaymentDTO(
            syncId = entity.syncId,
            expenseSyncId = expenseSyncId,
            paymentDate = entity.paymentDate.toString(),
            amount = entity.amount,
            paidBy = entity.paidBy,
            paymentMethod = entity.paymentMethod.name,
            notes = entity.notes,
            createdAt = entity.createdAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: SiteExpensePaymentDTO, localId: Long = 0L, localExpenseId: Long): SiteExpensePaymentEntity {
        val methodEnum = try { ExpensePaymentMethod.valueOf(dto.paymentMethod) } catch (_: Exception) { ExpensePaymentMethod.CASH }
        val dateParsed = try { LocalDate.parse(dto.paymentDate) } catch (_: Exception) { LocalDate.now() }

        return SiteExpensePaymentEntity(
            id = localId,
            expenseId = localExpenseId,
            paymentDate = dateParsed,
            amount = dto.amount,
            paidBy = dto.paidBy,
            paymentMethod = methodEnum,
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

package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.ClientPaymentEntity
import com.example.workermanager.data.entity.ClientPaymentMethod
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.sync.dto.ClientPaymentDTO
import java.time.LocalDate

object ClientPaymentMapper {
    fun toDTO(entity: ClientPaymentEntity, contractSyncId: String): ClientPaymentDTO {
        return ClientPaymentDTO(
            syncId = entity.syncId,
            contractSyncId = contractSyncId,
            amount = entity.amount,
            paymentDate = entity.paymentDate.toString(),
            method = entity.method.name,
            note = entity.note,
            createdAt = entity.createdAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            isEdited = entity.isEdited,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: ClientPaymentDTO, localId: Long = 0L, localContractId: Long): ClientPaymentEntity {
        val methodEnum = try { ClientPaymentMethod.valueOf(dto.method) } catch (_: Exception) { ClientPaymentMethod.CASH }
        val dateParsed = try { LocalDate.parse(dto.paymentDate) } catch (_: Exception) { LocalDate.now() }

        return ClientPaymentEntity(
            id = localId,
            contractId = localContractId,
            amount = dto.amount,
            paymentDate = dateParsed,
            method = methodEnum,
            note = dto.note,
            createdAt = dto.createdAt,
            isDeleted = dto.isDeleted,
            deletedAt = dto.deletedAt,
            isEdited = dto.isEdited,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }
}

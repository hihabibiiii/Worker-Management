package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.dto.PaymentDTO
import java.time.LocalDate

object PaymentMapper {
    fun toDTO(entity: PaymentEntity, workerSyncId: String, siteSyncId: String): PaymentDTO {
        return PaymentDTO(
            syncId = entity.syncId,
            workerSyncId = workerSyncId,
            siteSyncId = siteSyncId,
            amount = entity.amount,
            date = entity.date.toString(),
            method = entity.method.name,
            paymentType = entity.paymentType.name,
            note = entity.note,
            createdAt = entity.createdAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: PaymentDTO, localId: Long = 0L, localWorkerId: Long, localSiteId: Long): PaymentEntity {
        val methodEnum = try { PaymentMethod.valueOf(dto.method) } catch (_: Exception) { PaymentMethod.CASH }
        val typeEnum = try { PaymentType.valueOf(dto.paymentType) } catch (_: Exception) { PaymentType.PAYMENT }
        val dateParsed = try { LocalDate.parse(dto.date) } catch (_: Exception) { LocalDate.now() }

        return PaymentEntity(
            id = localId,
            workerId = localWorkerId,
            siteId = localSiteId,
            amount = dto.amount,
            date = dateParsed,
            method = methodEnum,
            paymentType = typeEnum,
            note = dto.note,
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

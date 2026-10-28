package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.dto.ContractorMaterialDTO

object ContractorMaterialMapper {
    fun toDTO(entity: ContractorMaterialEntity): ContractorMaterialDTO {
        return ContractorMaterialDTO(
            syncId = entity.syncId,
            name = entity.name,
            category = entity.category.name,
            unit = entity.unit.name,
            minStockLevel = entity.minStockLevel,
            notes = entity.notes,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    fun toEntity(dto: ContractorMaterialDTO, localId: Long = 0L): ContractorMaterialEntity {
        val catEnum = try { ContractorMaterialCategory.valueOf(dto.category) } catch (_: Exception) { ContractorMaterialCategory.OTHER }
        val unitEnum = try { ContractorMaterialUnit.valueOf(dto.unit) } catch (_: Exception) { ContractorMaterialUnit.PCS }

        return ContractorMaterialEntity(
            id = localId,
            name = dto.name,
            category = catEnum,
            unit = unitEnum,
            minStockLevel = dto.minStockLevel,
            notes = dto.notes,
            isActive = dto.isActive,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            isDeleted = dto.isDeleted,
            deletedAt = dto.deletedAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            isPendingSync = false
        )
    }
}

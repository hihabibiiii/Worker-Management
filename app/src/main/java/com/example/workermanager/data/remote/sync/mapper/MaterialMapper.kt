package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.*
import com.example.workermanager.data.remote.sync.dto.MaterialDTO

object MaterialMapper {
    fun toDTO(entity: MaterialEntity): MaterialDTO {
        return MaterialDTO(
            syncId = entity.syncId,
            name = entity.name,
            category = entity.category.name,
            unit = entity.unit.name,
            minStockLevel = entity.minStockLevel,
            createdAt = entity.createdAt,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: MaterialDTO, localId: Long = 0L): MaterialEntity {
        val catEnum = try { MaterialCategory.valueOf(dto.category) } catch (_: Exception) { MaterialCategory.OTHER }
        val unitEnum = try { MaterialUnit.valueOf(dto.unit) } catch (_: Exception) { MaterialUnit.BAGS }

        return MaterialEntity(
            id = localId,
            name = dto.name,
            category = catEnum,
            unit = unitEnum,
            minStockLevel = dto.minStockLevel,
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

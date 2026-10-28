package com.example.workermanager.data.remote.sync.mapper

import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerSiteHistoryEntity
import com.example.workermanager.data.remote.sync.dto.WorkerSiteHistoryDTO
import java.time.LocalDate

object WorkerSiteHistoryMapper {
    fun toDTO(entity: WorkerSiteHistoryEntity, workerSyncId: String, siteSyncId: String): WorkerSiteHistoryDTO {
        return WorkerSiteHistoryDTO(
            syncId = entity.syncId,
            workerSyncId = workerSyncId,
            siteSyncId = siteSyncId,
            startDate = entity.startDate.toString(),
            endDate = entity.endDate?.toString(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: WorkerSiteHistoryDTO, localId: Long = 0L, localWorkerId: Long, localSiteId: Long): WorkerSiteHistoryEntity {
        val startParsed = try { LocalDate.parse(dto.startDate) } catch (_: Exception) { LocalDate.now() }
        val endParsed = dto.endDate?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } }

        return WorkerSiteHistoryEntity(
            id = localId,
            workerId = localWorkerId,
            siteId = localSiteId,
            startDate = startParsed,
            endDate = endParsed,
            createdAt = dto.createdAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }
}

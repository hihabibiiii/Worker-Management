package com.example.workermanager.data.remote.sync.mapper

import android.util.Log
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.dto.SiteDTO
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

object SiteMapper {
    private const val TAG = "SiteMapper"

    fun toDTO(entity: SiteEntity): SiteDTO {
        return SiteDTO(
            syncId = entity.syncId,
            name = entity.name,
            address = entity.address,
            manager = entity.manager,
            startDate = entity.startDate.toString(),
            status = entity.status.name,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: SiteDTO, localId: Long = 0L): SiteEntity {
        val statusEnum = try {
            SiteStatus.valueOf(dto.status)
        } catch (_: Exception) {
            SiteStatus.ACTIVE
        }
        val dateParsed = try {
            LocalDate.parse(dto.startDate)
        } catch (_: Exception) {
            LocalDate.now()
        }

        val isDeletedEffective = dto.isDeleted || ((dto.deletedAt ?: 0L) > 0L)

        return SiteEntity(
            id = localId,
            name = dto.name,
            address = dto.address,
            manager = dto.manager,
            startDate = dateParsed,
            status = statusEnum,
            isDeleted = isDeletedEffective,
            deletedAt = dto.deletedAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }

    /**
     * Converts a raw map into SiteDTO, safely normalizing timestamps (Long, Timestamp, Date, String)
     * and missing fields without throwing deserialization exceptions.
     */
    fun fromMap(data: Map<String, Any?>, docId: String = ""): SiteDTO {
        val syncIdRaw = (data["syncId"] as? String)?.takeIf { it.isNotBlank() } ?: docId
        val name = (data["name"] as? String) ?: ""
        val address = (data["address"] as? String) ?: ""
        val manager = (data["manager"] as? String) ?: ""
        val startDate = (data["startDate"] as? String) ?: ""
        val status = (data["status"] as? String) ?: "ACTIVE"
        val deletedAt = FirestoreSyncHelper.parseTimestampValue(data["deletedAt"])
        val isDeleted = ((data["isDeleted"] as? Boolean) ?: false) || ((deletedAt ?: 0L) > 0L)
        val updatedAt = FirestoreSyncHelper.parseTimestampValue(data["updatedAt"]) ?: 0L

        return SiteDTO(
            syncId = syncIdRaw,
            name = name,
            address = address,
            manager = manager,
            startDate = startDate,
            status = status,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Deserializes a DocumentSnapshot into SiteDTO, resilient against Firestore Timestamp objects
     * and epoch millisecond numbers.
     */
    fun fromSnapshot(doc: DocumentSnapshot): SiteDTO? {
        return try {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            fromMap(data, doc.id)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse SiteDTO from snapshot ${doc.id}: ${e.message}")
            null
        }
    }
}

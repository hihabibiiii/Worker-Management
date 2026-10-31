package com.example.workermanager.data.remote.sync.mapper

import android.util.Log
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.dto.WorkerDTO
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

object WorkerMapper {
    private const val TAG = "WorkerMapper"

    fun toDTO(entity: WorkerEntity, siteSyncId: String? = null): WorkerDTO {
        return WorkerDTO(
            syncId = entity.syncId,
            name = entity.name,
            phone = entity.phone,
            workerType = entity.workerType.name,
            dailyRate = entity.dailyRate,
            overtimeRate = entity.overtimeRate,
            joiningDate = entity.joiningDate.toString(),
            siteSyncId = siteSyncId,
            isActive = entity.isActive,
            notes = entity.notes,
            photoPath = entity.photoPath,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: WorkerDTO, localId: Long = 0L, localSiteId: Long? = null): WorkerEntity {
        val typeEnum = try {
            WorkerType.valueOf(dto.workerType)
        } catch (_: Exception) {
            WorkerType.LABOUR
        }
        val dateParsed = try {
            LocalDate.parse(dto.joiningDate)
        } catch (_: Exception) {
            LocalDate.now()
        }

        val isDeletedEffective = dto.isDeleted || ((dto.deletedAt ?: 0L) > 0L)

        return WorkerEntity(
            id = localId,
            name = dto.name,
            phone = dto.phone,
            workerType = typeEnum,
            dailyRate = dto.dailyRate,
            overtimeRate = dto.overtimeRate,
            joiningDate = dateParsed,
            currentSiteId = localSiteId,
            isActive = dto.isActive,
            notes = dto.notes,
            photoPath = dto.photoPath,
            isDeleted = isDeletedEffective,
            deletedAt = dto.deletedAt,
            syncOrigin = SyncOrigin.CLOUD_SYNCED,
            syncId = dto.syncId,
            updatedAt = dto.updatedAt,
            isPendingSync = false
        )
    }

    /**
     * Converts a raw map into WorkerDTO, safely normalizing timestamps (Long, Timestamp, Date, String)
     * and missing fields without throwing deserialization exceptions.
     */
    fun fromMap(data: Map<String, Any?>, docId: String = ""): WorkerDTO {
        val syncIdRaw = (data["syncId"] as? String)?.takeIf { it.isNotBlank() } ?: docId
        val name = (data["name"] as? String) ?: ""
        val phone = (data["phone"] as? String) ?: ""
        val workerType = (data["workerType"] as? String) ?: "LABOUR"
        val dailyRate = when (val r = data["dailyRate"]) {
            is Number -> r.toDouble()
            is String -> r.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val overtimeRate = when (val r = data["overtimeRate"]) {
            is Number -> r.toDouble()
            is String -> r.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val joiningDate = (data["joiningDate"] as? String) ?: ""
        val siteSyncId = data["siteSyncId"] as? String
        val isActive = (data["isActive"] as? Boolean) ?: true
        val notes = (data["notes"] as? String) ?: ""
        val photoPath = data["photoPath"] as? String
        val deletedAt = FirestoreSyncHelper.parseTimestampValue(data["deletedAt"])
        val isDeleted = ((data["isDeleted"] as? Boolean) ?: false) || ((deletedAt ?: 0L) > 0L)
        val updatedAt = FirestoreSyncHelper.parseTimestampValue(data["updatedAt"]) ?: 0L

        return WorkerDTO(
            syncId = syncIdRaw,
            name = name,
            phone = phone,
            workerType = workerType,
            dailyRate = dailyRate,
            overtimeRate = overtimeRate,
            joiningDate = joiningDate,
            siteSyncId = siteSyncId,
            isActive = isActive,
            notes = notes,
            photoPath = photoPath,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Deserializes a DocumentSnapshot into WorkerDTO, resilient against Firestore Timestamp objects
     * and epoch millisecond numbers.
     */
    fun fromSnapshot(doc: DocumentSnapshot): WorkerDTO? {
        return try {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            fromMap(data, doc.id)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse WorkerDTO from snapshot ${doc.id}: ${e.message}")
            null
        }
    }
}

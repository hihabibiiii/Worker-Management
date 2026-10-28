package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class SiteStatus {
    ACTIVE, COMPLETED, ON_HOLD
}

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val manager: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val status: SiteStatus = SiteStatus.ACTIVE,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncOrigin: Int = SyncOrigin.NEW_LOCAL,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = true
)

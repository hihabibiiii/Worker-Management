package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ContractStatus {
    ACTIVE, COMPLETED
}

@Entity(
    tableName = "contracts",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ContractEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val mobile: String = "",
    val siteId: Long?,
    val totalAmount: Double,
    val startDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val status: ContractStatus = ContractStatus.ACTIVE,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncOrigin: Int = SyncOrigin.NEW_LOCAL,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = true
)

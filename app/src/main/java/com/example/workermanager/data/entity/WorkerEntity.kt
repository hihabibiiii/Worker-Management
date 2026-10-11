package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class WorkerType {
    MISTRI, LABOUR, HELPER, PAINTER, ELECTRICIAN, PLUMBER, OTHER
}

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val workerType: WorkerType = WorkerType.LABOUR,
    val dailyRate: Double = 0.0,
    val overtimeRate: Double = 0.0,
    val joiningDate: LocalDate = LocalDate.now(),
    val currentSiteId: Long? = null,
    val isActive: Boolean = true,
    val notes: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

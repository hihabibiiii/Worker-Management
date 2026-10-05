package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class AttendanceStatus {
    PRESENT, HALF_DAY, ABSENT, OVERTIME, OFF
}

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workerId", "siteId", "date"], unique = true)]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long,
    val siteId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    val overtimeHours: Double = 0.0,
    val overtimeAmount: Double = 0.0,
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

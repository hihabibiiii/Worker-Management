package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "worker_site_history",
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
    ]
)
data class WorkerSiteHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long,
    val siteId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis()
)

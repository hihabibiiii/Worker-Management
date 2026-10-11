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
    val deletedAt: Long? = null
)

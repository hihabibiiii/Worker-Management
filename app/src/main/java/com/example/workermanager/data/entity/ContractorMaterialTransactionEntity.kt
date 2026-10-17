package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ContractorMaterialTransactionType(val displayName: String) {
    ADDED("Material Added"),
    SENT("Sent to Site"),
    RETURNED("Returned from Site"),
    BROKEN("Marked as Broken"),
    LOST("Marked as Lost"),
    ADJUSTMENT("Stock Adjustment")
}

@Entity(
    tableName = "contractor_material_transactions",
    foreignKeys = [
        ForeignKey(
            entity = ContractorMaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ContractorMaterialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val siteId: Long? = null,
    val transactionType: ContractorMaterialTransactionType = ContractorMaterialTransactionType.ADDED,
    val transactionDate: LocalDate = LocalDate.now(),
    val quantity: Double,
    val unit: String,
    val reasonOrNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class MaterialTransactionType(val displayName: String) {
    RECEIVED("Material Received / Purchase"),
    USED("Material Used"),
    TRANSFER_IN("Transfer In"),
    TRANSFER_OUT("Transfer Out"),
    ADJUSTMENT("Stock Adjustment")
}

@Entity(
    tableName = "material_transactions",
    foreignKeys = [
        ForeignKey(
            entity = MaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
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
data class MaterialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val siteId: Long,
    val transactionType: MaterialTransactionType = MaterialTransactionType.RECEIVED,
    val transactionDate: LocalDate = LocalDate.now(),
    val quantity: Double,
    val unit: String,
    val ratePerUnit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val supplierName: String = "",
    val supplierPhone: String = "",
    val invoiceNumber: String = "",
    val workDescription: String = "",
    val targetSiteId: Long? = null,
    val linkedExpenseId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

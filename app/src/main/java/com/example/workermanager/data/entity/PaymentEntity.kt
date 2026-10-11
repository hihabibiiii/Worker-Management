package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class PaymentMethod {
    CASH, UPI, BANK
}

enum class PaymentType {
    SALARY, ADVANCE, PAYMENT, BONUS, OTHER
}

@Entity(
    tableName = "payments",
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
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long,
    val siteId: Long,
    val amount: Double,
    val date: LocalDate,
    val method: PaymentMethod = PaymentMethod.CASH,
    val paymentType: PaymentType = PaymentType.PAYMENT,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

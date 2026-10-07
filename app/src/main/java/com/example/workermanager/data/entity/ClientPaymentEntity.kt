package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ClientPaymentMethod {
    CASH, UPI, BANK_TRANSFER, CHEQUE, OTHER
}

@Entity(
    tableName = "client_payments",
    foreignKeys = [
        ForeignKey(
            entity = ContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["contractId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClientPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contractId: Long,
    val amount: Double,
    val paymentDate: LocalDate = LocalDate.now(),
    val method: ClientPaymentMethod = ClientPaymentMethod.CASH,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

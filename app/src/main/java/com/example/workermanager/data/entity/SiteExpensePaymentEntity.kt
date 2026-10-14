package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "site_expense_payments",
    foreignKeys = [
        ForeignKey(
            entity = SiteExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SiteExpensePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val paymentDate: LocalDate = LocalDate.now(),
    val amount: Double,
    val paidBy: String = "",
    val paymentMethod: ExpensePaymentMethod = ExpensePaymentMethod.CASH,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

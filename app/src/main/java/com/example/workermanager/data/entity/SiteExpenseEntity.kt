package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ExpenseCategory(val displayName: String) {
    NASHTA("Nashta"),
    CHAI_PANI("Chai-Pani"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    DRINKING_WATER("Drinking Water"),
    TRANSPORT("Transport"),
    CONSTRUCTION_MATERIAL("Construction Material"),
    LABOUR_EXPENSE("Labour-related Expense"),
    TOOLS_EQUIPMENT("Tools & Equipment"),
    OTHER("Other")
}

enum class ExpensePaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    ONLINE("Bank"),
    OTHER("Other")
}

enum class ExpensePaymentStatus {
    PAID, PARTIALLY_PAID, UNPAID
}

@Entity(
    tableName = "site_expenses",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SiteExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siteId: Long,
    val expenseDate: LocalDate = LocalDate.now(),
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val description: String,
    val quantity: Double? = null,
    val rate: Double? = null,
    val amount: Double,
    val paidBy: String = "",
    val paymentMethod: ExpensePaymentMethod = ExpensePaymentMethod.CASH,
    val notes: String = "",
    val receiptPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

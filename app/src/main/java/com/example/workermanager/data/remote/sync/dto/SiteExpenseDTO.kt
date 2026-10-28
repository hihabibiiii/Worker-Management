package com.example.workermanager.data.remote.sync.dto

data class SiteExpenseDTO(
    var syncId: String = "",
    var siteSyncId: String = "",
    var expenseDate: String = "",
    var category: String = "OTHER",
    var description: String = "",
    var quantity: Double? = null,
    var rate: Double? = null,
    var amount: Double = 0.0,
    var paidBy: String = "",
    var paymentMethod: String = "CASH",
    var notes: String = "",
    var receiptPath: String? = null,
    var createdAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

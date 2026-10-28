package com.example.workermanager.data.remote.sync.dto

data class SiteExpensePaymentDTO(
    var syncId: String = "",
    var expenseSyncId: String = "",
    var paymentDate: String = "",
    var amount: Double = 0.0,
    var paidBy: String = "",
    var paymentMethod: String = "CASH",
    var notes: String = "",
    var createdAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

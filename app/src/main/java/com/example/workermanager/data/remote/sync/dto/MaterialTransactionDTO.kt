package com.example.workermanager.data.remote.sync.dto

data class MaterialTransactionDTO(
    var syncId: String = "",
    var materialSyncId: String = "",
    var siteSyncId: String = "",
    var transactionType: String = "RECEIVED",
    var transactionDate: String = "",
    var quantity: Double = 0.0,
    var unit: String = "",
    var ratePerUnit: Double = 0.0,
    var totalAmount: Double = 0.0,
    var supplierName: String = "",
    var supplierPhone: String = "",
    var invoiceNumber: String = "",
    var workDescription: String = "",
    var targetSiteSyncId: String? = null,
    var linkedExpenseSyncId: String? = null,
    var notes: String = "",
    var createdAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

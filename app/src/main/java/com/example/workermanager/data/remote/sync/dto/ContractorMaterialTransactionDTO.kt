package com.example.workermanager.data.remote.sync.dto

data class ContractorMaterialTransactionDTO(
    var syncId: String = "",
    var materialSyncId: String = "",
    var siteSyncId: String? = null,
    var transactionType: String = "ADDED",
    var transactionDate: String = "",
    var quantity: Double = 0.0,
    var unit: String = "",
    var reasonOrNotes: String = "",
    var createdAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

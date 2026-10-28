package com.example.workermanager.data.remote.sync.dto

data class ClientPaymentDTO(
    var syncId: String = "",
    var contractSyncId: String = "",
    var amount: Double = 0.0,
    var paymentDate: String = "",
    var method: String = "CASH",
    var note: String = "",
    var createdAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var isEdited: Boolean = false,
    var updatedAt: Long = 0L
)

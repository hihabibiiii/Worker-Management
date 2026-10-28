package com.example.workermanager.data.remote.sync.dto

data class PaymentDTO(
    var syncId: String = "",
    var workerSyncId: String = "",
    var siteSyncId: String = "",
    var amount: Double = 0.0,
    var date: String = "",
    var method: String = "CASH",
    var paymentType: String = "PAYMENT",
    var note: String = "",
    var createdAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

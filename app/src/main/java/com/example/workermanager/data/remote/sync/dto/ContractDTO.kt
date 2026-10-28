package com.example.workermanager.data.remote.sync.dto

data class ContractDTO(
    var syncId: String = "",
    var clientName: String = "",
    var mobile: String = "",
    var siteSyncId: String? = null,
    var totalAmount: Double = 0.0,
    var startDate: String = "",
    var notes: String = "",
    var status: String = "ACTIVE",
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

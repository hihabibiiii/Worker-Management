package com.example.workermanager.data.remote.sync.dto

data class ContractorMaterialDTO(
    var syncId: String = "",
    var name: String = "",
    var category: String = "OTHER",
    var unit: String = "PCS",
    var minStockLevel: Double = 0.0,
    var notes: String = "",
    var isActive: Boolean = true,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null
)

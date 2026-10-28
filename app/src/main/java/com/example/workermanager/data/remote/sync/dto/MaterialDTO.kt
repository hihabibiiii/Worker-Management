package com.example.workermanager.data.remote.sync.dto

data class MaterialDTO(
    var syncId: String = "",
    var name: String = "",
    var category: String = "OTHER",
    var unit: String = "BAGS",
    var minStockLevel: Double = 0.0,
    var createdAt: Long = 0L,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

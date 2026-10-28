package com.example.workermanager.data.remote.sync.dto

data class WorkerDTO(
    var syncId: String = "",
    var name: String = "",
    var phone: String = "",
    var workerType: String = "LABOUR",
    var dailyRate: Double = 0.0,
    var overtimeRate: Double = 0.0,
    var joiningDate: String = "",
    var siteSyncId: String? = null,
    var isActive: Boolean = true,
    var notes: String = "",
    var photoPath: String? = null,
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

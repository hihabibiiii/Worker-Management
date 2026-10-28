package com.example.workermanager.data.remote.sync.dto

data class AttendanceDTO(
    var syncId: String = "",
    var workerSyncId: String = "",
    var siteSyncId: String = "",
    var date: String = "",
    var status: String = "PRESENT",
    var overtimeHours: Double = 0.0,
    var overtimeAmount: Double = 0.0,
    var amount: Double = 0.0,
    var note: String = "",
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)

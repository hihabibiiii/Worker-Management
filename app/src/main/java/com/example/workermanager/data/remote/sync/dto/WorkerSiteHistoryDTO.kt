package com.example.workermanager.data.remote.sync.dto

data class WorkerSiteHistoryDTO(
    var syncId: String = "",
    var workerSyncId: String = "",
    var siteSyncId: String = "",
    var startDate: String = "",
    var endDate: String? = null,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)

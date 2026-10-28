package com.example.workermanager.data.remote.sync.dto

data class SiteAttendanceDTO(
    var syncId: String = "",
    var siteSyncId: String = "",
    var date: String = "",
    var isPresent: Boolean = false,
    var updatedAt: Long = 0L
)

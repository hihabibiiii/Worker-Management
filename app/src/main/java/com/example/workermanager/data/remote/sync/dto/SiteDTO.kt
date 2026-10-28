package com.example.workermanager.data.remote.sync.dto

data class SiteDTO(
    var syncId: String = "",
    var name: String = "",
    var address: String = "",
    var manager: String = "",
    var startDate: String = "",
    var status: String = "ACTIVE",
    var isDeleted: Boolean = false,
    var deletedAt: Long? = null,
    var updatedAt: Long = 0L
)

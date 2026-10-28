package com.example.workermanager.data.remote.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SyncStatusState {
    object Synced : SyncStatusState()
    object Syncing : SyncStatusState()
    object PendingChanges : SyncStatusState()
    object Offline : SyncStatusState()
    data class Error(val message: String) : SyncStatusState()
}

object SyncStatusManager {
    private val _syncStatus = MutableStateFlow<SyncStatusState>(SyncStatusState.Synced)
    val syncStatus: StateFlow<SyncStatusState> = _syncStatus.asStateFlow()

    fun updateStatus(status: SyncStatusState) {
        _syncStatus.value = status
    }
}

package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.billing.QuotaExceededException
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.repository.SiteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SiteViewModel(private val repository: SiteRepository) : ViewModel() {

    val allSites: StateFlow<List<SiteEntity>> = repository.allSites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalActiveSiteCount: StateFlow<Int> = repository.allSites
        .map { list -> list.count { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _quotaExceededEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val quotaExceededEvent: SharedFlow<Unit> = _quotaExceededEvent.asSharedFlow()

    fun ensureSelfWorkIfNoSitesExist(onComplete: ((Long?) -> Unit)? = null) {
        viewModelScope.launch {
            val createdId = repository.ensureSelfWorkIfNoSitesExist()
            onComplete?.invoke(createdId)
        }
    }

    fun addSite(
        name: String, 
        address: String = "", 
        manager: String = "", 
        startDate: LocalDate = LocalDate.now(),
        onCreated: ((Long) -> Unit)? = null,
        onQuotaExceeded: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val id = repository.insertSite(SiteEntity(name = name, address = address, manager = manager, startDate = startDate))
                onCreated?.invoke(id)
            } catch (e: QuotaExceededException) {
                _quotaExceededEvent.tryEmit(Unit)
                onQuotaExceeded?.invoke()
            }
        }
    }

    fun updateSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.updateSite(site)
        }
    }

    fun deleteSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.deleteSite(site)
        }
    }

    fun getSiteById(id: Long) = repository.getSiteById(id)
}

class SiteViewModelFactory(private val repository: SiteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SiteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

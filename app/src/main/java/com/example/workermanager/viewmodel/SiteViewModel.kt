package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.data.repository.SiteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SiteViewModel(private val repository: SiteRepository) : ViewModel() {

    val allSites: StateFlow<List<SiteEntity>> = repository.allSites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addSite(name: String, address: String, manager: String, startDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            repository.insertSite(SiteEntity(name = name, address = address, manager = manager, startDate = startDate))
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

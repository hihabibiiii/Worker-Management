package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.*
import com.example.workermanager.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TrashCategory(val label: String) {
    ALL("All"),
    WORKER("Workers"),
    CONTRACT("Clients / Contracts"),
    SITE("Sites"),
    PAYMENT("Worker Payments"),
    CLIENT_PAYMENT("Client Payments")
}

data class TrashUiItem(
    val id: Long,
    val category: TrashCategory,
    val title: String,
    val subtitle: String,
    val deletedAt: Long?,
    val originalEntity: Any
)

class RecycleBinViewModel(
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository,
    private val contractRepository: ContractRepository,
    private val paymentRepository: PaymentRepository,
    private val clientPaymentRepository: ClientPaymentRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(TrashCategory.ALL)
    val selectedCategory: StateFlow<TrashCategory> = _selectedCategory

    val trashedItems: StateFlow<List<TrashUiItem>> = combine(
        workerRepository.getDeletedWorkers(),
        siteRepository.getDeletedSites(),
        contractRepository.getDeletedContracts(),
        paymentRepository.getDeletedPayments(),
        clientPaymentRepository.getDeletedClientPayments(),
        _selectedCategory
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val deletedWorkers = flows[0] as List<WorkerEntity>
        @Suppress("UNCHECKED_CAST")
        val deletedSites = flows[1] as List<SiteEntity>
        @Suppress("UNCHECKED_CAST")
        val deletedContracts = flows[2] as List<ContractEntity>
        @Suppress("UNCHECKED_CAST")
        val deletedPayments = flows[3] as List<PaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val deletedClientPayments = flows[4] as List<ClientPaymentEntity>
        val selectedCat = flows[5] as TrashCategory

        val items = mutableListOf<TrashUiItem>()

        deletedWorkers.forEach { worker ->
            items.add(
                TrashUiItem(
                    id = worker.id,
                    category = TrashCategory.WORKER,
                    title = worker.name,
                    subtitle = "Type: ${worker.workerType.name} ${if (worker.phone.isNotEmpty()) "| Phone: ${worker.phone}" else ""}",
                    deletedAt = worker.deletedAt,
                    originalEntity = worker
                )
            )
        }

        deletedSites.forEach { site ->
            items.add(
                TrashUiItem(
                    id = site.id,
                    category = TrashCategory.SITE,
                    title = site.name,
                    subtitle = "Address: ${site.address.ifBlank { "N/A" }}",
                    deletedAt = site.deletedAt,
                    originalEntity = site
                )
            )
        }

        deletedContracts.forEach { contract ->
            items.add(
                TrashUiItem(
                    id = contract.id,
                    category = TrashCategory.CONTRACT,
                    title = contract.clientName,
                    subtitle = "Mobile: ${contract.mobile.ifBlank { "N/A" }} | Contract Amount: ₹${contract.totalAmount}",
                    deletedAt = contract.deletedAt,
                    originalEntity = contract
                )
            )
        }

        deletedPayments.forEach { payment ->
            items.add(
                TrashUiItem(
                    id = payment.id,
                    category = TrashCategory.PAYMENT,
                    title = "Worker Payment: ₹${payment.amount}",
                    subtitle = "Type: ${payment.paymentType.name} | Method: ${payment.method.name}",
                    deletedAt = payment.deletedAt,
                    originalEntity = payment
                )
            )
        }

        deletedClientPayments.forEach { cp ->
            items.add(
                TrashUiItem(
                    id = cp.id,
                    category = TrashCategory.CLIENT_PAYMENT,
                    title = "Client Payment Received: ₹${cp.amount}",
                    subtitle = "Method: ${cp.method.name} ${if (cp.note.isNotEmpty()) "| ${cp.note}" else ""}",
                    deletedAt = cp.deletedAt,
                    originalEntity = cp
                )
            )
        }

        items.sortByDescending { it.deletedAt ?: 0L }

        if (selectedCat == TrashCategory.ALL) {
            items
        } else {
            items.filter { it.category == selectedCat }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCategory(category: TrashCategory) {
        _selectedCategory.value = category
    }

    fun restoreItem(item: TrashUiItem) {
        viewModelScope.launch {
            when (item.category) {
                TrashCategory.WORKER -> workerRepository.restoreWorker(item.id)
                TrashCategory.SITE -> siteRepository.restoreSite(item.id)
                TrashCategory.CONTRACT -> contractRepository.restoreContract(item.id)
                TrashCategory.PAYMENT -> paymentRepository.restorePayment(item.id)
                TrashCategory.CLIENT_PAYMENT -> clientPaymentRepository.restoreClientPayment(item.id)
                else -> {}
            }
        }
    }

    fun permanentlyDeleteItem(item: TrashUiItem) {
        viewModelScope.launch {
            when (item.category) {
                TrashCategory.WORKER -> workerRepository.permanentlyDeleteWorker(item.id)
                TrashCategory.SITE -> siteRepository.permanentlyDeleteSite(item.id)
                TrashCategory.CONTRACT -> contractRepository.permanentlyDeleteContract(item.id)
                TrashCategory.PAYMENT -> paymentRepository.permanentlyDeletePayment(item.id)
                TrashCategory.CLIENT_PAYMENT -> clientPaymentRepository.permanentlyDeleteClientPayment(item.id)
                else -> {}
            }
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            workerRepository.emptyWorkerTrash()
            siteRepository.emptySiteTrash()
            contractRepository.emptyContractTrash()
            paymentRepository.emptyPaymentTrash()
            clientPaymentRepository.emptyClientPaymentTrash()
        }
    }
}

class RecycleBinViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository,
    private val contractRepository: ContractRepository,
    private val paymentRepository: PaymentRepository,
    private val clientPaymentRepository: ClientPaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecycleBinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecycleBinViewModel(workerRepository, siteRepository, contractRepository, paymentRepository, clientPaymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

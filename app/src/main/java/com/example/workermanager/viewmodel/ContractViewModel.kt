package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.ClientPaymentEntity
import com.example.workermanager.data.entity.ClientPaymentMethod
import com.example.workermanager.data.entity.ContractEntity
import com.example.workermanager.data.entity.ContractStatus
import com.example.workermanager.data.repository.ClientPaymentRepository
import com.example.workermanager.data.repository.ContractRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ContractUiModel(
    val contract: ContractEntity,
    val totalReceived: Double,
    val remainingAmount: Double,
    val siteName: String
)

sealed class ContractUiState {
    data object Loading : ContractUiState()
    data object Empty : ContractUiState()
    data class Success(val contracts: List<ContractUiModel>) : ContractUiState()
}

class ContractViewModel(
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository,
    private val siteRepository: com.example.workermanager.data.repository.SiteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _statusFilter = MutableStateFlow<ContractStatus?>(null)
    val statusFilter: StateFlow<ContractStatus?> = _statusFilter

    val allSites: Flow<List<com.example.workermanager.data.entity.SiteEntity>> = siteRepository.allSites

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ContractUiState> = combine(
        contractRepository.allContracts,
        siteRepository.allSites,
        _searchQuery,
        _statusFilter
    ) { contracts, sites, query, status ->
        if (contracts.isEmpty()) {
            ContractUiState.Empty
        } else {
            val filtered = contracts.filter { contract ->
                (query.isEmpty() || contract.clientName.contains(query, ignoreCase = true)) &&
                (status == null || contract.status == status)
            }
            
            val mapped = filtered.map { contract ->
                val received = clientPaymentRepository.getTotalReceivedByContract(contract.id).first() ?: 0.0
                val siteName = sites.find { it.id == contract.siteId }?.name ?: "No Site"
                ContractUiModel(
                    contract = contract,
                    totalReceived = received,
                    remainingAmount = contract.totalAmount - received,
                    siteName = siteName
                )
            }
            ContractUiState.Success(mapped)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContractUiState.Loading
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateStatusFilter(status: ContractStatus?) {
        _statusFilter.value = status
    }

    fun addContract(name: String, mobile: String, siteId: Long?, amount: Double, date: LocalDate, notes: String) {
        viewModelScope.launch {
            val contract = ContractEntity(
                clientName = name,
                mobile = mobile,
                siteId = siteId,
                totalAmount = amount,
                startDate = date,
                notes = notes
            )
            contractRepository.insertContract(contract)
        }
    }

    fun updateContract(contract: ContractEntity) {
        viewModelScope.launch {
            contractRepository.updateContract(contract)
        }
    }

    fun deleteContract(contract: ContractEntity) {
        viewModelScope.launch {
            contractRepository.deleteContract(contract)
        }
    }

    fun getContractDetails(contractId: Long): Flow<ContractUiModel?> = combine(
        contractRepository.getContractById(contractId),
        clientPaymentRepository.getTotalReceivedByContract(contractId),
        siteRepository.allSites
    ) { contract, received, sites ->
        contract?.let {
            val siteName = sites.find { it.id == contract.siteId }?.name ?: "No Site"
            ContractUiModel(
                contract = it,
                totalReceived = received ?: 0.0,
                remainingAmount = it.totalAmount - (received ?: 0.0),
                siteName = siteName
            )
        }
    }

    fun getPaymentsForContract(contractId: Long): Flow<List<ClientPaymentEntity>> =
        clientPaymentRepository.getPaymentsByContract(contractId)

    fun addClientPayment(
        contractId: Long,
        amount: Double,
        date: LocalDate,
        method: ClientPaymentMethod,
        note: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val payment = ClientPaymentEntity(
                    contractId = contractId,
                    amount = amount,
                    paymentDate = date,
                    method = method,
                    note = note
                )
                clientPaymentRepository.insertPayment(payment)

                // Check if contract is completed
                val totalReceived = clientPaymentRepository.getTotalReceivedByContract(contractId).first() ?: 0.0
                val contract = contractRepository.getContractById(contractId).first()
                if (contract != null && totalReceived >= contract.totalAmount) {
                    contractRepository.updateContract(contract.copy(status = ContractStatus.COMPLETED))
                }

                onSuccess?.invoke()
            } catch (e: Throwable) {
                android.util.Log.e("WorkCraftDB", "Error adding client payment", e)
                onError?.invoke(e)
            }
        }
    }

    fun deleteClientPayment(payment: ClientPaymentEntity) {
        viewModelScope.launch {
            clientPaymentRepository.deletePayment(payment)
            
            // Re-check status
            val totalReceived = clientPaymentRepository.getTotalReceivedByContract(payment.contractId).first() ?: 0.0
            val contract = contractRepository.getContractById(payment.contractId).first()
            if (contract != null && totalReceived - payment.amount < contract.totalAmount) {
                contractRepository.updateContract(contract.copy(status = ContractStatus.ACTIVE))
            }
        }
    }

    fun updateClientPayment(payment: ClientPaymentEntity, newAmount: Double, newMethod: ClientPaymentMethod, newNote: String) {
        viewModelScope.launch {
            try {
                val updatedPayment = payment.copy(
                    amount = newAmount,
                    method = newMethod,
                    note = newNote,
                    isEdited = true
                )
                clientPaymentRepository.updatePayment(updatedPayment)

                // Re-check status
                val totalReceived = clientPaymentRepository.getTotalReceivedByContract(payment.contractId).first() ?: 0.0
                val contract = contractRepository.getContractById(payment.contractId).first()
                if (contract != null) {
                    if (totalReceived >= contract.totalAmount) {
                        contractRepository.updateContract(contract.copy(status = ContractStatus.COMPLETED))
                    } else {
                        contractRepository.updateContract(contract.copy(status = ContractStatus.ACTIVE))
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("WorkCraftDB", "Error updating client payment", e)
            }
        }
    }
}

class ContractViewModelFactory(
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository,
    private val siteRepository: com.example.workermanager.data.repository.SiteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContractViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContractViewModel(contractRepository, clientPaymentRepository, siteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

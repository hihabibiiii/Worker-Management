package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.PaymentEntity
import com.example.workermanager.data.entity.PaymentMethod
import com.example.workermanager.data.entity.PaymentType
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.WorkerRepository
import com.example.workermanager.data.repository.SiteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PaymentUiModel(
    val payment: PaymentEntity,
    val workerName: String,
    val siteName: String
)

sealed class PaymentsUiState {
    data object Loading : PaymentsUiState()
    data object Empty : PaymentsUiState()
    data class Success(val payments: List<PaymentUiModel>) : PaymentsUiState()
}

class PaymentViewModel(
    private val paymentRepository: PaymentRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    val workers = workerRepository.allWorkers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PaymentsUiState> = combine(
        _selectedSiteId.flatMapLatest { siteId ->
            if (siteId == null) paymentRepository.allPayments 
            else paymentRepository.getPaymentsBySite(siteId)
        },
        workerRepository.allWorkers,
        siteRepository.allSites
    ) { payments, workersList, sitesList ->
        if (payments.isEmpty()) {
            PaymentsUiState.Empty
        } else {
            val mapped = payments.map { payment ->
                val worker = workersList.find { it.id == payment.workerId }
                val site = sitesList.find { it.id == payment.siteId }
                PaymentUiModel(payment, worker?.name ?: "Unknown", site?.name ?: "Unknown Site")
            }
            PaymentsUiState.Success(mapped)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaymentsUiState.Loading
    )

    fun updateSelectedSite(siteId: Long?) {
        _selectedSiteId.value = siteId
    }

    fun addPayment(
        workerId: Long,
        siteId: Long,
        amount: Double,
        method: PaymentMethod,
        type: PaymentType,
        date: LocalDate,
        note: String
    ) {
        viewModelScope.launch {
            val payment = PaymentEntity(
                workerId = workerId,
                siteId = siteId,
                amount = amount,
                date = date,
                method = method,
                paymentType = type,
                note = note
            )
            paymentRepository.insertPayment(payment)
        }
    }

    fun updatePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            paymentRepository.updatePayment(payment)
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            paymentRepository.deletePayment(payment)
        }
    }

    fun getPaymentsByWorker(workerId: Long): Flow<List<PaymentEntity>> {
        return paymentRepository.getPaymentsByWorker(workerId)
    }

    fun getPaymentsBySite(siteId: Long): Flow<List<PaymentEntity>> {
        return paymentRepository.getPaymentsBySite(siteId)
    }
}

class PaymentViewModelFactory(
    private val paymentRepository: PaymentRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentViewModel(paymentRepository, workerRepository, siteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

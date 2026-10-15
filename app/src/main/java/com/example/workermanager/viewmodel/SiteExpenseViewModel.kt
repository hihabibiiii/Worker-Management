package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.*
import com.example.workermanager.data.repository.SiteExpenseRepository
import com.example.workermanager.data.repository.SiteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class SiteExpenseUiModel(
    val expense: SiteExpenseEntity,
    val siteName: String,
    val totalPaid: Double,
    val pendingAmount: Double,
    val status: ExpensePaymentStatus
)

data class SiteExpenseDashboardStats(
    val todayExpense: Double = 0.0,
    val monthExpense: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalPending: Double = 0.0
)

enum class DateFilterRange(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    WEEKLY("This Week"),
    MONTHLY("This Month"),
    YEARLY("This Year")
}

class SiteExpenseViewModel(
    private val repository: SiteExpenseRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    val selectedCategory: StateFlow<ExpenseCategory?> = _selectedCategory

    private val _selectedStatus = MutableStateFlow<ExpensePaymentStatus?>(null)
    val selectedStatus: StateFlow<ExpensePaymentStatus?> = _selectedStatus

    private val _dateFilterRange = MutableStateFlow(DateFilterRange.ALL)
    val dateFilterRange: StateFlow<DateFilterRange> = _dateFilterRange

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allSites: Flow<List<SiteEntity>> = siteRepository.allSites

    val expenseUiModels: StateFlow<List<SiteExpenseUiModel>> = combine(
        repository.allExpenses,
        repository.allPayments,
        siteRepository.allSites,
        _selectedSiteId,
        _selectedCategory,
        _selectedStatus,
        _dateFilterRange,
        _searchQuery
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val expenses = flows[0] as List<SiteExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val payments = flows[1] as List<SiteExpensePaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val sites = flows[2] as List<SiteEntity>
        val siteIdFilter = flows[3] as Long?
        val categoryFilter = flows[4] as ExpenseCategory?
        val statusFilter = flows[5] as ExpensePaymentStatus?
        val dateRange = flows[6] as DateFilterRange
        val query = (flows[7] as String).trim().lowercase()

        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val currentMonth = YearMonth.now()

        val filteredExpenses = expenses.filter { exp ->
            (siteIdFilter == null || exp.siteId == siteIdFilter) &&
            (categoryFilter == null || exp.category == categoryFilter) &&
            (query.isBlank() || exp.description.lowercase().contains(query) || exp.paidBy.lowercase().contains(query) || exp.notes.lowercase().contains(query)) &&
            when (dateRange) {
                DateFilterRange.ALL -> true
                DateFilterRange.TODAY -> exp.expenseDate == today
                DateFilterRange.YESTERDAY -> exp.expenseDate == today.minusDays(1)
                DateFilterRange.WEEKLY -> !exp.expenseDate.isBefore(monday) && !exp.expenseDate.isAfter(monday.plusDays(6))
                DateFilterRange.MONTHLY -> exp.expenseDate.year == currentMonth.year && exp.expenseDate.monthValue == currentMonth.monthValue
                DateFilterRange.YEARLY -> exp.expenseDate.year == today.year
            }
        }

        val models = filteredExpenses.map { exp ->
            val siteName = sites.find { it.id == exp.siteId }?.name ?: "Unknown Site"
            val expPayments = payments.filter { it.expenseId == exp.id }
            val paidSum = expPayments.sumOf { it.amount }
            val pending = (exp.amount - paidSum).coerceAtLeast(0.0)

            val status = when {
                pending <= 0.001 && exp.amount > 0 -> ExpensePaymentStatus.PAID
                paidSum > 0.001 -> ExpensePaymentStatus.PARTIALLY_PAID
                else -> ExpensePaymentStatus.UNPAID
            }

            SiteExpenseUiModel(
                expense = exp,
                siteName = siteName,
                totalPaid = paidSum,
                pendingAmount = pending,
                status = status
            )
        }

        if (statusFilter != null) {
            models.filter { it.status == statusFilter }
        } else {
            models
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<SiteExpenseDashboardStats> = combine(
        repository.allExpenses,
        repository.allPayments,
        _selectedSiteId
    ) { expenses, payments, siteId ->
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()

        val filteredExpenses = if (siteId == null) expenses else expenses.filter { it.siteId == siteId }
        val activeExpIds = filteredExpenses.map { it.id }.toSet()
        val filteredPayments = payments.filter { activeExpIds.contains(it.expenseId) }

        val todayExp = filteredExpenses.filter { it.expenseDate == today }.sumOf { it.amount }
        val monthExp = filteredExpenses.filter { it.expenseDate.year == currentMonth.year && it.expenseDate.monthValue == currentMonth.monthValue }.sumOf { it.amount }
        val totalExp = filteredExpenses.sumOf { it.amount }
        val totalPaid = filteredPayments.sumOf { it.amount }
        val totalPending = (totalExp - totalPaid).coerceAtLeast(0.0)

        SiteExpenseDashboardStats(
            todayExpense = todayExp,
            monthExpense = monthExp,
            totalExpense = totalExp,
            totalPaid = totalPaid,
            totalPending = totalPending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SiteExpenseDashboardStats())

    val categoryBreakdown: StateFlow<Map<ExpenseCategory, Double>> = combine(
        repository.allExpenses,
        _selectedSiteId,
        _dateFilterRange
    ) { expenses, siteId, dateRange ->
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val currentMonth = YearMonth.now()

        val filtered = expenses.filter { exp ->
            (siteId == null || exp.siteId == siteId) &&
            when (dateRange) {
                DateFilterRange.ALL -> true
                DateFilterRange.TODAY -> exp.expenseDate == today
                DateFilterRange.YESTERDAY -> exp.expenseDate == today.minusDays(1)
                DateFilterRange.WEEKLY -> !exp.expenseDate.isBefore(monday) && !exp.expenseDate.isAfter(monday.plusDays(6))
                DateFilterRange.MONTHLY -> exp.expenseDate.year == currentMonth.year && exp.expenseDate.monthValue == currentMonth.monthValue
                DateFilterRange.YEARLY -> exp.expenseDate.year == today.year
            }
        }

        filtered.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setSelectedSite(siteId: Long?) { _selectedSiteId.value = siteId }
    fun setSelectedCategory(cat: ExpenseCategory?) { _selectedCategory.value = cat }
    fun setSelectedStatus(status: ExpensePaymentStatus?) { _selectedStatus.value = status }
    fun setDateFilterRange(range: DateFilterRange) { _dateFilterRange.value = range }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun addExpense(
        siteId: Long,
        expenseDate: LocalDate,
        category: ExpenseCategory,
        description: String,
        quantity: Double?,
        rate: Double?,
        amount: Double,
        paidBy: String,
        paymentMethod: ExpensePaymentMethod,
        notes: String,
        receiptPath: String?,
        initialAmountPaid: Double = 0.0
    ) {
        viewModelScope.launch {
            val expense = SiteExpenseEntity(
                siteId = siteId,
                expenseDate = expenseDate,
                category = category,
                description = description,
                quantity = quantity,
                rate = rate,
                amount = amount,
                paidBy = paidBy,
                paymentMethod = paymentMethod,
                notes = notes,
                receiptPath = receiptPath
            )
            val expId = repository.insertExpense(expense)
            if (initialAmountPaid > 0.001) {
                val initPayment = SiteExpensePaymentEntity(
                    expenseId = expId,
                    paymentDate = expenseDate,
                    amount = initialAmountPaid.coerceAtMost(amount),
                    paidBy = paidBy,
                    paymentMethod = paymentMethod,
                    notes = "Initial Payment"
                )
                repository.insertPayment(initPayment)
            }
        }
    }

    fun updateExpense(expense: SiteExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun addExpensePayment(
        expenseId: Long,
        paymentDate: LocalDate,
        amount: Double,
        paidBy: String,
        paymentMethod: ExpensePaymentMethod,
        notes: String
    ) {
        viewModelScope.launch {
            val payment = SiteExpensePaymentEntity(
                expenseId = expenseId,
                paymentDate = paymentDate,
                amount = amount,
                paidBy = paidBy,
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.insertPayment(payment)
        }
    }

    fun deleteExpensePayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
        }
    }

    fun getPaymentsForExpense(expenseId: Long): Flow<List<SiteExpensePaymentEntity>> =
        repository.getPaymentsForExpense(expenseId)
}

class SiteExpenseViewModelFactory(
    private val repository: SiteExpenseRepository,
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SiteExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SiteExpenseViewModel(repository, siteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

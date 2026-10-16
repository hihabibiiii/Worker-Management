package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.*
import com.example.workermanager.data.repository.MaterialRepository
import com.example.workermanager.data.repository.SiteExpenseRepository
import com.example.workermanager.data.repository.SiteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MaterialStockUiModel(
    val material: MaterialEntity,
    val siteId: Long?,
    val siteName: String,
    val availableQuantity: Double,
    val unit: String,
    val averagePurchaseRate: Double,
    val currentStockValue: Double,
    val isLowStock: Boolean
)

data class MaterialDashboardSummary(
    val totalMaterialsCount: Int = 0,
    val totalStockQuantity: Double = 0.0,
    val totalStockValue: Double = 0.0,
    val lowStockCount: Int = 0
)

data class MaterialTransactionUiModel(
    val transaction: MaterialTransactionEntity,
    val materialName: String,
    val siteName: String,
    val targetSiteName: String? = null
)

class MaterialViewModel(
    private val repository: MaterialRepository,
    private val siteRepository: SiteRepository,
    private val siteExpenseRepository: SiteExpenseRepository
) : ViewModel() {

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    private val _selectedCategory = MutableStateFlow<MaterialCategory?>(null)
    val selectedCategory: StateFlow<MaterialCategory?> = _selectedCategory

    private val _selectedMaterialId = MutableStateFlow<Long?>(null)
    val selectedMaterialId: StateFlow<Long?> = _selectedMaterialId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allSites: Flow<List<SiteEntity>> = siteRepository.allSites
    val allMaterials: Flow<List<MaterialEntity>> = repository.allMaterials

    init {
        viewModelScope.launch {
            repository.allMaterials.firstOrNull()?.let { list ->
                if (list.isEmpty()) {
                    populateDefaultMaterials()
                }
            }
        }
    }

    private suspend fun populateDefaultMaterials() {
        val defaults = listOf(
            // Civil
            MaterialEntity(name = "Cement", category = MaterialCategory.CIVIL, unit = MaterialUnit.BAGS, minStockLevel = 10.0),
            MaterialEntity(name = "Sand", category = MaterialCategory.CIVIL, unit = MaterialUnit.CU_FT, minStockLevel = 50.0),
            MaterialEntity(name = "Aggregate / Gitti", category = MaterialCategory.CIVIL, unit = MaterialUnit.CU_FT, minStockLevel = 50.0),
            MaterialEntity(name = "Bricks", category = MaterialCategory.CIVIL, unit = MaterialUnit.PCS, minStockLevel = 500.0),
            MaterialEntity(name = "Steel / Sariya", category = MaterialCategory.CIVIL, unit = MaterialUnit.KG, minStockLevel = 100.0),
            MaterialEntity(name = "Binding Wire", category = MaterialCategory.CIVIL, unit = MaterialUnit.KG, minStockLevel = 5.0),
            MaterialEntity(name = "Stone", category = MaterialCategory.CIVIL, unit = MaterialUnit.TON, minStockLevel = 5.0),
            MaterialEntity(name = "Concrete", category = MaterialCategory.CIVIL, unit = MaterialUnit.CU_FT, minStockLevel = 20.0),

            // Plumbing
            MaterialEntity(name = "PVC Pipe", category = MaterialCategory.PLUMBING, unit = MaterialUnit.PCS, minStockLevel = 5.0),
            MaterialEntity(name = "CPVC Pipe", category = MaterialCategory.PLUMBING, unit = MaterialUnit.PCS, minStockLevel = 5.0),
            MaterialEntity(name = "Water Pipe", category = MaterialCategory.PLUMBING, unit = MaterialUnit.MTR, minStockLevel = 10.0),
            MaterialEntity(name = "Elbow", category = MaterialCategory.PLUMBING, unit = MaterialUnit.PCS, minStockLevel = 10.0),
            MaterialEntity(name = "Taps", category = MaterialCategory.PLUMBING, unit = MaterialUnit.PCS, minStockLevel = 2.0),
            MaterialEntity(name = "Plumbing Fittings", category = MaterialCategory.PLUMBING, unit = MaterialUnit.PCS, minStockLevel = 10.0),

            // Electrical
            MaterialEntity(name = "Electrical Wire", category = MaterialCategory.ELECTRICAL, unit = MaterialUnit.MTR, minStockLevel = 50.0),
            MaterialEntity(name = "Switch", category = MaterialCategory.ELECTRICAL, unit = MaterialUnit.PCS, minStockLevel = 10.0),
            MaterialEntity(name = "Socket", category = MaterialCategory.ELECTRICAL, unit = MaterialUnit.PCS, minStockLevel = 10.0),
            MaterialEntity(name = "Conduit Pipe", category = MaterialCategory.ELECTRICAL, unit = MaterialUnit.PCS, minStockLevel = 10.0),
            MaterialEntity(name = "MCB", category = MaterialCategory.ELECTRICAL, unit = MaterialUnit.PCS, minStockLevel = 2.0),
            MaterialEntity(name = "Electrical Accessories", category = MaterialCategory.ELECTRICAL, unit = MaterialUnit.PCS, minStockLevel = 10.0),

            // Finishing
            MaterialEntity(name = "Floor Tiles", category = MaterialCategory.FINISHING, unit = MaterialUnit.SQ_FT, minStockLevel = 100.0),
            MaterialEntity(name = "Wall Tiles", category = MaterialCategory.FINISHING, unit = MaterialUnit.SQ_FT, minStockLevel = 100.0),
            MaterialEntity(name = "Paint", category = MaterialCategory.FINISHING, unit = MaterialUnit.LTR, minStockLevel = 10.0),
            MaterialEntity(name = "Primer", category = MaterialCategory.FINISHING, unit = MaterialUnit.LTR, minStockLevel = 10.0),
            MaterialEntity(name = "Putty", category = MaterialCategory.FINISHING, unit = MaterialUnit.KG, minStockLevel = 20.0),
            MaterialEntity(name = "Waterproofing Material", category = MaterialCategory.FINISHING, unit = MaterialUnit.KG, minStockLevel = 10.0),

            // Hardware
            MaterialEntity(name = "Nails", category = MaterialCategory.HARDWARE, unit = MaterialUnit.KG, minStockLevel = 2.0),
            MaterialEntity(name = "Screws", category = MaterialCategory.HARDWARE, unit = MaterialUnit.PCS, minStockLevel = 50.0),
            MaterialEntity(name = "Bolts", category = MaterialCategory.HARDWARE, unit = MaterialUnit.PCS, minStockLevel = 20.0),
            MaterialEntity(name = "Tools", category = MaterialCategory.HARDWARE, unit = MaterialUnit.PCS, minStockLevel = 1.0),
            MaterialEntity(name = "Equipment", category = MaterialCategory.HARDWARE, unit = MaterialUnit.PCS, minStockLevel = 1.0),

            // Other
            MaterialEntity(name = "Custom Material", category = MaterialCategory.OTHER, unit = MaterialUnit.OTHER, minStockLevel = 0.0)
        )
        defaults.forEach { repository.insertMaterial(it) }
    }

    val stockUiModels: StateFlow<List<MaterialStockUiModel>> = combine(
        repository.allMaterials,
        repository.allTransactions,
        siteRepository.allSites,
        _selectedSiteId,
        _selectedCategory,
        _searchQuery
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val materials = flows[0] as List<MaterialEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[1] as List<MaterialTransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val sites = flows[2] as List<SiteEntity>
        val siteIdFilter = flows[3] as Long?
        val categoryFilter = flows[4] as MaterialCategory?
        val query = (flows[5] as String).trim().lowercase()

        val filteredMaterials = materials.filter { mat ->
            (categoryFilter == null || mat.category == categoryFilter) &&
            (query.isBlank() || mat.name.lowercase().contains(query))
        }

        val models = mutableListOf<MaterialStockUiModel>()

        for (mat in filteredMaterials) {
            val matTxs = transactions.filter { it.materialId == mat.id }

            if (siteIdFilter != null) {
                val siteName = sites.find { it.id == siteIdFilter }?.name ?: "Unknown Site"
                val siteTxs = matTxs.filter { it.siteId == siteIdFilter }

                var avail = 0.0
                var totalPurchaseAmt = 0.0
                var totalPurchaseQty = 0.0

                for (tx in siteTxs) {
                    when (tx.transactionType) {
                        MaterialTransactionType.RECEIVED -> {
                            avail += tx.quantity
                            totalPurchaseAmt += tx.totalAmount
                            totalPurchaseQty += tx.quantity
                        }
                        MaterialTransactionType.TRANSFER_IN, MaterialTransactionType.ADJUSTMENT -> {
                            avail += tx.quantity
                        }
                        MaterialTransactionType.USED, MaterialTransactionType.TRANSFER_OUT -> {
                            avail -= tx.quantity
                        }
                    }
                }

                avail = avail.coerceAtLeast(0.0)
                val avgRate = if (totalPurchaseQty > 0) totalPurchaseAmt / totalPurchaseQty else 0.0
                val stockVal = avail * avgRate

                models.add(
                    MaterialStockUiModel(
                        material = mat,
                        siteId = siteIdFilter,
                        siteName = siteName,
                        availableQuantity = avail,
                        unit = mat.unit.displayName,
                        averagePurchaseRate = avgRate,
                        currentStockValue = stockVal,
                        isLowStock = mat.minStockLevel > 0 && avail <= mat.minStockLevel
                    )
                )
            } else {
                for (site in sites) {
                    val siteTxs = matTxs.filter { it.siteId == site.id }
                    if (siteTxs.isEmpty()) continue

                    var avail = 0.0
                    var totalPurchaseAmt = 0.0
                    var totalPurchaseQty = 0.0

                    for (tx in siteTxs) {
                        when (tx.transactionType) {
                            MaterialTransactionType.RECEIVED -> {
                                avail += tx.quantity
                                totalPurchaseAmt += tx.totalAmount
                                totalPurchaseQty += tx.quantity
                            }
                            MaterialTransactionType.TRANSFER_IN, MaterialTransactionType.ADJUSTMENT -> {
                                avail += tx.quantity
                            }
                            MaterialTransactionType.USED, MaterialTransactionType.TRANSFER_OUT -> {
                                avail -= tx.quantity
                            }
                        }
                    }

                    avail = avail.coerceAtLeast(0.0)
                    val avgRate = if (totalPurchaseQty > 0) totalPurchaseAmt / totalPurchaseQty else 0.0
                    val stockVal = avail * avgRate

                    models.add(
                        MaterialStockUiModel(
                            material = mat,
                            siteId = site.id,
                            siteName = site.name,
                            availableQuantity = avail,
                            unit = mat.unit.displayName,
                            averagePurchaseRate = avgRate,
                            currentStockValue = stockVal,
                            isLowStock = mat.minStockLevel > 0 && avail <= mat.minStockLevel
                        )
                    )
                }
            }
        }
        models
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<MaterialDashboardSummary> = combine(
        repository.allMaterials,
        stockUiModels
    ) { materials, stocks ->
        val totalMatCount = materials.size
        val totalQty = stocks.sumOf { it.availableQuantity }
        val totalVal = stocks.sumOf { it.currentStockValue }
        val lowStockCount = stocks.count { it.isLowStock }

        MaterialDashboardSummary(
            totalMaterialsCount = totalMatCount,
            totalStockQuantity = totalQty,
            totalStockValue = totalVal,
            lowStockCount = lowStockCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MaterialDashboardSummary())

    val transactionUiModels: StateFlow<List<MaterialTransactionUiModel>> = combine(
        repository.allTransactions,
        repository.allMaterials,
        siteRepository.allSites,
        _selectedSiteId,
        _selectedCategory,
        _selectedMaterialId,
        _searchQuery
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[0] as List<MaterialTransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val materials = flows[1] as List<MaterialEntity>
        @Suppress("UNCHECKED_CAST")
        val sites = flows[2] as List<SiteEntity>
        val siteIdFilter = flows[3] as Long?
        val categoryFilter = flows[4] as MaterialCategory?
        val matIdFilter = flows[5] as Long?
        val query = (flows[6] as String).trim().lowercase()

        val filtered = transactions.filter { tx ->
            val mat = materials.find { it.id == tx.materialId }
            val siteName = sites.find { it.id == tx.siteId }?.name ?: ""

            (siteIdFilter == null || tx.siteId == siteIdFilter || tx.targetSiteId == siteIdFilter) &&
            (categoryFilter == null || mat?.category == categoryFilter) &&
            (matIdFilter == null || tx.materialId == matIdFilter) &&
            (query.isBlank() || (mat?.name?.lowercase()?.contains(query) == true) ||
                    siteName.lowercase().contains(query) ||
                    tx.notes.lowercase().contains(query) ||
                    tx.supplierName.lowercase().contains(query))
        }

        filtered.map { tx ->
            val matName = materials.find { it.id == tx.materialId }?.name ?: "Unknown Material"
            val siteName = sites.find { it.id == tx.siteId }?.name ?: "Unknown Site"
            val targetSiteName = tx.targetSiteId?.let { targetId -> sites.find { it.id == targetId }?.name }

            MaterialTransactionUiModel(
                transaction = tx,
                materialName = matName,
                siteName = siteName,
                targetSiteName = targetSiteName
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedSite(siteId: Long?) { _selectedSiteId.value = siteId }
    fun setSelectedCategory(cat: MaterialCategory?) { _selectedCategory.value = cat }
    fun setSelectedMaterial(matId: Long?) { _selectedMaterialId.value = matId }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun addNewMaterial(name: String, category: MaterialCategory, unit: MaterialUnit, minStock: Double, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val existing = repository.allMaterials.firstOrNull()?.find { it.name.equals(name.trim(), ignoreCase = true) }
            if (existing != null) {
                onSuccess(existing.id)
            } else {
                val mat = MaterialEntity(
                    name = name.trim(),
                    category = category,
                    unit = unit,
                    minStockLevel = minStock
                )
                val id = repository.insertMaterial(mat)
                onSuccess(id)
            }
        }
    }

    fun recordMaterialReceived(
        siteId: Long,
        materialId: Long,
        purchaseDate: LocalDate,
        quantity: Double,
        unit: String,
        ratePerUnit: Double,
        totalAmount: Double,
        supplierName: String,
        supplierPhone: String,
        invoiceNumber: String,
        notes: String,
        createSiteExpense: Boolean = false
    ) {
        viewModelScope.launch {
            var linkedExpenseId: Long? = null
            if (createSiteExpense && totalAmount > 0.001) {
                val mat = repository.allMaterials.firstOrNull()?.find { it.id == materialId }
                val exp = SiteExpenseEntity(
                    siteId = siteId,
                    expenseDate = purchaseDate,
                    category = ExpenseCategory.CONSTRUCTION_MATERIAL,
                    description = "Material Purchase: ${mat?.name ?: "Material"} ($quantity $unit)",
                    quantity = quantity,
                    rate = ratePerUnit,
                    amount = totalAmount,
                    paidBy = supplierName,
                    notes = notes
                )
                linkedExpenseId = siteExpenseRepository.insertExpense(exp)
            }

            val tx = MaterialTransactionEntity(
                materialId = materialId,
                siteId = siteId,
                transactionType = MaterialTransactionType.RECEIVED,
                transactionDate = purchaseDate,
                quantity = quantity,
                unit = unit,
                ratePerUnit = ratePerUnit,
                totalAmount = totalAmount,
                supplierName = supplierName.trim(),
                supplierPhone = supplierPhone.trim(),
                invoiceNumber = invoiceNumber.trim(),
                linkedExpenseId = linkedExpenseId,
                notes = notes.trim()
            )
            repository.insertTransaction(tx)
        }
    }

    fun recordMaterialUsed(
        siteId: Long,
        materialId: Long,
        usageDate: LocalDate,
        quantity: Double,
        unit: String,
        workDescription: String,
        notes: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val stocks = stockUiModels.value
            val currentStock = stocks.find { it.material.id == materialId && it.siteId == siteId }?.availableQuantity ?: 0.0

            if (quantity > currentStock + 0.001) {
                onError("Insufficient stock! Available stock at this site is $currentStock $unit.")
                return@launch
            }

            val tx = MaterialTransactionEntity(
                materialId = materialId,
                siteId = siteId,
                transactionType = MaterialTransactionType.USED,
                transactionDate = usageDate,
                quantity = quantity,
                unit = unit,
                workDescription = workDescription.trim(),
                notes = notes.trim()
            )
            repository.insertTransaction(tx)
            onSuccess()
        }
    }

    fun recordMaterialTransfer(
        materialId: Long,
        sourceSiteId: Long,
        destinationSiteId: Long,
        transferDate: LocalDate,
        quantity: Double,
        unit: String,
        notes: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (sourceSiteId == destinationSiteId) {
                onError("Source and destination sites must be different.")
                return@launch
            }

            val stocks = stockUiModels.value
            val sourceStock = stocks.find { it.material.id == materialId && it.siteId == sourceSiteId }?.availableQuantity ?: 0.0

            if (quantity > sourceStock + 0.001) {
                onError("Insufficient stock at source site! Available stock is $sourceStock $unit.")
                return@launch
            }

            repository.performTransfer(
                materialId = materialId,
                sourceSiteId = sourceSiteId,
                destinationSiteId = destinationSiteId,
                quantity = quantity,
                unit = unit,
                transferDate = transferDate,
                notes = notes.trim()
            )
            onSuccess()
        }
    }

    fun deleteTransaction(txId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(txId)
        }
    }
}

class MaterialViewModelFactory(
    private val repository: MaterialRepository,
    private val siteRepository: SiteRepository,
    private val siteExpenseRepository: SiteExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MaterialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MaterialViewModel(repository, siteRepository, siteExpenseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.*
import com.example.workermanager.data.repository.ContractorMaterialRepository
import com.example.workermanager.data.repository.SiteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PurchaseDestination {
    INVENTORY,
    DIRECT_TO_SITE
}

data class ContractorMaterialStockModel(
    val material: ContractorMaterialEntity,
    val totalAddedQuantity: Double,
    val availableQuantity: Double,
    val quantityAtSites: Double,
    val brokenQuantity: Double,
    val lostQuantity: Double,
    val isLowStock: Boolean
)

data class SiteMaterialStockModel(
    val siteId: Long,
    val siteName: String,
    val materialId: Long,
    val materialName: String,
    val unit: String,
    val totalSent: Double,
    val totalReturned: Double,
    val brokenAtSite: Double,
    val lostAtSite: Double,
    val currentSiteQuantity: Double
)

data class ContractorMaterialSummary(
    val totalMaterialTypes: Int = 0,
    val totalQuantity: Double = 0.0,
    val availableQuantity: Double = 0.0,
    val quantityAtSites: Double = 0.0,
    val totalBrokenLostQuantity: Double = 0.0
)

data class ContractorMaterialTxUiModel(
    val transaction: ContractorMaterialTransactionEntity,
    val materialName: String,
    val siteName: String?
)

class ContractorMaterialViewModel(
    private val repository: ContractorMaterialRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    private val _selectedCategory = MutableStateFlow<ContractorMaterialCategory?>(null)
    val selectedCategory: StateFlow<ContractorMaterialCategory?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allSites: Flow<List<SiteEntity>> = siteRepository.allSites
    val allMaterials: Flow<List<ContractorMaterialEntity>> = repository.allMaterials

    val stockModels: StateFlow<List<ContractorMaterialStockModel>> = combine(
        repository.allMaterials,
        repository.allTransactions,
        _selectedCategory,
        _searchQuery
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val materials = flows[0] as List<ContractorMaterialEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[1] as List<ContractorMaterialTransactionEntity>
        val categoryFilter = flows[2] as ContractorMaterialCategory?
        val query = (flows[3] as String).trim().lowercase()

        val filtered = materials.filter { mat ->
            mat.isActive &&
            (categoryFilter == null || mat.category == categoryFilter) &&
            (query.isBlank() || mat.name.lowercase().contains(query))
        }

        val txsByMaterial = transactions.groupBy { it.materialId }

        filtered.map { mat ->
            val matTxs = txsByMaterial[mat.id] ?: emptyList()

            var totalAdded = 0.0
            var sent = 0.0
            var returned = 0.0
            var brokenCentral = 0.0
            var lostCentral = 0.0
            var brokenSite = 0.0
            var lostSite = 0.0

            for (tx in matTxs) {
                when (tx.transactionType) {
                    ContractorMaterialTransactionType.ADDED,
                    ContractorMaterialTransactionType.ADJUSTMENT -> totalAdded += tx.quantity
                    ContractorMaterialTransactionType.SENT -> sent += tx.quantity
                    ContractorMaterialTransactionType.RETURNED -> returned += tx.quantity
                    ContractorMaterialTransactionType.BROKEN -> {
                        if (tx.siteId == null) brokenCentral += tx.quantity else brokenSite += tx.quantity
                    }
                    ContractorMaterialTransactionType.LOST -> {
                        if (tx.siteId == null) lostCentral += tx.quantity else lostSite += tx.quantity
                    }
                }
            }

            val avail = (totalAdded - sent + returned - brokenCentral - lostCentral).coerceAtLeast(0.0)
            val atSites = (sent - returned - brokenSite - lostSite).coerceAtLeast(0.0)
            val totalBroken = brokenCentral + brokenSite
            val totalLost = lostCentral + lostSite

            ContractorMaterialStockModel(
                material = mat,
                totalAddedQuantity = totalAdded,
                availableQuantity = avail,
                quantityAtSites = atSites,
                brokenQuantity = totalBroken,
                lostQuantity = totalLost,
                isLowStock = mat.minStockLevel > 0 && avail <= mat.minStockLevel
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<ContractorMaterialSummary> = combine(
        stockModels
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val stocks = flows[0] as List<ContractorMaterialStockModel>

        ContractorMaterialSummary(
            totalMaterialTypes = stocks.size,
            totalQuantity = stocks.sumOf { it.totalAddedQuantity },
            availableQuantity = stocks.sumOf { it.availableQuantity },
            quantityAtSites = stocks.sumOf { it.quantityAtSites },
            totalBrokenLostQuantity = stocks.sumOf { it.brokenQuantity + it.lostQuantity }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContractorMaterialSummary())

    val siteStockModels: StateFlow<List<SiteMaterialStockModel>> = combine(
        repository.allMaterials,
        repository.allTransactions,
        siteRepository.allSites,
        _selectedSiteId
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val materials = flows[0] as List<ContractorMaterialEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[1] as List<ContractorMaterialTransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val sites = flows[2] as List<SiteEntity>
        val siteIdFilter = flows[3] as Long?

        val activeSites = if (siteIdFilter != null) sites.filter { it.id == siteIdFilter } else sites
        val txsBySiteAndMaterial = transactions.filter { it.siteId != null }.groupBy { Pair(it.siteId!!, it.materialId) }
        val list = mutableListOf<SiteMaterialStockModel>()

        for (site in activeSites) {
            for (mat in materials) {
                if (!mat.isActive) continue
                val matSiteTxs = txsBySiteAndMaterial[Pair(site.id, mat.id)] ?: continue

                var sent = 0.0
                var returned = 0.0
                var broken = 0.0
                var lost = 0.0

                for (tx in matSiteTxs) {
                    when (tx.transactionType) {
                        ContractorMaterialTransactionType.SENT -> sent += tx.quantity
                        ContractorMaterialTransactionType.RETURNED -> returned += tx.quantity
                        ContractorMaterialTransactionType.BROKEN -> broken += tx.quantity
                        ContractorMaterialTransactionType.LOST -> lost += tx.quantity
                        else -> {}
                    }
                }

                val currentSiteQty = (sent - returned - broken - lost).coerceAtLeast(0.0)

                if (sent > 0 || currentSiteQty > 0) {
                    list.add(
                        SiteMaterialStockModel(
                            siteId = site.id,
                            siteName = site.name,
                            materialId = mat.id,
                            materialName = mat.name,
                            unit = mat.unit.displayName,
                            totalSent = sent,
                            totalReturned = returned,
                            brokenAtSite = broken,
                            lostAtSite = lost,
                            currentSiteQuantity = currentSiteQty
                        )
                    )
                }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionUiModels: StateFlow<List<ContractorMaterialTxUiModel>> = combine(
        repository.allTransactions,
        repository.allMaterials,
        siteRepository.allSites,
        _selectedSiteId,
        _selectedCategory,
        _searchQuery
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[0] as List<ContractorMaterialTransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val materials = flows[1] as List<ContractorMaterialEntity>
        @Suppress("UNCHECKED_CAST")
        val sites = flows[2] as List<SiteEntity>
        val siteIdFilter = flows[3] as Long?
        val categoryFilter = flows[4] as ContractorMaterialCategory?
        val query = (flows[5] as String).trim().lowercase()

        val materialsMap = materials.associateBy { it.id }
        val sitesMap = sites.associateBy { it.id }

        val filtered = transactions.filter { tx ->
            val mat = materialsMap[tx.materialId]
            val siteName = tx.siteId?.let { sitesMap[it]?.name } ?: ""

            (siteIdFilter == null || tx.siteId == siteIdFilter) &&
            (categoryFilter == null || mat?.category == categoryFilter) &&
            (query.isBlank() || (mat?.name?.lowercase()?.contains(query) == true) ||
                    siteName.lowercase().contains(query) ||
                    tx.reasonOrNotes.lowercase().contains(query))
        }

        filtered.map { tx ->
            val matName = materialsMap[tx.materialId]?.name ?: "Unknown Material"
            val siteName = tx.siteId?.let { sitesMap[it]?.name }

            ContractorMaterialTxUiModel(
                transaction = tx,
                materialName = matName,
                siteName = siteName
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedSite(siteId: Long?) { _selectedSiteId.value = siteId }
    fun setSelectedCategory(cat: ContractorMaterialCategory?) { _selectedCategory.value = cat }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun purchaseMaterial(
        materialId: Long?,
        newMaterialName: String?,
        category: ContractorMaterialCategory = ContractorMaterialCategory.SHUTTERING_ROOF_CASTING,
        unit: ContractorMaterialUnit = ContractorMaterialUnit.PCS,
        quantity: Double,
        purchaseDate: LocalDate,
        supplierName: String,
        invoiceNumber: String,
        notes: String,
        destination: PurchaseDestination,
        destinationSiteId: Long?,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (supplierName.isBlank()) {
                onError("Purchased From (Shop / Supplier) name is required.")
                return@launch
            }
            if (quantity <= 0.001) {
                onError("Purchase quantity must be greater than zero.")
                return@launch
            }
            if (destination == PurchaseDestination.DIRECT_TO_SITE && destinationSiteId == null) {
                onError("Please select a destination site for direct delivery.")
                return@launch
            }

            val materialsList = repository.allMaterials.firstOrNull() ?: emptyList()
            val targetMaterialId = if (materialId != null) {
                materialId
            } else if (!newMaterialName.isNullOrBlank()) {
                val existing = materialsList.find { it.name.equals(newMaterialName.trim(), ignoreCase = true) && it.isActive }
                if (existing != null) {
                    existing.id
                } else {
                    val mat = ContractorMaterialEntity(
                        name = newMaterialName.trim(),
                        category = category,
                        unit = unit,
                        minStockLevel = 0.0,
                        notes = ""
                    )
                    repository.insertMaterial(mat)
                }
            } else {
                onError("Please select or enter a material.")
                return@launch
            }

            val currentMaterial = repository.allMaterials.firstOrNull()?.find { it.id == targetMaterialId }
            val unitDisplayName = currentMaterial?.unit?.displayName ?: unit.displayName

            val formattedNotes = buildString {
                append("Purchased from: ${supplierName.trim()}")
                if (invoiceNumber.isNotBlank()) append(" | Bill/Inv: ${invoiceNumber.trim()}")
                if (notes.isNotBlank()) append(" | Notes: ${notes.trim()}")
            }

            if (destination == PurchaseDestination.INVENTORY) {
                val tx = ContractorMaterialTransactionEntity(
                    materialId = targetMaterialId,
                    siteId = null,
                    transactionType = ContractorMaterialTransactionType.ADDED,
                    transactionDate = purchaseDate,
                    quantity = quantity,
                    unit = unitDisplayName,
                    reasonOrNotes = formattedNotes
                )
                repository.insertTransaction(tx)
            } else {
                val siteName = siteRepository.allSites.firstOrNull()?.find { it.id == destinationSiteId }?.name ?: "Site"
                val addTx = ContractorMaterialTransactionEntity(
                    materialId = targetMaterialId,
                    siteId = null,
                    transactionType = ContractorMaterialTransactionType.ADDED,
                    transactionDate = purchaseDate,
                    quantity = quantity,
                    unit = unitDisplayName,
                    reasonOrNotes = "Purchased from: ${supplierName.trim()} [Direct Delivery to $siteName]"
                )
                val sendTx = ContractorMaterialTransactionEntity(
                    materialId = targetMaterialId,
                    siteId = destinationSiteId,
                    transactionType = ContractorMaterialTransactionType.SENT,
                    transactionDate = purchaseDate,
                    quantity = quantity,
                    unit = unitDisplayName,
                    reasonOrNotes = formattedNotes
                )
                repository.insertTransaction(addTx)
                repository.insertTransaction(sendTx)
            }

            onSuccess()
        }
    }

    fun addMaterialToInventory(
        name: String,
        category: ContractorMaterialCategory,
        unit: ContractorMaterialUnit,
        initialQuantity: Double,
        minStockLevel: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.allMaterials.firstOrNull()?.find { it.name.equals(name.trim(), ignoreCase = true) && it.isActive }
            val materialId = if (existing != null) {
                existing.id
            } else {
                val mat = ContractorMaterialEntity(
                    name = name.trim(),
                    category = category,
                    unit = unit,
                    minStockLevel = minStockLevel,
                    notes = notes.trim()
                )
                repository.insertMaterial(mat)
            }

            if (initialQuantity > 0.001) {
                val mat = repository.allMaterials.firstOrNull()?.find { it.id == materialId }
                val tx = ContractorMaterialTransactionEntity(
                    materialId = materialId,
                    siteId = null,
                    transactionType = ContractorMaterialTransactionType.ADDED,
                    transactionDate = LocalDate.now(),
                    quantity = initialQuantity,
                    unit = mat?.unit?.displayName ?: unit.displayName,
                    reasonOrNotes = "Initial Stock Purchase / Added"
                )
                repository.insertTransaction(tx)
            }
            onSuccess()
        }
    }

    fun sendMaterialToSite(
        materialId: Long,
        siteId: Long,
        quantity: Double,
        transactionDate: LocalDate,
        notes: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val stocks = stockModels.value
            val currentStock = stocks.find { it.material.id == materialId }
            val available = currentStock?.availableQuantity ?: 0.0

            if (quantity > available + 0.001) {
                onError("Cannot send more than available stock! Available stock: $available ${currentStock?.material?.unit?.displayName ?: ""}")
                return@launch
            }

            val tx = ContractorMaterialTransactionEntity(
                materialId = materialId,
                siteId = siteId,
                transactionType = ContractorMaterialTransactionType.SENT,
                transactionDate = transactionDate,
                quantity = quantity,
                unit = currentStock?.material?.unit?.displayName ?: "Pcs",
                reasonOrNotes = notes.trim()
            )
            repository.insertTransaction(tx)
            onSuccess()
        }
    }

    fun returnMaterialFromSite(
        materialId: Long,
        siteId: Long,
        quantity: Double,
        returnDate: LocalDate,
        notes: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val siteStocks = siteStockModels.value
            val currentSiteQty = siteStocks.find { it.materialId == materialId && it.siteId == siteId }?.currentSiteQuantity ?: 0.0

            if (quantity > currentSiteQty + 0.001) {
                onError("Cannot return more than quantity assigned to this site! Current site quantity: $currentSiteQty")
                return@launch
            }

            val mat = repository.allMaterials.firstOrNull()?.find { it.id == materialId }
            val tx = ContractorMaterialTransactionEntity(
                materialId = materialId,
                siteId = siteId,
                transactionType = ContractorMaterialTransactionType.RETURNED,
                transactionDate = returnDate,
                quantity = quantity,
                unit = mat?.unit?.displayName ?: "Pcs",
                reasonOrNotes = notes.trim()
            )
            repository.insertTransaction(tx)
            onSuccess()
        }
    }

    fun markMaterialBroken(
        materialId: Long,
        siteId: Long?,
        quantity: Double,
        date: LocalDate,
        reason: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val mat = repository.allMaterials.firstOrNull()?.find { it.id == materialId }
            val availableAtLocation = if (siteId != null) {
                siteStockModels.value.find { it.materialId == materialId && it.siteId == siteId }?.currentSiteQuantity ?: 0.0
            } else {
                stockModels.value.find { it.material.id == materialId }?.availableQuantity ?: 0.0
            }

            if (quantity > availableAtLocation + 0.001) {
                onError("Cannot mark more than available quantity at this location! Available: $availableAtLocation")
                return@launch
            }

            val tx = ContractorMaterialTransactionEntity(
                materialId = materialId,
                siteId = siteId,
                transactionType = ContractorMaterialTransactionType.BROKEN,
                transactionDate = date,
                quantity = quantity,
                unit = mat?.unit?.displayName ?: "Pcs",
                reasonOrNotes = reason.trim()
            )
            repository.insertTransaction(tx)
            onSuccess()
        }
    }

    fun markMaterialLost(
        materialId: Long,
        siteId: Long?,
        quantity: Double,
        date: LocalDate,
        reason: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val mat = repository.allMaterials.firstOrNull()?.find { it.id == materialId }
            val availableAtLocation = if (siteId != null) {
                siteStockModels.value.find { it.materialId == materialId && it.siteId == siteId }?.currentSiteQuantity ?: 0.0
            } else {
                stockModels.value.find { it.material.id == materialId }?.availableQuantity ?: 0.0
            }

            if (quantity > availableAtLocation + 0.001) {
                onError("Cannot mark more than available quantity at this location! Available: $availableAtLocation")
                return@launch
            }

            val tx = ContractorMaterialTransactionEntity(
                materialId = materialId,
                siteId = siteId,
                transactionType = ContractorMaterialTransactionType.LOST,
                transactionDate = date,
                quantity = quantity,
                unit = mat?.unit?.displayName ?: "Pcs",
                reasonOrNotes = reason.trim()
            )
            repository.insertTransaction(tx)
            onSuccess()
        }
    }

    fun deactivateMaterial(materialId: Long) {
        viewModelScope.launch {
            repository.deactivateMaterial(materialId)
        }
    }

    fun updateMaterial(
        material: ContractorMaterialEntity,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateMaterial(
                material.copy(updatedAt = System.currentTimeMillis())
            )
            onSuccess()
        }
    }

    fun deleteMaterial(
        materialId: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.softDeleteMaterial(materialId)
            onSuccess()
        }
    }

    fun deleteTransaction(txId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(txId)
        }
    }
}

class ContractorMaterialViewModelFactory(
    private val repository: ContractorMaterialRepository,
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContractorMaterialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContractorMaterialViewModel(repository, siteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

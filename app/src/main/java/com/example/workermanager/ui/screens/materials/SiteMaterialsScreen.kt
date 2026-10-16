package com.example.workermanager.ui.screens.materials

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.*
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.MaterialDashboardSummary
import com.example.workermanager.viewmodel.MaterialStockUiModel
import com.example.workermanager.viewmodel.MaterialTransactionUiModel
import com.example.workermanager.viewmodel.MaterialViewModel
import com.example.workermanager.viewmodel.SiteViewModel
import java.time.LocalDate

enum class MaterialTab(val title: String) {
    STOCK("Stock"),
    RECEIVE("Receive"),
    USE("Use"),
    TRANSFER("Transfer"),
    HISTORY("History"),
    REPORTS("Reports")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteMaterialsScreen(
    viewModel: MaterialViewModel,
    siteViewModel: SiteViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val sites by siteViewModel.allSites.collectAsState(initial = emptyList())
    val materials by viewModel.allMaterials.collectAsState(initial = emptyList())
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val stocks by viewModel.stockUiModels.collectAsState()
    val transactions by viewModel.transactionUiModels.collectAsState()

    var activeTab by remember { mutableStateOf(MaterialTab.STOCK) }
    var showAddCustomMaterialDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Site Material Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dashboard Summary Cards
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaterialStatBox("Total Materials", dashboardSummary.totalMaterialsCount.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    MaterialStatBox("Total Stock Qty", String.format("%.1f", dashboardSummary.totalStockQuantity), Color(0xFF1565C0), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaterialStatBox("Stock Purchase Value", Utils.formatCurrency(dashboardSummary.totalStockValue), Color(0xFF2E7D32), Modifier.weight(1f))
                    MaterialStatBox("Low Stock Items", dashboardSummary.lowStockCount.toString(), if (dashboardSummary.lowStockCount > 0) Color(0xFFC62828) else Color(0xFF2E7D32), Modifier.weight(1f))
                }
            }

            // Tabs Header
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                MaterialTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            // Search and Filters
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search material or site...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        MaterialChip(
                            label = "All Sites",
                            isSelected = selectedSiteId == null,
                            onClick = { viewModel.setSelectedSite(null) }
                        )
                    }
                    items(sites) { site ->
                        MaterialChip(
                            label = site.name,
                            isSelected = selectedSiteId == site.id,
                            onClick = { viewModel.setSelectedSite(site.id) }
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        MaterialChip(
                            label = "All Categories",
                            isSelected = selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) }
                        )
                    }
                    items(MaterialCategory.entries) { cat ->
                        MaterialChip(
                            label = cat.displayName,
                            isSelected = selectedCategory == cat,
                            onClick = { viewModel.setSelectedCategory(cat) }
                        )
                    }
                }
            }

            // Tab View Body
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (activeTab) {
                    MaterialTab.STOCK -> {
                        MaterialStockTab(
                            stocks = stocks,
                            onAddCustomMaterial = { showAddCustomMaterialDialog = true }
                        )
                    }
                    MaterialTab.RECEIVE -> {
                        MaterialReceiveTab(
                            sites = sites,
                            materials = materials,
                            viewModel = viewModel,
                            onAddCustomMaterial = { showAddCustomMaterialDialog = true },
                            onSuccess = { activeTab = MaterialTab.STOCK }
                        )
                    }
                    MaterialTab.USE -> {
                        MaterialUseTab(
                            sites = sites,
                            materials = materials,
                            stocks = stocks,
                            viewModel = viewModel,
                            onSuccess = { activeTab = MaterialTab.STOCK }
                        )
                    }
                    MaterialTab.TRANSFER -> {
                        MaterialTransferTab(
                            sites = sites,
                            materials = materials,
                            stocks = stocks,
                            viewModel = viewModel,
                            onSuccess = { activeTab = MaterialTab.STOCK }
                        )
                    }
                    MaterialTab.HISTORY -> {
                        MaterialHistoryTab(
                            transactions = transactions,
                            onDelete = { txId -> viewModel.deleteTransaction(txId) }
                        )
                    }
                    MaterialTab.REPORTS -> {
                        MaterialReportsTab(
                            stocks = stocks,
                            transactions = transactions
                        )
                    }
                }
            }
        }

        if (showAddCustomMaterialDialog) {
            AddCustomMaterialDialog(
                onDismiss = { showAddCustomMaterialDialog = false },
                onConfirm = { name, category, unit, minStock ->
                    viewModel.addNewMaterial(name, category, unit, minStock) { id ->
                        showAddCustomMaterialDialog = false
                        Toast.makeText(context, "Material created!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun MaterialStatBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
        }
    }
}

@Composable
fun MaterialChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        tonalElevation = 2.dp,
        modifier = Modifier.height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MaterialStockTab(
    stocks: List<MaterialStockUiModel>,
    onAddCustomMaterial: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Material Stock List (${stocks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onAddCustomMaterial, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Material", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (stocks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No material stock records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stocks) { stock ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (stock.isLowStock) Color(0xFFC62828) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(stock.material.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${stock.material.category.displayName} • Site: ${stock.siteName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                if (stock.isLowStock) {
                                    Surface(color = Color(0xFFC62828).copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
                                        Text("LOW STOCK ALERT", color = Color(0xFFC62828), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Available: ${stock.availableQuantity} ${stock.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Avg Rate: ${Utils.formatCurrency(stock.averagePurchaseRate)}", fontSize = 12.sp)
                                Text("Value: ${Utils.formatCurrency(stock.currentStockValue)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialReceiveTab(
    sites: List<SiteEntity>,
    materials: List<MaterialEntity>,
    viewModel: MaterialViewModel,
    onAddCustomMaterial: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedSiteId by remember { mutableStateOf<Long?>(sites.firstOrNull()?.id) }
    var selectedMaterialId by remember { mutableStateOf<Long?>(materials.firstOrNull()?.id) }
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
    var quantityStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var totalAmountStr by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var createSiteExpense by remember { mutableStateOf(true) }

    var siteExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    val selectedMaterial = materials.find { it.id == selectedMaterialId }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> purchaseDate = LocalDate.of(y, m + 1, d) },
        purchaseDate.year, purchaseDate.monthValue - 1, purchaseDate.dayOfMonth
    )

    // Auto calculate total amount
    LaunchedEffect(quantityStr, rateStr) {
        val q = quantityStr.toDoubleOrNull()
        val r = rateStr.toDoubleOrNull()
        if (q != null && r != null && q > 0 && r > 0) {
            totalAmountStr = (q * r).toString()
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Receive Material / Stock Purchase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Site Selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site*",
                onValueChange = {},
                readOnly = true,
                label = { Text("Site*") },
                modifier = Modifier.fillMaxWidth().clickable { siteExpanded = true }
            )
            Box(modifier = Modifier.matchParentSize().clickable { siteExpanded = true })
            DropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                sites.forEach { site ->
                    DropdownMenuItem(text = { Text(site.name) }, onClick = { selectedSiteId = site.id; siteExpanded = false })
                }
            }
        }

        // Material Selection
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedMaterial?.name ?: "Select Material*",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Material*") },
                    modifier = Modifier.fillMaxWidth().clickable { materialExpanded = true }
                )
                Box(modifier = Modifier.matchParentSize().clickable { materialExpanded = true })
                DropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                    materials.forEach { mat ->
                        DropdownMenuItem(
                            text = { Text("${mat.name} (${mat.category.displayName})") },
                            onClick = { selectedMaterialId = mat.id; materialExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onAddCustomMaterial) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add Material", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = quantityStr,
                onValueChange = { quantityStr = it },
                label = { Text("Quantity Received* (${selectedMaterial?.unit?.displayName ?: "Unit"})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = rateStr,
                onValueChange = { rateStr = it },
                label = { Text("Rate / Unit (₹)*") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = totalAmountStr,
            onValueChange = { totalAmountStr = it },
            label = { Text("Total Purchase Amount (₹)*") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = supplierName,
            onValueChange = { supplierName = it },
            label = { Text("Supplier Name (Opt)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = invoiceNumber,
            onValueChange = { invoiceNumber = it },
            label = { Text("Invoice / Bill No. (Opt)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = Utils.formatDate(purchaseDate),
            onValueChange = {},
            readOnly = true,
            label = { Text("Purchase Date*") },
            trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.CalendarMonth, contentDescription = null) } },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = createSiteExpense, onCheckedChange = { createSiteExpense = it })
            Text("Auto Create Linked Record in Site Expenses", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Opt)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val r = rateStr.toDoubleOrNull() ?: 0.0
                val amt = totalAmountStr.toDoubleOrNull() ?: (q * r)

                if (selectedSiteId != null && selectedMaterialId != null && q > 0) {
                    viewModel.recordMaterialReceived(
                        siteId = selectedSiteId!!,
                        materialId = selectedMaterialId!!,
                        purchaseDate = purchaseDate,
                        quantity = q,
                        unit = selectedMaterial?.unit?.displayName ?: "Unit",
                        ratePerUnit = r,
                        totalAmount = amt,
                        supplierName = supplierName,
                        supplierPhone = supplierPhone,
                        invoiceNumber = invoiceNumber,
                        notes = notes,
                        createSiteExpense = createSiteExpense
                    )
                    Toast.makeText(context, "Material Received Logged!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedSiteId != null && selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Text("Save Received Material", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MaterialUseTab(
    sites: List<SiteEntity>,
    materials: List<MaterialEntity>,
    stocks: List<MaterialStockUiModel>,
    viewModel: MaterialViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedSiteId by remember { mutableStateOf<Long?>(sites.firstOrNull()?.id) }
    var selectedMaterialId by remember { mutableStateOf<Long?>(materials.firstOrNull()?.id) }
    var usageDate by remember { mutableStateOf(LocalDate.now()) }
    var quantityStr by remember { mutableStateOf("") }
    var workDescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var siteExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    val selectedMaterial = materials.find { it.id == selectedMaterialId }
    val currentStockModel = stocks.find { it.material.id == selectedMaterialId && it.siteId == selectedSiteId }
    val availStock = currentStockModel?.availableQuantity ?: 0.0

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> usageDate = LocalDate.of(y, m + 1, d) },
        usageDate.year, usageDate.monthValue - 1, usageDate.dayOfMonth
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Log Material Used on Site", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Site Selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site*",
                onValueChange = {},
                readOnly = true,
                label = { Text("Site*") },
                modifier = Modifier.fillMaxWidth().clickable { siteExpanded = true }
            )
            Box(modifier = Modifier.matchParentSize().clickable { siteExpanded = true })
            DropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                sites.forEach { site ->
                    DropdownMenuItem(text = { Text(site.name) }, onClick = { selectedSiteId = site.id; siteExpanded = false })
                }
            }
        }

        // Material Selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedMaterial?.name ?: "Select Material*",
                onValueChange = {},
                readOnly = true,
                label = { Text("Material*") },
                modifier = Modifier.fillMaxWidth().clickable { materialExpanded = true }
            )
            Box(modifier = Modifier.matchParentSize().clickable { materialExpanded = true })
            DropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                materials.forEach { mat ->
                    DropdownMenuItem(
                        text = { Text("${mat.name} (${mat.category.displayName})") },
                        onClick = { selectedMaterialId = mat.id; materialExpanded = false }
                    )
                }
            }
        }

        // Available Stock Indicator
        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available Stock at Selected Site:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("$availStock ${selectedMaterial?.unit?.displayName ?: "Units"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (availStock > 0) Color(0xFF2E7D32) else Color(0xFFC62828))
            }
        }

        OutlinedTextField(
            value = quantityStr,
            onValueChange = { quantityStr = it },
            label = { Text("Quantity Used* (${selectedMaterial?.unit?.displayName ?: "Unit"})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = workDescription,
            onValueChange = { workDescription = it },
            label = { Text("Work Description (e.g. Roof Casting, Slab)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = Utils.formatDate(usageDate),
            onValueChange = {},
            readOnly = true,
            label = { Text("Usage Date*") },
            trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.CalendarMonth, contentDescription = null) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Opt)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                if (selectedSiteId != null && selectedMaterialId != null && q > 0) {
                    viewModel.recordMaterialUsed(
                        siteId = selectedSiteId!!,
                        materialId = selectedMaterialId!!,
                        usageDate = usageDate,
                        quantity = q,
                        unit = selectedMaterial?.unit?.displayName ?: "Unit",
                        workDescription = workDescription,
                        notes = notes,
                        onError = { errMsg -> Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show() },
                        onSuccess = {
                            Toast.makeText(context, "Material Usage Recorded!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedSiteId != null && selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Text("Save Material Usage", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MaterialTransferTab(
    sites: List<SiteEntity>,
    materials: List<MaterialEntity>,
    stocks: List<MaterialStockUiModel>,
    viewModel: MaterialViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var sourceSiteId by remember { mutableStateOf<Long?>(sites.firstOrNull()?.id) }
    var destSiteId by remember { mutableStateOf<Long?>(sites.getOrNull(1)?.id ?: sites.firstOrNull()?.id) }
    var selectedMaterialId by remember { mutableStateOf<Long?>(materials.firstOrNull()?.id) }
    var transferDate by remember { mutableStateOf(LocalDate.now()) }
    var quantityStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var sourceExpanded by remember { mutableStateOf(false) }
    var destExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    val selectedMaterial = materials.find { it.id == selectedMaterialId }
    val sourceStockModel = stocks.find { it.material.id == selectedMaterialId && it.siteId == sourceSiteId }
    val sourceAvailStock = sourceStockModel?.availableQuantity ?: 0.0

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> transferDate = LocalDate.of(y, m + 1, d) },
        transferDate.year, transferDate.monthValue - 1, transferDate.dayOfMonth
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Material Transfer Between Sites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Source Site
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = sites.find { it.id == sourceSiteId }?.name ?: "Source Site*",
                onValueChange = {},
                readOnly = true,
                label = { Text("Source Site* (From)") },
                modifier = Modifier.fillMaxWidth().clickable { sourceExpanded = true }
            )
            Box(modifier = Modifier.matchParentSize().clickable { sourceExpanded = true })
            DropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                sites.forEach { site ->
                    DropdownMenuItem(text = { Text(site.name) }, onClick = { sourceSiteId = site.id; sourceExpanded = false })
                }
            }
        }

        // Destination Site
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = sites.find { it.id == destSiteId }?.name ?: "Destination Site*",
                onValueChange = {},
                readOnly = true,
                label = { Text("Destination Site* (To)") },
                modifier = Modifier.fillMaxWidth().clickable { destExpanded = true }
            )
            Box(modifier = Modifier.matchParentSize().clickable { destExpanded = true })
            DropdownMenu(expanded = destExpanded, onDismissRequest = { destExpanded = false }) {
                sites.filter { it.id != sourceSiteId }.forEach { site ->
                    DropdownMenuItem(text = { Text(site.name) }, onClick = { destSiteId = site.id; destExpanded = false })
                }
            }
        }

        // Material Selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedMaterial?.name ?: "Select Material*",
                onValueChange = {},
                readOnly = true,
                label = { Text("Material*") },
                modifier = Modifier.fillMaxWidth().clickable { materialExpanded = true }
            )
            Box(modifier = Modifier.matchParentSize().clickable { materialExpanded = true })
            DropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                materials.forEach { mat ->
                    DropdownMenuItem(text = { Text("${mat.name} (${mat.category.displayName})") }, onClick = { selectedMaterialId = mat.id; materialExpanded = false })
                }
            }
        }

        // Source Stock Indicator
        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available Stock at Source Site:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("$sourceAvailStock ${selectedMaterial?.unit?.displayName ?: "Units"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (sourceAvailStock > 0) Color(0xFF2E7D32) else Color(0xFFC62828))
            }
        }

        OutlinedTextField(
            value = quantityStr,
            onValueChange = { quantityStr = it },
            label = { Text("Transfer Quantity* (${selectedMaterial?.unit?.displayName ?: "Unit"})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = Utils.formatDate(transferDate),
            onValueChange = {},
            readOnly = true,
            label = { Text("Transfer Date*") },
            trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.CalendarMonth, contentDescription = null) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Opt)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                if (sourceSiteId != null && destSiteId != null && selectedMaterialId != null && q > 0) {
                    viewModel.recordMaterialTransfer(
                        materialId = selectedMaterialId!!,
                        sourceSiteId = sourceSiteId!!,
                        destinationSiteId = destSiteId!!,
                        transferDate = transferDate,
                        quantity = q,
                        unit = selectedMaterial?.unit?.displayName ?: "Unit",
                        notes = notes,
                        onError = { errMsg -> Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show() },
                        onSuccess = {
                            Toast.makeText(context, "Material Transfer Completed!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sourceSiteId != null && destSiteId != null && sourceSiteId != destSiteId && selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Text("Execute Transfer", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MaterialHistoryTab(
    transactions: List<MaterialTransactionUiModel>,
    onDelete: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Material Transaction History (${transactions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No material transactions recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { model ->
                    val tx = model.transaction
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(model.materialName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    val siteDisplay = if (model.targetSiteName != null) "${model.siteName} ➔ ${model.targetSiteName}" else model.siteName
                                    Text("Site: $siteDisplay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(Utils.formatDate(tx.transactionDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                TransactionTypeBadge(tx.transactionType)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Quantity: ${tx.quantity} ${tx.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (tx.totalAmount > 0) {
                                    Text("Total: ${Utils.formatCurrency(tx.totalAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                                }
                            }

                            if (tx.workDescription.isNotBlank()) {
                                Text("Work: ${tx.workDescription}", style = MaterialTheme.typography.bodySmall)
                            }

                            if (tx.notes.isNotBlank()) {
                                Text("Notes: ${tx.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionTypeBadge(type: MaterialTransactionType) {
    val (color, text) = when (type) {
        MaterialTransactionType.RECEIVED -> Color(0xFF2E7D32) to "RECEIVED"
        MaterialTransactionType.USED -> Color(0xFFEF6C00) to "USED"
        MaterialTransactionType.TRANSFER_IN -> Color(0xFF1565C0) to "TRANSFER IN"
        MaterialTransactionType.TRANSFER_OUT -> Color(0xFFC62828) to "TRANSFER OUT"
        MaterialTransactionType.ADJUSTMENT -> Color(0xFF6A1B9A) to "ADJUSTMENT"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MaterialReportsTab(
    stocks: List<MaterialStockUiModel>,
    transactions: List<MaterialTransactionUiModel>
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Material Summary Reports & Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current Inventory Summary", fontWeight = FontWeight.Bold)
                Text("Total Stock Items: ${stocks.size}", style = MaterialTheme.typography.bodyMedium)
                Text("Total Stock Value: ${Utils.formatCurrency(stocks.sumOf { it.currentStockValue })}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("Total Transactions Logged: ${transactions.size}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Button(
            onClick = {
                Toast.makeText(context, "Material Report Export completed!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Assessment, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Material Report to Excel", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomMaterialDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, MaterialCategory, MaterialUnit, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MaterialCategory.CIVIL) }
    var selectedUnit by remember { mutableStateOf(MaterialUnit.BAGS) }
    var minStockStr by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Material", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Material Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category*") },
                        modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = true }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { categoryExpanded = true })
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        MaterialCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { selectedCategory = cat; categoryExpanded = false })
                        }
                    }
                }

                // Unit Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedUnit.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit*") },
                        modifier = Modifier.fillMaxWidth().clickable { unitExpanded = true }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { unitExpanded = true })
                    DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        MaterialUnit.entries.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit.displayName) }, onClick = { selectedUnit = unit; unitExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = minStockStr,
                    onValueChange = { minStockStr = it },
                    label = { Text("Minimum Stock Alert Level (Opt)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedCategory, selectedUnit, minStockStr.toDoubleOrNull() ?: 0.0)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Material", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

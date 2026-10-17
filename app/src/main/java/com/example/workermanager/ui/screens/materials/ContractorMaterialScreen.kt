package com.example.workermanager.ui.screens.materials

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.*
import com.example.workermanager.util.ExcelExporter
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.ContractorMaterialStockModel
import com.example.workermanager.viewmodel.ContractorMaterialTxUiModel
import com.example.workermanager.viewmodel.ContractorMaterialViewModel
import com.example.workermanager.viewmodel.SiteMaterialStockModel
import com.example.workermanager.viewmodel.SiteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class ContractorMaterialTab(val title: String) {
    INVENTORY("Inventory"),
    SEND("Send to Site"),
    RETURN("Return from Site"),
    BROKEN_LOST("Broken / Lost"),
    HISTORY("History"),
    REPORTS("Reports")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorMaterialScreen(
    viewModel: ContractorMaterialViewModel,
    siteViewModel: SiteViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sites by siteViewModel.allSites.collectAsState(initial = emptyList())
    val materials by viewModel.allMaterials.collectAsState(initial = emptyList())
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val summary by viewModel.dashboardSummary.collectAsState()
    val stockModels by viewModel.stockModels.collectAsState()
    val siteStockModels by viewModel.siteStockModels.collectAsState()
    val transactions by viewModel.transactionUiModels.collectAsState()

    var activeTab by remember { mutableStateOf(ContractorMaterialTab.INVENTORY) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }

    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { targetUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        ExcelExporter.exportContractorMaterials(stockModels, siteStockModels, transactions, stream)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Material Report exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Material Management", fontWeight = FontWeight.Bold) },
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
            // Dashboard Summary Cards (Quantities Only - NO Prices!)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QtySummaryBox("Total Material Types", summary.totalMaterialTypes.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    QtySummaryBox("Total Added Qty", String.format("%.1f", summary.totalQuantity), Color(0xFF1565C0), Modifier.weight(1f))
                    QtySummaryBox("Available Qty", String.format("%.1f", summary.availableQuantity), Color(0xFF2E7D32), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QtySummaryBox("Quantity at Sites", String.format("%.1f", summary.quantityAtSites), Color(0xFFEF6C00), Modifier.weight(1f))
                    QtySummaryBox("Total Broken/Lost Qty", String.format("%.1f", summary.totalBrokenLostQuantity), if (summary.totalBrokenLostQuantity > 0) Color(0xFFC62828) else Color(0xFF2E7D32), Modifier.weight(1f))
                }
            }

            // Tab Selection Header
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                ContractorMaterialTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            // Search and Category Filters
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search material name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) },
                            label = { Text("All Categories", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        )
                    }
                    items(ContractorMaterialCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.setSelectedCategory(cat) },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Tab Content Body
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (activeTab) {
                    ContractorMaterialTab.INVENTORY -> {
                        MaterialInventoryTab(
                            stocks = stockModels,
                            onAddMaterialClick = { showAddMaterialDialog = true },
                            onDeactivate = { id -> viewModel.deactivateMaterial(id) }
                        )
                    }
                    ContractorMaterialTab.SEND -> {
                        SendMaterialTab(
                            sites = sites,
                            materials = materials.filter { it.isActive },
                            viewModel = viewModel,
                            onSuccess = { activeTab = ContractorMaterialTab.INVENTORY }
                        )
                    }
                    ContractorMaterialTab.RETURN -> {
                        ReturnMaterialTab(
                            sites = sites,
                            materials = materials.filter { it.isActive },
                            siteStocks = siteStockModels,
                            viewModel = viewModel,
                            onSuccess = { activeTab = ContractorMaterialTab.INVENTORY }
                        )
                    }
                    ContractorMaterialTab.BROKEN_LOST -> {
                        BrokenLostMaterialTab(
                            sites = sites,
                            materials = materials.filter { it.isActive },
                            viewModel = viewModel,
                            onSuccess = { activeTab = ContractorMaterialTab.INVENTORY }
                        )
                    }
                    ContractorMaterialTab.HISTORY -> {
                        MaterialHistoryLedgerTab(
                            transactions = transactions,
                            onDelete = { txId -> viewModel.deleteTransaction(txId) }
                        )
                    }
                    ContractorMaterialTab.REPORTS -> {
                        SiteWiseReportTab(
                            sites = sites,
                            siteStocks = siteStockModels,
                            inventory = stockModels,
                            onExportExcel = {
                                excelExportLauncher.launch(ExcelExporter.generateContractorMaterialFileName())
                            }
                        )
                    }
                }
            }
        }

        if (showAddMaterialDialog) {
            AddMaterialDialog(
                onDismiss = { showAddMaterialDialog = false },
                onConfirm = { name, category, unit, initialQty, minStock, notes ->
                    viewModel.addMaterialToInventory(name, category, unit, initialQty, minStock, notes) {
                        showAddMaterialDialog = false
                        Toast.makeText(context, "Material added successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun QtySummaryBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
        }
    }
}

@Composable
fun MaterialInventoryTab(
    stocks: List<ContractorMaterialStockModel>,
    onAddMaterialClick: () -> Unit,
    onDeactivate: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contractor Inventory (${stocks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddMaterialClick, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Add Material", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (stocks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No material inventory records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stocks) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (item.isLowStock) Color(0xFFC62828) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(item.material.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Category: ${item.material.category.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                if (item.isLowStock) {
                                    Surface(color = Color(0xFFC62828).copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
                                        Text("LOW STOCK", color = Color(0xFFC62828), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Added: ${item.totalAddedQuantity} ${item.material.unit.displayName}", fontSize = 11.sp)
                                Text("Available: ${item.availableQuantity} ${item.material.unit.displayName}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 12.sp)
                                Text("At Sites: ${item.quantityAtSites} ${item.material.unit.displayName}", fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00), fontSize = 12.sp)
                            }

                            if (item.brokenQuantity > 0 || item.lostQuantity > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Broken: ${item.brokenQuantity} | Lost: ${item.lostQuantity}", fontSize = 11.sp, color = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendMaterialTab(
    sites: List<SiteEntity>,
    materials: List<ContractorMaterialEntity>,
    viewModel: ContractorMaterialViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedSiteId by remember { mutableStateOf<Long?>(sites.firstOrNull()?.id) }
    var selectedMaterialId by remember { mutableStateOf<Long?>(materials.firstOrNull()?.id) }
    var sendDate by remember { mutableStateOf(LocalDate.now()) }
    var quantityStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var siteExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    val selectedMaterial = materials.find { it.id == selectedMaterialId }
    val stocks by viewModel.stockModels.collectAsState()
    val availStock = stocks.find { it.material.id == selectedMaterialId }?.availableQuantity ?: 0.0

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> sendDate = LocalDate.of(y, m + 1, d) },
        sendDate.year, sendDate.monthValue - 1, sendDate.dayOfMonth
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Send Material to Site", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                    DropdownMenuItem(text = { Text("${mat.name} (${mat.category.displayName})") }, onClick = { selectedMaterialId = mat.id; materialExpanded = false })
                }
            }
        }

        // Available Central Stock Box
        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available Stock (Central):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("$availStock ${selectedMaterial?.unit?.displayName ?: ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (availStock > 0) Color(0xFF2E7D32) else Color(0xFFC62828))
            }
        }

        OutlinedTextField(
            value = quantityStr,
            onValueChange = { quantityStr = it },
            label = { Text("Quantity to Send* (${selectedMaterial?.unit?.displayName ?: ""})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = Utils.formatDate(sendDate),
            onValueChange = {},
            readOnly = true,
            label = { Text("Send Date*") },
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
                    viewModel.sendMaterialToSite(
                        materialId = selectedMaterialId!!,
                        siteId = selectedSiteId!!,
                        quantity = q,
                        transactionDate = sendDate,
                        notes = notes,
                        onError = { errMsg -> Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show() },
                        onSuccess = {
                            Toast.makeText(context, "Material Sent to Site!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedSiteId != null && selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Text("Send Material", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReturnMaterialTab(
    sites: List<SiteEntity>,
    materials: List<ContractorMaterialEntity>,
    siteStocks: List<SiteMaterialStockModel>,
    viewModel: ContractorMaterialViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedSiteId by remember { mutableStateOf<Long?>(sites.firstOrNull()?.id) }
    var selectedMaterialId by remember { mutableStateOf<Long?>(materials.firstOrNull()?.id) }
    var returnDate by remember { mutableStateOf(LocalDate.now()) }
    var quantityStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var siteExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    val selectedMaterial = materials.find { it.id == selectedMaterialId }
    val siteQty = siteStocks.find { it.materialId == selectedMaterialId && it.siteId == selectedSiteId }?.currentSiteQuantity ?: 0.0

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> returnDate = LocalDate.of(y, m + 1, d) },
        returnDate.year, returnDate.monthValue - 1, returnDate.dayOfMonth
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Return Material From Site", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                    DropdownMenuItem(text = { Text(mat.name) }, onClick = { selectedMaterialId = mat.id; materialExpanded = false })
                }
            }
        }

        // Current Site Quantity Indicator
        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Quantity currently at this site:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("$siteQty ${selectedMaterial?.unit?.displayName ?: ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (siteQty > 0) Color(0xFF2E7D32) else Color(0xFFC62828))
            }
        }

        OutlinedTextField(
            value = quantityStr,
            onValueChange = { quantityStr = it },
            label = { Text("Quantity to Return* (${selectedMaterial?.unit?.displayName ?: ""})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = Utils.formatDate(returnDate),
            onValueChange = {},
            readOnly = true,
            label = { Text("Return Date*") },
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
                    viewModel.returnMaterialFromSite(
                        materialId = selectedMaterialId!!,
                        siteId = selectedSiteId!!,
                        quantity = q,
                        returnDate = returnDate,
                        notes = notes,
                        onError = { errMsg -> Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show() },
                        onSuccess = {
                            Toast.makeText(context, "Material Returned to Central Inventory!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedSiteId != null && selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Text("Return Material", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BrokenLostMaterialTab(
    sites: List<SiteEntity>,
    materials: List<ContractorMaterialEntity>,
    viewModel: ContractorMaterialViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isCentralInventory by remember { mutableStateOf(true) }
    var selectedSiteId by remember { mutableStateOf<Long?>(null) }
    var selectedMaterialId by remember { mutableStateOf<Long?>(materials.firstOrNull()?.id) }
    var isBrokenAction by remember { mutableStateOf(true) }
    var eventDate by remember { mutableStateOf(LocalDate.now()) }
    var quantityStr by remember { mutableStateOf("") }
    var reasonNotes by remember { mutableStateOf("") }

    var siteExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    val selectedMaterial = materials.find { it.id == selectedMaterialId }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> eventDate = LocalDate.of(y, m + 1, d) },
        eventDate.year, eventDate.monthValue - 1, eventDate.dayOfMonth
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mark Material Broken / Lost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isBrokenAction,
                onClick = { isBrokenAction = true },
                label = { Text("Mark Broken", fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = !isBrokenAction,
                onClick = { isBrokenAction = false },
                label = { Text("Mark Lost", fontWeight = FontWeight.Bold) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isCentralInventory,
                onClick = { isCentralInventory = true; selectedSiteId = null },
                label = { Text("Central Inventory") }
            )
            FilterChip(
                selected = !isCentralInventory,
                onClick = { isCentralInventory = false; selectedSiteId = sites.firstOrNull()?.id },
                label = { Text("At Site") }
            )
        }

        if (!isCentralInventory) {
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
                    DropdownMenuItem(text = { Text(mat.name) }, onClick = { selectedMaterialId = mat.id; materialExpanded = false })
                }
            }
        }

        OutlinedTextField(
            value = quantityStr,
            onValueChange = { quantityStr = it },
            label = { Text("Quantity* (${selectedMaterial?.unit?.displayName ?: ""})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = reasonNotes,
            onValueChange = { reasonNotes = it },
            label = { Text("Reason / Notes (Opt)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                if (selectedMaterialId != null && q > 0) {
                    if (isBrokenAction) {
                        viewModel.markMaterialBroken(
                            materialId = selectedMaterialId!!,
                            siteId = if (isCentralInventory) null else selectedSiteId,
                            quantity = q,
                            date = eventDate,
                            reason = reasonNotes,
                            onError = { errMsg -> Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show() },
                            onSuccess = {
                                Toast.makeText(context, "Marked as Broken!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        )
                    } else {
                        viewModel.markMaterialLost(
                            materialId = selectedMaterialId!!,
                            siteId = if (isCentralInventory) null else selectedSiteId,
                            quantity = q,
                            date = eventDate,
                            reason = reasonNotes,
                            onError = { errMsg -> Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show() },
                            onSuccess = {
                                Toast.makeText(context, "Marked as Lost!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(if (isBrokenAction) "Confirm Mark Broken" else "Confirm Mark Lost", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MaterialHistoryLedgerTab(
    transactions: List<ContractorMaterialTxUiModel>,
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
                                    Text("Site: ${model.siteName ?: "Central Inventory"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(Utils.formatDate(tx.transactionDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                ContractorTxBadge(tx.transactionType)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Quantity: ${tx.quantity} ${tx.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            if (tx.reasonOrNotes.isNotBlank()) {
                                Text("Notes: ${tx.reasonOrNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContractorTxBadge(type: ContractorMaterialTransactionType) {
    val (color, text) = when (type) {
        ContractorMaterialTransactionType.ADDED -> Color(0xFF1565C0) to "ADDED"
        ContractorMaterialTransactionType.SENT -> Color(0xFFEF6C00) to "SENT TO SITE"
        ContractorMaterialTransactionType.RETURNED -> Color(0xFF2E7D32) to "RETURNED"
        ContractorMaterialTransactionType.BROKEN -> Color(0xFFC62828) to "BROKEN"
        ContractorMaterialTransactionType.LOST -> Color(0xFFD84315) to "LOST"
        ContractorMaterialTransactionType.ADJUSTMENT -> Color(0xFF6A1B9A) to "ADJUSTMENT"
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
fun SiteWiseReportTab(
    sites: List<SiteEntity>,
    siteStocks: List<SiteMaterialStockModel>,
    inventory: List<ContractorMaterialStockModel>,
    onExportExcel: () -> Unit
) {
    var selectedReportSiteId by remember { mutableStateOf<Long?>(null) }
    var siteExpanded by remember { mutableStateOf(false) }

    val filteredSiteStocks = if (selectedReportSiteId != null) {
        siteStocks.filter { it.siteId == selectedReportSiteId }
    } else {
        siteStocks
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Site-wise Material Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onExportExcel, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Site Filter
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (selectedReportSiteId == null) "All Sites Combined" else sites.find { it.id == selectedReportSiteId }?.name ?: "Select Site",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Site for Report") },
                modifier = Modifier.fillMaxWidth().clickable { siteExpanded = true }
            )
            Box(modifier = Modifier.matchParentSize().clickable { siteExpanded = true })
            DropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                DropdownMenuItem(text = { Text("All Sites Combined") }, onClick = { selectedReportSiteId = null; siteExpanded = false })
                sites.forEach { site ->
                    DropdownMenuItem(text = { Text(site.name) }, onClick = { selectedReportSiteId = site.id; siteExpanded = false })
                }
            }
        }

        if (filteredSiteStocks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No material assignments for this site.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSiteStocks) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.materialName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Site: ${item.siteName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sent: ${item.totalSent} ${item.unit}", fontSize = 11.sp)
                                Text("Returned: ${item.totalReturned} ${item.unit}", fontSize = 11.sp)
                                Text("Current: ${item.currentSiteQuantity} ${item.unit}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 12.sp)
                            }

                            if (item.brokenAtSite > 0 || item.lostAtSite > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Broken at Site: ${item.brokenAtSite} | Lost at Site: ${item.lostAtSite}", fontSize = 11.sp, color = Color(0xFFC62828))
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
fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ContractorMaterialCategory, ContractorMaterialUnit, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ContractorMaterialCategory.SHUTTERING_ROOF_CASTING) }
    var selectedUnit by remember { mutableStateOf(ContractorMaterialUnit.PCS) }
    var initialQtyStr by remember { mutableStateOf("") }
    var minStockStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Construction Material", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Material Name* (e.g. Bans, Patara, Shuttering Plate)") },
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
                        ContractorMaterialCategory.entries.forEach { cat ->
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
                        ContractorMaterialUnit.entries.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit.displayName) }, onClick = { selectedUnit = unit; unitExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = initialQtyStr,
                    onValueChange = { initialQtyStr = it },
                    label = { Text("Initial Stock Quantity (Central)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minStockStr,
                    onValueChange = { minStockStr = it },
                    label = { Text("Minimum Stock Alert Level (Opt)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Opt)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            selectedCategory,
                            selectedUnit,
                            initialQtyStr.toDoubleOrNull() ?: 0.0,
                            minStockStr.toDoubleOrNull() ?: 0.0,
                            notes.trim()
                        )
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

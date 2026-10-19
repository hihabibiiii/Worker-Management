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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

val LightBlueBg = Color(0xFFF5F9FF)
val PrimaryBlue = Color(0xFF0866E8)
val DarkText = Color(0xFF0F172A)
val SubtitleText = Color(0xFF64748B)

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
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val summary by viewModel.dashboardSummary.collectAsState()
    val stockModels by viewModel.stockModels.collectAsState()
    val siteStockModels by viewModel.siteStockModels.collectAsState()
    val transactions by viewModel.transactionUiModels.collectAsState()

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else LightBlueBg
    val darkText = if (isDark) MaterialTheme.colorScheme.onSurface else DarkText
    val subtitleText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SubtitleText

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
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Material Management",
                screenIcon = Icons.Default.Inventory2,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            if (activeTab == ContractorMaterialTab.INVENTORY) {
                ExtendedFloatingActionButton(
                    onClick = { showAddMaterialDialog = true },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                    text = { Text("Add Material", fontWeight = FontWeight.Bold) },
                    shape = CircleShape
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(screenBg)
        ) {
            when (activeTab) {
                ContractorMaterialTab.INVENTORY -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. SEARCH BAR AT THE VERY TOP
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f)),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.setSearchQuery(it) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = darkText),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text("Search material name...", style = MaterialTheme.typography.bodyMedium, color = subtitleText)
                                            }
                                            innerTextField()
                                        }
                                    )
                                    IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Close else Icons.Default.FilterList,
                                            contentDescription = "Filter",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. TOP 5 SUMMARY CARDS
                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    ModernSummaryCard(
                                        title = "Total Types",
                                        value = summary.totalMaterialTypes.toString(),
                                        icon = Icons.Default.Inventory2,
                                        containerColor = if (isDark) Color(0xFF1E2A3A) else Color(0xFFD6ECFF),
                                        contentColor = if (isDark) Color(0xFF4285F4) else PrimaryBlue,
                                        darkText = darkText,
                                        modifier = Modifier.width(84.dp).height(64.dp)
                                    )
                                }
                                item {
                                    ModernSummaryCard(
                                        title = "Added",
                                        value = String.format("%.0f", summary.totalQuantity),
                                        icon = Icons.Default.AddCircle,
                                        containerColor = if (isDark) Color(0xFF2A1F3E) else Color(0xFFE8DFFF),
                                        contentColor = if (isDark) Color(0xFFB388FF) else Color(0xFF6A1B9A),
                                        darkText = darkText
                                    )
                                }
                                item {
                                    ModernSummaryCard(
                                        title = "Available",
                                        value = String.format("%.0f", summary.availableQuantity),
                                        icon = Icons.Default.CheckCircle,
                                        containerColor = if (isDark) Color(0xFF1B382B) else Color(0xFFD7FBE0),
                                        contentColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF2E7D32),
                                        darkText = darkText
                                    )
                                }
                                item {
                                    ModernSummaryCard(
                                        title = "At Sites",
                                        value = String.format("%.0f", summary.quantityAtSites),
                                        icon = Icons.Default.LocationOn,
                                        containerColor = if (isDark) Color(0xFF3E2C1B) else Color(0xFFFFE8CC),
                                        contentColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFEF6C00),
                                        darkText = darkText
                                    )
                                }
                                item {
                                    ModernSummaryCard(
                                        title = "Broken/Lost",
                                        value = String.format("%.0f", summary.totalBrokenLostQuantity),
                                        icon = Icons.Default.LinkOff,
                                        containerColor = if (isDark) Color(0xFF3A1E27) else Color(0xFFFFDCE4),
                                        contentColor = if (isDark) Color(0xFFFF5252) else Color(0xFFC62828),
                                        darkText = darkText
                                    )
                                }
                            }
                        }

                        // 3. CORE ACTION CHIPS (STYLED WITH A 5% PROPORTIONAL SIZE INCREASE)
                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    CoreActionChip(
                                        label = "Inventory",
                                        icon = Icons.Default.Inventory2,
                                        isSelected = activeTab == ContractorMaterialTab.INVENTORY,
                                        isDark = isDark,
                                        darkText = darkText,
                                        onClick = { activeTab = ContractorMaterialTab.INVENTORY }
                                    )
                                }
                                item {
                                    CoreActionChip(
                                        label = "Send to Site",
                                        icon = Icons.Default.Send,
                                        isSelected = activeTab == ContractorMaterialTab.SEND,
                                        isDark = isDark,
                                        darkText = darkText,
                                        onClick = { activeTab = ContractorMaterialTab.SEND }
                                    )
                                }
                                item {
                                    CoreActionChip(
                                        label = "Return from Site",
                                        icon = Icons.Default.Reply,
                                        isSelected = activeTab == ContractorMaterialTab.RETURN,
                                        isDark = isDark,
                                        darkText = darkText,
                                        onClick = { activeTab = ContractorMaterialTab.RETURN }
                                    )
                                }
                                item {
                                    CoreActionChip(
                                        label = "Add Material",
                                        icon = Icons.Default.AddCircle,
                                        isSelected = showAddMaterialDialog,
                                        isDark = isDark,
                                        darkText = darkText,
                                        onClick = { showAddMaterialDialog = true }
                                    )
                                }
                                item {
                                    CoreActionChip(
                                        label = "Broken / Lost",
                                        icon = Icons.Default.LinkOff,
                                        isSelected = activeTab == ContractorMaterialTab.BROKEN_LOST,
                                        isDark = isDark,
                                        darkText = darkText,
                                        onClick = { activeTab = ContractorMaterialTab.BROKEN_LOST }
                                    )
                                }
                                item {
                                    CoreActionChip(
                                        label = "History Ledger",
                                        icon = Icons.Default.History,
                                        isSelected = activeTab == ContractorMaterialTab.HISTORY,
                                        isDark = isDark,
                                        darkText = darkText,
                                        onClick = { activeTab = ContractorMaterialTab.HISTORY }
                                    )
                                }
                                item {
                                    CoreActionChip(
                                        label = "Site Reports",
                                        icon = Icons.Default.Assessment,
                                        isSelected = activeTab == ContractorMaterialTab.REPORTS,
                                        isDark = isDark,
                                        darkText = darkText,
                                        onClick = { activeTab = ContractorMaterialTab.REPORTS }
                                    )
                                }
                            }
                        }

                        // 4. HORIZONTAL CATEGORY CHIPS
                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    CategoryFilterChip("All Categories", Icons.Default.GridView, selectedCategory == null, isDark, darkText, { viewModel.setSelectedCategory(null) })
                                }
                                items(ContractorMaterialCategory.entries) { cat ->
                                    val catIcon = when (cat) {
                                        ContractorMaterialCategory.SHUTTERING_ROOF_CASTING -> Icons.Default.Layers
                                        ContractorMaterialCategory.CONSTRUCTION_TOOLS_MATERIALS -> Icons.Default.Build
                                        ContractorMaterialCategory.MASON_TOOLS -> Icons.Default.Construction
                                        ContractorMaterialCategory.OTHER_SITE_EQUIPMENT -> Icons.Default.PrecisionManufacturing
                                        ContractorMaterialCategory.OTHER -> Icons.Default.Category
                                    }
                                    CategoryFilterChip(cat.displayName, catIcon, selectedCategory == cat, isDark, darkText, { viewModel.setSelectedCategory(cat) })
                                }
                            }
                        }

                        // 5. CONTRACTOR INVENTORY SECTION HEADING
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Contractor Inventory (${stockModels.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = darkText
                                        )
                                    }
                                    Icon(Icons.Default.Tune, contentDescription = "Decor", tint = PrimaryBlue.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(modifier = Modifier.width(60.dp).height(3.dp).background(PrimaryBlue, shape = RoundedCornerShape(2.dp)))
                            }
                        }

                        // 6. MATERIAL INVENTORY CARDS
                        if (stockModels.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No material inventory records found.", color = subtitleText)
                                }
                            }
                        } else {
                            items(stockModels) { item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    MaterialInventoryCard(item = item, isDark = isDark, darkText = darkText, subtitleText = subtitleText)
                                }
                            }
                        }
                    }
                }
                ContractorMaterialTab.SEND -> {
                    SendMaterialTab(
                        sites = sites,
                        materials = materials.filter { it.isActive },
                        viewModel = viewModel,
                        darkText = darkText,
                        onSuccess = { activeTab = ContractorMaterialTab.INVENTORY }
                    )
                }
                ContractorMaterialTab.RETURN -> {
                    ReturnMaterialTab(
                        sites = sites,
                        materials = materials.filter { it.isActive },
                        siteStocks = siteStockModels,
                        viewModel = viewModel,
                        darkText = darkText,
                        onSuccess = { activeTab = ContractorMaterialTab.INVENTORY }
                    )
                }
                ContractorMaterialTab.BROKEN_LOST -> {
                    BrokenLostMaterialTab(
                        sites = sites,
                        materials = materials.filter { it.isActive },
                        viewModel = viewModel,
                        darkText = darkText,
                        onSuccess = { activeTab = ContractorMaterialTab.INVENTORY }
                    )
                }
                ContractorMaterialTab.HISTORY -> {
                    MaterialHistoryLedgerTab(
                        transactions = transactions,
                        darkText = darkText,
                        subtitleText = subtitleText,
                        onDelete = { txId -> viewModel.deleteTransaction(txId) }
                    )
                }
                ContractorMaterialTab.REPORTS -> {
                    SiteWiseReportTab(
                        sites = sites,
                        siteStocks = siteStockModels,
                        inventory = stockModels,
                        darkText = darkText,
                        subtitleText = subtitleText,
                        onExportExcel = {
                            excelExportLauncher.launch(ExcelExporter.generateContractorMaterialFileName())
                        }
                    )
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
fun ModernSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    darkText: Color = DarkText,
    modifier: Modifier = Modifier.width(105.dp).height(68.dp)
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = darkText, maxLines = 1)
            Spacer(modifier = Modifier.height(1.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = contentColor, maxLines = 1)
        }
    }
}

@Composable
fun CoreActionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean = false,
    darkText: Color = DarkText,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryBlue else (if (isDark) MaterialTheme.colorScheme.surface else Color.White),
        contentColor = if (isSelected) Color.White else darkText,
        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else (if (isDark) Color(0xFF333A45) else Color(0xFFCBD5E1))),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else PrimaryBlue,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryFilterChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean = false,
    darkText: Color = DarkText,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryBlue else (if (isDark) MaterialTheme.colorScheme.surface else Color.White),
        contentColor = if (isSelected) Color.White else darkText,
        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else (if (isDark) Color(0xFF333A45) else Color(0xFFCBD5E1))),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else PrimaryBlue,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MaterialInventoryCard(
    item: ContractorMaterialStockModel,
    isDark: Boolean = false,
    darkText: Color = DarkText,
    subtitleText: Color = SubtitleText
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        border = BorderStroke(1.dp, if (item.isLowStock) Color(0xFFFF5252).copy(alpha = 0.4f) else PrimaryBlue.copy(alpha = 0.15f)),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (item.isLowStock) Color(0xFFFF5252) else PrimaryBlue)
            )

            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (item.material.category) {
                                        ContractorMaterialCategory.SHUTTERING_ROOF_CASTING -> Icons.Default.Layers
                                        ContractorMaterialCategory.CONSTRUCTION_TOOLS_MATERIALS -> Icons.Default.Build
                                        ContractorMaterialCategory.MASON_TOOLS -> Icons.Default.Construction
                                        ContractorMaterialCategory.OTHER_SITE_EQUIPMENT -> Icons.Default.PrecisionManufacturing
                                        ContractorMaterialCategory.OTHER -> Icons.Default.Category
                                    },
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.material.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = darkText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.material.category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF4285F4) else PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = if (isDark) Color(0xFF1B382B) else Color(0xFFD7FBE0),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, (if (isDark) Color(0xFF2ECC71) else Color(0xFF2E7D32)).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "✓ ${String.format("%.0f", item.availableQuantity)} ${item.material.unit.displayName} Avail.",
                            color = if (isDark) Color(0xFF2ECC71) else Color(0xFF2E7D32),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = if (isDark) Color(0xFF333A45) else Color(0xFFF1F5F9))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Total Added", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = subtitleText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format("%.0f", item.totalAddedQuantity)} ${item.material.unit.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF4285F4) else PrimaryBlue
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp), color = if (isDark) Color(0xFF333A45) else Color(0xFFE2E8F0))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("At Sites", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = subtitleText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format("%.0f", item.quantityAtSites)} ${item.material.unit.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFFFB74D) else Color(0xFFEF6C00)
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp), color = if (isDark) Color(0xFF333A45) else Color(0xFFE2E8F0))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lost / Broken", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = subtitleText)
                        Spacer(modifier = Modifier.height(2.dp))
                        val totalLostBroken = item.brokenQuantity + item.lostQuantity
                        Text(
                            text = "${String.format("%.0f", totalLostBroken)} ${item.material.unit.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (totalLostBroken > 0) (if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)) else (if (isDark) Color(0xFF2ECC71) else Color(0xFF2E7D32))
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = subtitleText,
                        modifier = Modifier.size(20.dp)
                    )
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
    darkText: Color = DarkText,
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
        Text("Send Material to Site", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)

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
        Surface(color = Color(0xFFD7FBE0), shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available Stock (Central):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DarkText)
                Text("${String.format("%.0f", availStock)} ${selectedMaterial?.unit?.displayName ?: ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (availStock > 0) Color(0xFF2E7D32) else Color(0xFFC62828))
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
            enabled = selectedSiteId != null && selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
    darkText: Color = DarkText,
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
        Text("Return Material From Site", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)

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
        Surface(color = Color(0xFFFFE8CC), shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Color(0xFFEF6C00).copy(alpha = 0.3f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Quantity currently at this site:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DarkText)
                Text("${String.format("%.0f", siteQty)} ${selectedMaterial?.unit?.displayName ?: ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (siteQty > 0) Color(0xFFEF6C00) else Color(0xFFC62828))
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
            enabled = selectedSiteId != null && selectedMaterialId != null && (quantityStr.toDoubleOrNull() ?: 0.0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
    darkText: Color = DarkText,
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
        Text("Mark Material Broken / Lost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)

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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
            Text(if (isBrokenAction) "Confirm Mark Broken" else "Confirm Mark Lost", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MaterialHistoryLedgerTab(
    transactions: List<ContractorMaterialTxUiModel>,
    darkText: Color = DarkText,
    subtitleText: Color = SubtitleText,
    onDelete: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Material Transaction History (${transactions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No material transactions recorded.", color = subtitleText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { model ->
                    val tx = model.transaction
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(model.materialName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)
                                    Text("Site: ${model.siteName ?: "Central Inventory"}", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                    Text(Utils.formatDate(tx.transactionDate), style = MaterialTheme.typography.bodySmall, color = subtitleText)
                                }

                                ContractorTxBadge(tx.transactionType)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Quantity: ${tx.quantity} ${tx.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = darkText)

                            if (tx.reasonOrNotes.isNotBlank()) {
                                Text("Notes: ${tx.reasonOrNotes}", style = MaterialTheme.typography.bodySmall, color = subtitleText)
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
    darkText: Color = DarkText,
    subtitleText: Color = SubtitleText,
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
            Text("Site-wise Material Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)
            Button(onClick = onExportExcel, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
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
                Text("No material assignments for this site.", color = subtitleText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSiteStocks) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.materialName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)
                            Text("Site: ${item.siteName}", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.Bold)

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
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Material", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

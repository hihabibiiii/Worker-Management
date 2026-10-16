package com.example.workermanager.ui.screens.expenses

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.*
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteExpensesScreen(
    viewModel: SiteExpenseViewModel,
    siteViewModel: SiteViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val sites by siteViewModel.allSites.collectAsState(initial = emptyList())
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val stats by viewModel.dashboardStats.collectAsState()
    val uiModels by viewModel.expenseUiModels.collectAsState()

    val selectedSiteName = sites.find { it.id == selectedSiteId }?.name

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var selectedUiModelDetails by remember { mutableStateOf<SiteExpenseUiModel?>(null) }
    var expenseToEdit by remember { mutableStateOf<SiteExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<SiteExpenseEntity?>(null) }
    var expenseToRecordPayment by remember { mutableStateOf<SiteExpenseUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedSiteName ?: "Site Expenses",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedSiteId != null) {
                                viewModel.setSelectedSite(null)
                            } else {
                                onBack()
                            }
                        }
                    ) {
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Expense", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedSiteId == null) {
                // MAIN SCREEN: SITE SEARCH & SITE LIST
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search site name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Select a Site to View Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredSites = if (searchQuery.isBlank()) {
                        sites
                    } else {
                        sites.filter {
                            it.name.lowercase().contains(searchQuery.trim().lowercase()) ||
                            it.address.lowercase().contains(searchQuery.trim().lowercase())
                        }
                    }

                    if (filteredSites.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No project sites found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredSites) { site ->
                                SiteExpenseSelectCard(
                                    site = site,
                                    onClick = { viewModel.setSelectedSite(site.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                // SELECTED SITE DETAILS & EXPENSE HISTORY VIEW
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selected Site 4-Item Summary Cards
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExpenseStatBox("Today Expense", Utils.formatCurrency(stats.todayExpense), Color(0xFFEF6C00), Modifier.weight(1f))
                                ExpenseStatBox("Total Expense", Utils.formatCurrency(stats.totalExpense), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExpenseStatBox("Total Paid", Utils.formatCurrency(stats.totalPaid), Color(0xFF2E7D32), Modifier.weight(1f))
                                ExpenseStatBox("Total Remain", Utils.formatCurrency(stats.totalPending), if (stats.totalPending > 0) Color(0xFFC62828) else Color(0xFF2E7D32), Modifier.weight(1f))
                            }
                        }
                    }

                    // Site History Heading
                    item {
                        Text(
                            "Site History (${uiModels.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (uiModels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Is site ke liye abhi koi expense record nahi hai.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(uiModels) { model ->
                            ExpenseCard(
                                model = model,
                                onClick = { selectedUiModelDetails = model },
                                onRecordPayment = { expenseToRecordPayment = model },
                                onEdit = { expenseToEdit = model.expense },
                                onDelete = { expenseToDelete = model.expense }
                            )
                        }
                    }
                }
            }
        }

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                sites = sites,
                initialSiteId = selectedSiteId,
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { siteId, date, cat, desc, qty, rate, amt, paidBy, method, notes, receiptPath, initialPaid ->
                    viewModel.addExpense(siteId, date, cat, desc, qty, rate, amt, paidBy, method, notes, receiptPath, initialPaid)
                    showAddExpenseDialog = false
                    Toast.makeText(context, "Expense recorded successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (selectedUiModelDetails != null) {
            ExpenseDetailsDialog(
                model = selectedUiModelDetails!!,
                viewModel = viewModel,
                onDismiss = { selectedUiModelDetails = null },
                onRecordPayment = {
                    expenseToRecordPayment = selectedUiModelDetails
                    selectedUiModelDetails = null
                },
                onEdit = {
                    expenseToEdit = selectedUiModelDetails!!.expense
                    selectedUiModelDetails = null
                },
                onDelete = {
                    expenseToDelete = selectedUiModelDetails!!.expense
                    selectedUiModelDetails = null
                }
            )
        }

        if (expenseToEdit != null) {
            EditExpenseDialog(
                expense = expenseToEdit!!,
                sites = sites,
                onDismiss = { expenseToEdit = null },
                onConfirm = { updated ->
                    viewModel.updateExpense(updated)
                    expenseToEdit = null
                    Toast.makeText(context, "Expense updated!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = { expenseToDelete = null },
                title = { Text("Delete Expense?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this expense record and its linked payments?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteExpense(expenseToDelete!!.id)
                        expenseToDelete = null
                        Toast.makeText(context, "Expense deleted.", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expenseToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (expenseToRecordPayment != null) {
            RecordExpensePaymentDialog(
                model = expenseToRecordPayment!!,
                onDismiss = { expenseToRecordPayment = null },
                onConfirm = { date, amount, paidBy, method, notes ->
                    viewModel.addExpensePayment(expenseToRecordPayment!!.expense.id, date, amount, paidBy, method, notes)
                    expenseToRecordPayment = null
                    Toast.makeText(context, "Payment recorded!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SiteExpenseSelectCard(site: SiteEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (site.address.isNotEmpty()) {
                    Text(
                        text = site.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Manager: ${site.manager.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Expenses",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExpenseStatBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
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
fun ExpenseChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
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
fun ExpenseCard(
    model: SiteExpenseUiModel,
    onClick: () -> Unit,
    onRecordPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.expense.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${model.expense.category.displayName} • ${model.siteName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(Utils.formatDate(model.expense.expenseDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (model.expense.paidBy.isNotBlank() && model.status != ExpensePaymentStatus.UNPAID) {
                        Text("Paid By: ${model.expense.paidBy}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
                StatusBadge(model.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total: ${Utils.formatCurrency(model.expense.amount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Paid: ${Utils.formatCurrency(model.totalPaid)}", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Text("Remain: ${Utils.formatCurrency(model.pendingAmount)}", fontSize = 12.sp, color = if (model.pendingAmount > 0) Color(0xFFC62828) else Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }

            if (model.pendingAmount > 0.001) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRecordPayment,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Record Payment (Udhaar)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ExpensePaymentStatus) {
    val (color, text) = when (status) {
        ExpensePaymentStatus.PAID -> Color(0xFF2E7D32) to "PAID"
        ExpensePaymentStatus.PARTIALLY_PAID -> Color(0xFFEF6C00) to "PARTIAL"
        ExpensePaymentStatus.UNPAID -> Color(0xFFC62828) to "UDHAAR"
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

enum class PaymentModeOption {
    PAID, UDHAAR, PARTIAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    sites: List<SiteEntity>,
    initialSiteId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, LocalDate, ExpenseCategory, String, Double?, Double?, Double, String, ExpensePaymentMethod, String, String?, Double) -> Unit
) {
    var selectedSiteIdState by remember { mutableStateOf<Long?>(initialSiteId ?: sites.firstOrNull()?.id) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategoryState by remember { mutableStateOf<ExpenseCategory?>(ExpenseCategory.NASHTA) }
    var amountStr by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf(PaymentModeOption.PAID) }
    var partialAmountStr by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(ExpensePaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }

    var siteExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
        selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("+ Add Site Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Site Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteIdState }?.name ?: "Select Site*",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site*") },
                        trailingIcon = {
                            IconButton(onClick = { siteExpanded = true }) {
                                Icon(
                                    imageVector = if (siteExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Site"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { siteExpanded = true }
                    )
                    DropdownMenu(
                        expanded = siteExpanded,
                        onDismissRequest = { siteExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        if (sites.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No sites available", color = MaterialTheme.colorScheme.error) },
                                onClick = { siteExpanded = false }
                            )
                        } else {
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = site.name,
                                            fontWeight = if (site.id == selectedSiteIdState) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = { selectedSiteIdState = site.id; siteExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Category Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategoryState?.displayName ?: "Select Category*",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category*") },
                        trailingIcon = {
                            IconButton(onClick = { categoryExpanded = true }) {
                                Icon(
                                    imageVector = if (categoryExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Category"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { categoryExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat.displayName,
                                        fontWeight = if (cat == selectedCategoryState) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = { selectedCategoryState = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                // Total Expense Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Expense Amount* (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Status Options (PAID, UDHAAR, PARTIAL)
                Text("Payment Status*", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseChip(
                        label = "PAID",
                        isSelected = selectedPaymentMode == PaymentModeOption.PAID,
                        onClick = { selectedPaymentMode = PaymentModeOption.PAID }
                    )
                    ExpenseChip(
                        label = "Unpaid",
                        isSelected = selectedPaymentMode == PaymentModeOption.UDHAAR,
                        onClick = { selectedPaymentMode = PaymentModeOption.UDHAAR }
                    )
                    ExpenseChip(
                        label = "PARTIAL",
                        isSelected = selectedPaymentMode == PaymentModeOption.PARTIAL,
                        onClick = { selectedPaymentMode = PaymentModeOption.PARTIAL }
                    )
                }

                if (selectedPaymentMode == PaymentModeOption.PARTIAL) {
                    OutlinedTextField(
                        value = partialAmountStr,
                        onValueChange = { partialAmountStr = it },
                        label = { Text("Amount Paid Now (₹)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Payment Method & Paid By
                if (selectedPaymentMode != PaymentModeOption.UDHAAR) {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = { paidBy = it },
                        label = { Text("Paid By (Kisne Payment Kiya?)*") },
                        placeholder = { Text("e.g. Habibullah, Manager, Self") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpensePaymentMethod.entries.forEach { method ->
                            ExpenseChip(
                                label = method.displayName,
                                isSelected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                        }
                    }
                }

                // Expense Date
                OutlinedTextField(
                    value = Utils.formatDate(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expense Date*") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
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
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val initialPaid = when (selectedPaymentMode) {
                        PaymentModeOption.PAID -> amt
                        PaymentModeOption.UDHAAR -> 0.0
                        PaymentModeOption.PARTIAL -> (partialAmountStr.toDoubleOrNull() ?: 0.0).coerceAtMost(amt)
                    }
                    val finalPaidBy = if (selectedPaymentMode == PaymentModeOption.UDHAAR) "" else paidBy.trim()

                    if (selectedSiteIdState != null && selectedCategoryState != null && amt > 0) {
                        onConfirm(
                            selectedSiteIdState!!,
                            selectedDate,
                            selectedCategoryState!!,
                            selectedCategoryState!!.displayName,
                            null,
                            null,
                            amt,
                            finalPaidBy,
                            selectedMethod,
                            notes.trim(),
                            null,
                            initialPaid
                        )
                    }
                },
                enabled = selectedSiteIdState != null &&
                        selectedCategoryState != null &&
                        (amountStr.toDoubleOrNull() ?: 0.0) > 0 &&
                        (selectedPaymentMode == PaymentModeOption.UDHAAR || paidBy.trim().isNotBlank()) &&
                        (selectedPaymentMode != PaymentModeOption.PARTIAL || (partialAmountStr.toDoubleOrNull() ?: 0.0) > 0)
            ) {
                Text("Save Expense", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: SiteExpenseEntity,
    sites: List<SiteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (SiteExpenseEntity) -> Unit
) {
    var selectedSiteId by remember { mutableStateOf<Long?>(expense.siteId) }
    var selectedDate by remember { mutableStateOf(expense.expenseDate) }
    var selectedCategory by remember { mutableStateOf(expense.category) }
    var description by remember { mutableStateOf(expense.description) }
    var amountStr by remember { mutableStateOf(expense.amount.toString()) }
    var paidBy by remember { mutableStateOf(expense.paidBy) }
    var notes by remember { mutableStateOf(expense.notes) }

    var siteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = !siteExpanded }) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        sites.forEach { site ->
                            DropdownMenuItem(text = { Text(site.name) }, onClick = { selectedSiteId = site.id; siteExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description*") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Expense Amount* (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paidBy,
                    onValueChange = { paidBy = it },
                    label = { Text("Paid By") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (selectedSiteId != null && description.isNotBlank() && amt > 0) {
                        onConfirm(
                            expense.copy(
                                siteId = selectedSiteId!!,
                                expenseDate = selectedDate,
                                category = selectedCategory,
                                description = description.trim(),
                                amount = amt,
                                paidBy = paidBy.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = selectedSiteId != null && description.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RecordExpensePaymentDialog(
    model: SiteExpenseUiModel,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Double, String, ExpensePaymentMethod, String) -> Unit
) {
    var amountStr by remember { mutableStateOf(model.pendingAmount.toString()) }
    var paidBy by remember { mutableStateOf(model.expense.paidBy) }
    var selectedMethod by remember { mutableStateOf(ExpensePaymentMethod.CASH) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
        selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Expense: ${model.expense.description}", fontWeight = FontWeight.Bold)
                Text("Pending Amount: ${Utils.formatCurrency(model.pendingAmount)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount Paying Now* (Max ₹${model.pendingAmount})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paidBy,
                    onValueChange = { paidBy = it },
                    label = { Text("Paid By (Kisne Payment Kiya?)*") },
                    placeholder = { Text("e.g. Habibullah, Manager") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && amt <= model.pendingAmount + 0.01) {
                        onConfirm(selectedDate, amt, paidBy.trim(), selectedMethod, notes.trim())
                    } else {
                        Toast.makeText(context, "Payment amount cannot exceed pending amount ₹${model.pendingAmount}", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0 && paidBy.trim().isNotBlank()
            ) {
                Text("Save Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExpenseDetailsDialog(
    model: SiteExpenseUiModel,
    viewModel: SiteExpenseViewModel,
    onDismiss: () -> Unit,
    onRecordPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val payments by viewModel.getPaymentsForExpense(model.expense.id).collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(model.expense.description, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Site: ${model.siteName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Category: ${model.expense.category.displayName}", style = MaterialTheme.typography.bodyMedium)
                Text("Date: ${Utils.formatDate(model.expense.expenseDate)}", style = MaterialTheme.typography.bodyMedium)
                Text("Total Expense: ${Utils.formatCurrency(model.expense.amount)}", fontWeight = FontWeight.Bold)
                Text("Total Paid: ${Utils.formatCurrency(model.totalPaid)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Text("Pending Amount: ${Utils.formatCurrency(model.pendingAmount)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                if (model.expense.paidBy.isNotBlank()) Text("Payer: ${model.expense.paidBy}", style = MaterialTheme.typography.bodySmall)
                if (model.expense.notes.isNotBlank()) Text("Notes: ${model.expense.notes}", style = MaterialTheme.typography.bodySmall)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Payment History (${payments.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                if (payments.isEmpty()) {
                    Text("No payments recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    payments.forEach { pay ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(Utils.formatDate(pay.paymentDate), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (pay.paidBy.isNotBlank()) Text("By: ${pay.paidBy}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(Utils.formatCurrency(pay.amount), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (model.pendingAmount > 0.001) {
                    Button(onClick = onRecordPayment) { Text("Pay", fontWeight = FontWeight.Bold) }
                }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

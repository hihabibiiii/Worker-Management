package com.example.workermanager.ui.screens.payments

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workermanager.data.entity.*
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.PaymentViewModel
import com.example.workermanager.viewmodel.PaymentsUiState
import com.example.workermanager.viewmodel.SiteViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: PaymentViewModel,
    siteViewModel: SiteViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sites by siteViewModel.allSites.collectAsStateWithLifecycle()
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val selectedSiteId by viewModel.selectedSiteId.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<PaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    Scaffold(
        topBar = {
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Payments",
                screenIcon = Icons.Default.Payments
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Payment", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Site Selector
            LazyRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SiteChip(
                        name = "All Sites",
                        isSelected = selectedSiteId == null,
                        onClick = { viewModel.updateSelectedSite(null) }
                    )
                }
                items(sites) { site ->
                    SiteChip(
                        name = site.name,
                        isSelected = selectedSiteId == site.id,
                        onClick = { viewModel.updateSelectedSite(site.id) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is PaymentsUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is PaymentsUiState.Empty -> {
                        Text(
                            text = "No payments recorded for this selection.",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is PaymentsUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.payments,
                                key = { it.payment.id }
                            ) { model ->
                                PaymentCard(
                                    payment = model.payment,
                                    workerName = model.workerName,
                                    siteName = model.siteName,
                                    onEdit = { paymentToEdit = model.payment },
                                    onDelete = { paymentToDelete = model.payment }
                                )
                            }
                        }
                    }
                }
            }
        }

        val context = LocalContext.current

        if (showAddDialog) {
            AddPaymentDialog(
                sites = sites,
                workers = workers,
                onDismiss = { showAddDialog = false },
                onConfirm = { workerId, siteId, amount, method, type, date, note ->
                    viewModel.addPayment(
                        workerId = workerId,
                        siteId = siteId,
                        amount = amount,
                        method = method,
                        type = type,
                        date = date,
                        note = note,
                        onSuccess = {
                            showAddDialog = false
                            if (method == PaymentMethod.UPI) {
                                com.example.workermanager.util.UpiUtils.launchUpiApp(context)
                            }
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Failed to save payment: ${error.localizedMessage ?: "Database error"}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        if (paymentToEdit != null) {
            EditPaymentDialog(
                payment = paymentToEdit!!,
                sites = sites,
                onDismiss = { paymentToEdit = null },
                onConfirm = { updatedPayment ->
                    viewModel.updatePayment(
                        updatedPayment,
                        onSuccess = {
                            paymentToEdit = null
                            if (updatedPayment.method == PaymentMethod.UPI) {
                                com.example.workermanager.util.UpiUtils.launchUpiApp(context)
                            }
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Failed to update payment: ${error.localizedMessage ?: "Database error"}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        if (paymentToDelete != null) {
            AlertDialog(
                onDismissRequest = { paymentToDelete = null },
                title = { Text("Delete Payment?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this payment?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePayment(paymentToDelete!!)
                        paymentToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { paymentToDelete = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}

@Composable
fun SiteChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        tonalElevation = 2.dp,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = name,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentCard(
    payment: PaymentEntity,
    workerName: String,
    siteName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true }
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        workerName, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Site: $siteName", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Date: ${Utils.formatDate(payment.date)}", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Type: ${payment.paymentType.name} | ${payment.method.name}", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (payment.note.isNotEmpty()) {
                        Text(
                            "Note: ${payment.note}", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Utils.formatCurrency(payment.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32) // Darker Green
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = Color(0xFFC62828)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPaymentDialog(
    sites: List<SiteEntity>,
    workers: List<WorkerEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Double, PaymentMethod, PaymentType, LocalDate, String) -> Unit
) {
    var selectedSiteId by remember { mutableStateOf<Long?>(null) }
    var selectedWorkerId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedType by remember { mutableStateOf(PaymentType.PAYMENT) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    
    var siteExpanded by remember { mutableStateOf(false) }
    var workerExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Make Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Site Selection
                ExposedDropdownMenuBox(
                    expanded = siteExpanded,
                    onExpandedChange = { siteExpanded = !siteExpanded }
                ) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site*",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name, fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    selectedSiteId = site.id
                                    selectedWorkerId = null
                                    siteExpanded = false 
                                }
                            )
                        }
                    }
                }

                // Worker Selection
                val filteredWorkers = if (selectedSiteId != null) {
                    workers.filter { it.currentSiteId == selectedSiteId }
                } else {
                    emptyList()
                }

                ExposedDropdownMenuBox(
                    expanded = workerExpanded,
                    onExpandedChange = { 
                        if (selectedSiteId != null) workerExpanded = !workerExpanded 
                    }
                ) {
                    OutlinedTextField(
                        value = filteredWorkers.find { it.id == selectedWorkerId }?.name ?: "Select Labour*",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Labour") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = selectedSiteId != null
                    )
                    ExposedDropdownMenu(
                        expanded = workerExpanded,
                        onDismissRequest = { workerExpanded = false }
                    ) {
                        if (filteredWorkers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No labour assigned to this site", color = MaterialTheme.colorScheme.error) },
                                onClick = { workerExpanded = false }
                            )
                        } else {
                            filteredWorkers.forEach { worker ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(worker.name, fontWeight = FontWeight.Medium)
                                            Text(worker.workerType.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = { 
                                        selectedWorkerId = worker.id
                                        workerExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedSiteId == null) {
                    Text("Site selection required", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = Utils.formatDate(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date*") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaymentType.entries.forEach { type ->
                        val isTypeSelected = selectedType == type
                        FilterChip(
                            selected = isTypeSelected,
                            onClick = { selectedType = type },
                            label = { Text(type.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isTypeSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Text("Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        val isMethodSelected = selectedMethod == method
                        FilterChip(
                            selected = isMethodSelected,
                            onClick = { selectedMethod = method },
                            label = { Text(method.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isMethodSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            var isSaving by remember { mutableStateOf(false) }
            val buttonText = if (selectedMethod == PaymentMethod.UPI) "Save & Open UPI App" else "Save"
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (selectedSiteId != null && selectedWorkerId != null && amountVal > 0 && !isSaving) {
                        isSaving = true
                        onConfirm(selectedWorkerId!!, selectedSiteId!!, amountVal, selectedMethod, selectedType, selectedDate, note)
                    }
                },
                enabled = !isSaving && selectedSiteId != null && selectedWorkerId != null && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditPaymentDialog(
    payment: PaymentEntity,
    sites: List<SiteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (PaymentEntity) -> Unit
) {
    var amount by remember { mutableStateOf(payment.amount.toString()) }
    var selectedMethod by remember { mutableStateOf(payment.method) }
    var selectedType by remember { mutableStateOf(payment.paymentType) }
    var selectedDate by remember { mutableStateOf(payment.date) }
    var selectedSiteId by remember { mutableStateOf<Long?>(payment.siteId) }
    var note by remember { mutableStateOf(payment.note) }

    var siteExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Site Selection
                ExposedDropdownMenuBox(
                    expanded = siteExpanded,
                    onExpandedChange = { siteExpanded = !siteExpanded }
                ) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site*",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name, fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    selectedSiteId = site.id
                                    siteExpanded = false 
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = Utils.formatDate(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date*") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaymentType.entries.forEach { type ->
                        val isTypeSelected = selectedType == type
                        FilterChip(
                            selected = isTypeSelected,
                            onClick = { selectedType = type },
                            label = { Text(type.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isTypeSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Text("Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        val isMethodSelected = selectedMethod == method
                        FilterChip(
                            selected = isMethodSelected,
                            onClick = { selectedMethod = method },
                            label = { Text(method.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isMethodSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            var isSaving by remember { mutableStateOf(false) }
            val buttonText = if (selectedMethod == PaymentMethod.UPI) "Save & Open UPI App" else "Save Changes"
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (selectedSiteId != null && amountVal > 0 && !isSaving) {
                        isSaving = true
                        onConfirm(
                            payment.copy(
                                siteId = selectedSiteId!!,
                                amount = amountVal,
                                method = selectedMethod,
                                paymentType = selectedType,
                                date = selectedDate,
                                note = note
                            )
                        )
                    }
                },
                enabled = !isSaving && selectedSiteId != null && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

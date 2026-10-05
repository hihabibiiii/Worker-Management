package com.example.workermanager.ui.screens.payments

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
    val selectedSiteId by viewModel.selectedSiteId.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Payments") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
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
                            color = Color.Gray
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
                                    onDelete = { paymentToDelete = model.payment }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddPaymentDialog(
                sites = sites,
                onDismiss = { showAddDialog = false },
                onConfirm = { workerId, siteId, amount, method, type, date, note ->
                    viewModel.addPayment(workerId, siteId, amount, method, type, date, note)
                    showAddDialog = false
                }
            )
        }

        if (paymentToDelete != null) {
            AlertDialog(
                onDismissRequest = { paymentToDelete = null },
                title = { Text("Delete Payment") },
                text = { Text("Are you sure you want to delete this payment record?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePayment(paymentToDelete!!)
                        paymentToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { paymentToDelete = null }) {
                        Text("Cancel")
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
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        tonalElevation = 2.dp,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = name,
                color = if (isSelected) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PaymentCard(payment: PaymentEntity, workerName: String, siteName: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Site: $siteName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text("Date: ${Utils.formatDate(payment.date)}", style = MaterialTheme.typography.bodyMedium)
                Text("Type: ${payment.paymentType.name} | ${payment.method.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (payment.note.isNotEmpty()) {
                    Text("Note: ${payment.note}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Utils.formatCurrency(payment.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPaymentDialog(
    sites: List<SiteEntity>,
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
        title = { Text("Make Payment") },
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
                                text = { Text(site.name) },
                                onClick = { selectedSiteId = site.id; selectedWorkerId = null; siteExpanded = false }
                            )
                        }
                    }
                }

                Text("Site selection required", style = MaterialTheme.typography.labelSmall)

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

                Text("Payment Type", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaymentType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Method", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.name) }
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
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (selectedSiteId != null && amountVal > 0) {
                        // For simplicity, using siteId as a placeholder for workerId if logic is simplified
                        onConfirm(0L, selectedSiteId!!, amountVal, selectedMethod, selectedType, selectedDate, note)
                    }
                },
                enabled = selectedSiteId != null && amount.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.example.workermanager.ui.screens.contracts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.ClientPaymentEntity
import com.example.workermanager.data.entity.ClientPaymentMethod
import com.example.workermanager.data.entity.ContractStatus
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.ContractViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailsScreen(
    contractId: Long,
    viewModel: ContractViewModel,
    onBack: () -> Unit
) {
    val contractModel by viewModel.getContractDetails(contractId).collectAsState(initial = null)
    val payments by viewModel.getPaymentsForContract(contractId).collectAsState(initial = emptyList())
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<ClientPaymentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contract Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if ((contractModel?.remainingAmount ?: 0.0) > 0) {
                    showAddPaymentDialog = true 
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment")
            }
        }
    ) { padding ->
        if (contractModel == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val model = contractModel!!
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryCard(model)
                }
                
                item {
                    Text("Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                if (payments.isEmpty()) {
                    item {
                        Text("No payments received yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(payments) { payment ->
                        PaymentItem(
                            payment = payment,
                            onDelete = { paymentToDelete = payment }
                        )
                    }
                }
            }
        }

        if (showAddPaymentDialog) {
            AddClientPaymentDialog(
                remainingAmount = contractModel?.remainingAmount ?: 0.0,
                onDismiss = { showAddPaymentDialog = false },
                onConfirm = { amount, date, method, note ->
                    viewModel.addClientPayment(contractId, amount, date, method, note)
                    showAddPaymentDialog = false
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
                        viewModel.deleteClientPayment(paymentToDelete!!)
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
fun SummaryCard(model: com.example.workermanager.viewmodel.ContractUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(model.contract.clientName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Site: ${model.siteName}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Contract Amount", style = MaterialTheme.typography.labelSmall)
                    Text(Utils.formatCurrency(model.contract.totalAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                ContractStatusBadge(status = model.contract.status)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Received", style = MaterialTheme.typography.labelSmall)
                    Text(Utils.formatCurrency(model.totalReceived), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall)
                    Text(Utils.formatCurrency(model.remainingAmount), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val progress = if (model.contract.totalAmount > 0) (model.totalReceived / model.contract.totalAmount).toFloat() else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = Color(0xFF2E7D32),
                trackColor = Color(0xFF2E7D32).copy(alpha = 0.2f)
            )
            Text(
                "${(progress * 100).toInt()}% Received",
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun PaymentItem(payment: ClientPaymentEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(Utils.formatDate(payment.paymentDate), fontWeight = FontWeight.Bold)
                Text("${payment.method.name} ${if (payment.note.isNotEmpty()) "- ${payment.note}" else ""}", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Utils.formatCurrency(payment.amount), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientPaymentDialog(
    remainingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, LocalDate, ClientPaymentMethod, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(ClientPaymentMethod.CASH) }
    var methodExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Client Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remaining to receive: ${Utils.formatCurrency(remainingAmount)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        val input = it.toDoubleOrNull()
                        if (input == null || input <= remainingAmount) {
                            amount = it
                        }
                    },
                    label = { Text("Amount*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = !methodExpanded }) {
                    OutlinedTextField(
                        value = selectedMethod.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        ClientPaymentMethod.entries.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.name.replace("_", " ")) },
                                onClick = { selectedMethod = method; methodExpanded = false }
                            )
                        }
                    }
                }
                
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt, LocalDate.now(), selectedMethod, note)
                        onDismiss()
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ContractStatusBadge(status: ContractStatus) {
    Surface(
        color = if (status == ContractStatus.ACTIVE) Color(0xFFE3F2FD) else Color(0xFFE8F5E9),
        contentColor = if (status == ContractStatus.ACTIVE) Color(0xFF1565C0) else Color(0xFF2E7D32),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

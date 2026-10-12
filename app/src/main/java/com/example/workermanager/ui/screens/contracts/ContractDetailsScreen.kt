package com.example.workermanager.ui.screens.contracts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PermContactCalendar
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
import com.example.workermanager.data.entity.ClientPaymentEntity
import com.example.workermanager.data.entity.ClientPaymentMethod
import com.example.workermanager.util.ContactUtils
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
    val context = LocalContext.current
    val contractModel by viewModel.getContractDetails(contractId).collectAsState(initial = null)
    val payments by viewModel.getPaymentsForContract(contractId).collectAsState(initial = emptyList())
    val sites by viewModel.allSites.collectAsState(initial = emptyList())
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<ClientPaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<ClientPaymentEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contract Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (contractModel != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Contract")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Contract")
                        }
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
                onClick = { showAddPaymentDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Payment", fontWeight = FontWeight.Bold) }
            )
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
                            onEdit = { paymentToEdit = payment },
                            onDelete = { paymentToDelete = payment }
                        )
                    }
                }
            }
        }

        if (showEditDialog && contractModel != null) {
            EditContractDialog(
                contract = contractModel!!.contract,
                sites = sites,
                onDismiss = { showEditDialog = false },
                onConfirm = { name, mobile, siteId, amount, _, notes ->
                    viewModel.updateContract(
                        contractModel!!.contract.copy(
                            clientName = name,
                            mobile = mobile,
                            siteId = siteId,
                            totalAmount = amount,
                            notes = notes
                        )
                    )
                    showEditDialog = false
                }
            )
        }

        if (showDeleteDialog && contractModel != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Contract") },
                text = { Text("Are you sure you want to delete this contract? All related payments will be deleted too.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteContract(contractModel!!.contract)
                        showDeleteDialog = false
                        onBack()
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddPaymentDialog) {
            AddClientPaymentDialog(
                remainingAmount = contractModel?.remainingAmount ?: 0.0,
                onDismiss = { showAddPaymentDialog = false },
                onConfirm = { amount, date, method, note ->
                    viewModel.addClientPayment(
                        contractId = contractId,
                        amount = amount,
                        date = date,
                        method = method,
                        note = note,
                        onSuccess = {
                            showAddPaymentDialog = false
                            if (method == ClientPaymentMethod.UPI) {
                                com.example.workermanager.util.UpiUtils.launchUpiApp(context)
                            }
                        },
                        onError = { error ->
                            Toast.makeText(context, "Failed to save payment: ${error.localizedMessage ?: "Database error"}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        if (paymentToEdit != null) {
            EditClientPaymentDialog(
                payment = paymentToEdit!!,
                onDismiss = { paymentToEdit = null },
                onConfirm = { amount, method, note ->
                    viewModel.updateClientPayment(paymentToEdit!!, amount, method, note)
                    paymentToEdit = null
                    Toast.makeText(context, "Payment updated successfully", Toast.LENGTH_SHORT).show()
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
                if (model.contract.totalAmount > 0) "${(progress * 100).toInt()}% Received" else "No Payment",
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentItem(
    payment: ClientPaymentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { menuExpanded = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = Utils.formatDate(payment.paymentDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (payment.isEdited) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "Edited",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val methodText = payment.method.name.replace("_", " ")
                    val noteText = if (payment.note.isNotBlank()) " • ${payment.note}" else ""
                    Text(
                        text = "$methodText$noteText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = Utils.formatCurrency(payment.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Payment", fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Payment", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { menuExpanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientPaymentDialog(
    payment: ClientPaymentEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, ClientPaymentMethod, String) -> Unit
) {
    var amount by remember { mutableStateOf(payment.amount.toString()) }
    var note by remember { mutableStateOf(payment.note) }
    var selectedMethod by remember { mutableStateOf(payment.method) }
    var methodExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Client Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
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
                        onConfirm(amt, selectedMethod, note)
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Save Changes", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
                        amount = it
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
            var isSaving by remember { mutableStateOf(false) }
            val buttonText = if (selectedMethod == ClientPaymentMethod.UPI) "Save & Open UPI App" else "Save"
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && !isSaving) {
                        isSaving = true
                        onConfirm(amt, LocalDate.now(), selectedMethod, note)
                    }
                },
                enabled = !isSaving && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) { Text(buttonText, fontWeight = FontWeight.Bold) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContractDialog(
    contract: com.example.workermanager.data.entity.ContractEntity,
    sites: List<com.example.workermanager.data.entity.SiteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?, Double, LocalDate, String) -> Unit
) {
    var name by remember { mutableStateOf(contract.clientName) }
    var mobile by remember { mutableStateOf(contract.mobile) }
    var siteId by remember { mutableStateOf(contract.siteId) }
    var amount by remember { mutableStateOf(contract.totalAmount.toString()) }
    var notes by remember { mutableStateOf(contract.notes) }
    var siteExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                val (fetchedName, fetchedNumber) = ContactUtils.processContactUri(context, contactUri)

                if (!fetchedName.isNullOrBlank()) {
                    name = fetchedName.trim()
                }

                if (!fetchedNumber.isNullOrBlank()) {
                    val normalized = ContactUtils.normalizePhoneNumber(fetchedNumber)
                    if (normalized.isNotEmpty()) {
                        mobile = normalized
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Selected contact has no phone number.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Contract", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Name*") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                try {
                                    contactPickerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Log.e("WorkCraftContact", "Failed to launch contact picker with Phone URI, trying Contacts URI", e)
                                    try {
                                        val fallbackIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                        contactPickerLauncher.launch(fallbackIntent)
                                    } catch (ex: Exception) {
                                        Log.e("WorkCraftContact", "Failed to launch fallback contact picker", ex)
                                        Toast.makeText(context, "Unable to open contact picker.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PermContactCalendar,
                                contentDescription = "Select Contact",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = !siteExpanded }) {
                    OutlinedTextField(
                        value = sites.find { it.id == siteId }?.name ?: "Select Site",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { siteId = null; siteExpanded = false }
                        )
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name) },
                                onClick = { siteId = site.id; siteExpanded = false }
                            )
                        }
                    }
                }
                
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Total Contract Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, mobile, siteId, amount.toDoubleOrNull() ?: 0.0, contract.startDate, notes)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

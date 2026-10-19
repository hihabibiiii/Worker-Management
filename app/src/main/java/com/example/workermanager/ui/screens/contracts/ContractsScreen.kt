package com.example.workermanager.ui.screens.contracts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Search
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
import com.example.workermanager.data.entity.ContractStatus
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.util.ContactUtils
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.ContractUiModel
import com.example.workermanager.viewmodel.ContractUiState
import com.example.workermanager.viewmodel.ContractViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsScreen(
    viewModel: ContractViewModel,
    sites: List<SiteEntity>,
    onBack: () -> Unit = {},
    onContractClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Contracts",
                screenIcon = Icons.AutoMirrored.Filled.Assignment,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Contract", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search and Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search client name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            ScrollableTabRow(
                selectedTabIndex = when (statusFilter) {
                    null -> 0
                    ContractStatus.ACTIVE -> 1
                    ContractStatus.COMPLETED -> 2
                },
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(selected = statusFilter == null, onClick = { viewModel.updateStatusFilter(null) }) {
                    Text("All", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = statusFilter == ContractStatus.ACTIVE, onClick = { viewModel.updateStatusFilter(ContractStatus.ACTIVE) }) {
                    Text("Active", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = statusFilter == ContractStatus.COMPLETED, onClick = { viewModel.updateStatusFilter(ContractStatus.COMPLETED) }) {
                    Text("Completed", modifier = Modifier.padding(16.dp))
                }
            }

            when (val state = uiState) {
                is ContractUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ContractUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No contracts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is ContractUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.contracts) { model ->
                            ContractCard(model = model, onClick = { onContractClick(model.contract.id) })
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddContractDialog(
                sites = sites,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, mobile, siteId, amount, date, notes ->
                    viewModel.addContract(name, mobile, siteId, amount, date, notes)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ContractCard(model: ContractUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.contract.clientName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Site: ${model.siteName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                StatusBadge(status = model.contract.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Utils.formatCurrency(model.contract.totalAmount), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Received", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Utils.formatCurrency(model.totalReceived), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = if (model.contract.totalAmount > 0) (model.totalReceived / model.contract.totalAmount).toFloat() else 0f,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Remaining: ${Utils.formatCurrency(model.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                val percentage = if (model.contract.totalAmount > 0) {
                    ((model.totalReceived / model.contract.totalAmount) * 100).toInt()
                } else 0
                Text(
                    if (model.contract.totalAmount > 0) "$percentage% Received" else "No Payment",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: ContractStatus) {
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
fun AddContractDialog(
    sites: List<SiteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?, Double, LocalDate, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
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
        title = { Text("Add New Contract", fontWeight = FontWeight.Bold) },
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
                        // Allow deselecting site
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
                        onConfirm(name, mobile, siteId, amount.toDoubleOrNull() ?: 0.0, LocalDate.now(), notes)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

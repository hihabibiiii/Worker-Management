package com.example.workermanager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkersScreen(
    viewModel: WorkerViewModel,
    siteViewModel: SiteViewModel,
    onWorkerClick: (Long) -> Unit
) {
    val workers by viewModel.workers.collectAsState()
    val sites by siteViewModel.allSites.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var workerToEdit by remember { mutableStateOf<WorkerEntity?>(null) }
    var workerToDelete by remember { mutableStateOf<WorkerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workers", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Worker")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name or phone") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (workers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No workers added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(workers) { worker ->
                        val stats by viewModel.getWorkerStats(worker.id).collectAsState(initial = null)
                        val currentSiteName = sites.find { it.id == worker.currentSiteId }?.name ?: "No Site"
                        
                        WorkerCard(
                            worker = worker,
                            siteName = currentSiteName,
                            pendingBalance = stats?.pendingBalance ?: 0.0,
                            onClick = { onWorkerClick(worker.id) },
                            onEdit = { workerToEdit = worker },
                            onDelete = { workerToDelete = worker }
                        )
                    }
                }
            }
        }

        if (showAddDialog || workerToEdit != null) {
            WorkerDialog(
                worker = workerToEdit,
                sites = sites,
                onDismiss = {
                    showAddDialog = false
                    workerToEdit = null
                },
                onConfirm = { name, phone, type, dailyRate, otRate, date, siteId, notes, active ->
                    if (workerToEdit == null) {
                        viewModel.addWorker(name, phone, type, dailyRate, otRate, date, siteId, notes)
                    } else {
                        viewModel.updateWorker(
                            workerToEdit!!.copy(
                                name = name,
                                phone = phone,
                                workerType = type,
                                dailyRate = dailyRate,
                                overtimeRate = otRate,
                                joiningDate = date,
                                currentSiteId = siteId ?: workerToEdit!!.currentSiteId,
                                notes = notes,
                                isActive = active
                            )
                        )
                    }
                    showAddDialog = false
                    workerToEdit = null
                }
            )
        }

        if (workerToDelete != null) {
            AlertDialog(
                onDismissRequest = { workerToDelete = null },
                title = { Text("Delete Worker", fontWeight = FontWeight.Bold) },
                text = { Text("Delete this worker and all related attendance/payment records? This cannot be undone.", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteWorker(workerToDelete!!)
                        workerToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { workerToDelete = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}

@Composable
fun WorkerCard(
    worker: WorkerEntity,
    siteName: String,
    pendingBalance: Double,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    worker.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        worker.workerType.name, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        siteName, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Balance: ${Utils.formatCurrency(pendingBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Edit", 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete", 
                        tint = Color(0xFFC62828), 
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDialog(
    worker: WorkerEntity?,
    sites: List<SiteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, WorkerType, Double, Double, LocalDate, Long?, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(worker?.name ?: "") }
    var phone by remember { mutableStateOf(worker?.phone ?: "") }
    var type by remember { mutableStateOf(worker?.workerType ?: WorkerType.LABOUR) }
    var dailyRate by remember { mutableStateOf(worker?.dailyRate?.toString() ?: "") }
    var otRate by remember { mutableStateOf(worker?.overtimeRate?.toString() ?: "") }
    var joiningDate by remember { mutableStateOf(worker?.joiningDate ?: LocalDate.now()) }
    var siteId by remember { mutableStateOf(worker?.currentSiteId) }
    var notes by remember { mutableStateOf(worker?.notes ?: "") }
    var isActive by remember { mutableStateOf(worker?.isActive ?: true) }

    var typeExpanded by remember { mutableStateOf(false) }
    var siteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (worker == null) "Add Worker" else "Edit Worker", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name*") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Worker Type Dropdown
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Worker Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        WorkerType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name, fontWeight = FontWeight.Medium) }, 
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dailyRate, 
                        onValueChange = { dailyRate = it }, 
                        label = { Text("Daily Rate*") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = otRate, 
                        onValueChange = { otRate = it }, 
                        label = { Text("OT Rate/Hr") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                        modifier = Modifier.weight(1f)
                    )
                }

                // Site selection (only for new workers or showing current)
                ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = !siteExpanded }) {
                    OutlinedTextField(
                        value = sites.find { it.id == siteId }?.name ?: "Select Site",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name, fontWeight = FontWeight.Medium) }, 
                                onClick = { siteId = site.id; siteExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dRate = dailyRate.toDoubleOrNull() ?: 0.0
                    val oRate = otRate.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, phone, type, dRate, oRate, joiningDate, siteId, notes, isActive)
                    }
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

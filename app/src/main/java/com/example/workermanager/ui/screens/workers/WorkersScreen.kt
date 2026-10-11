package com.example.workermanager.ui.screens.workers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MoreVert
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
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Worker", fontWeight = FontWeight.Bold) }
            )
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
                onAddNewSite = { siteName, siteAddress, siteManager, onCreated ->
                    siteViewModel.addSite(name = siteName, address = siteAddress, manager = siteManager) { newId ->
                        onCreated(newId)
                    }
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
                title = { Text("Delete Worker?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this worker?", color = MaterialTheme.colorScheme.onSurface) },
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
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
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
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert, 
                        contentDescription = "More options", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
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
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFC62828)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDialog(
    worker: WorkerEntity?,
    sites: List<SiteEntity>,
    initialName: String = "",
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onAddNewSite: (String, String, String, (Long) -> Unit) -> Unit,
    onConfirm: (String, String, WorkerType, Double, Double, LocalDate, Long?, String, Boolean) -> Unit
) {
    var name by remember(worker, initialName) { mutableStateOf(worker?.name ?: initialName) }
    var phone by remember(worker, initialPhone) { mutableStateOf(worker?.phone ?: initialPhone) }
    var type by remember { mutableStateOf(worker?.workerType ?: WorkerType.LABOUR) }
    var dailyRate by remember { mutableStateOf(worker?.dailyRate?.toString() ?: "") }
    var otRate by remember { mutableStateOf(worker?.overtimeRate?.toString() ?: "") }
    var joiningDate by remember { mutableStateOf(worker?.joiningDate ?: LocalDate.now()) }
    var siteId by remember { mutableStateOf(worker?.currentSiteId) }
    var notes by remember { mutableStateOf(worker?.notes ?: "") }
    var isActive by remember { mutableStateOf(worker?.isActive ?: true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var siteError by remember { mutableStateOf<String?>(null) }

    var typeExpanded by remember { mutableStateOf(false) }
    var siteExpanded by remember { mutableStateOf(false) }

    var showAddSiteDialog by remember { mutableStateOf(false) }
    var newSiteName by remember { mutableStateOf("") }
    var newSiteAddress by remember { mutableStateOf("") }
    var newSiteManager by remember { mutableStateOf("") }
    var newSiteError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                val (fetchedName, fetchedNumber) = processContactUri(context, contactUri)

                if (!fetchedName.isNullOrBlank()) {
                    name = fetchedName.trim()
                    nameError = null
                }

                if (!fetchedNumber.isNullOrBlank()) {
                    val normalized = normalizePhoneNumber(fetchedNumber)
                    if (normalized.isNotEmpty()) {
                        phone = normalized
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

    val displaySites = remember(sites) {
        val selfWork = sites.find { it.name.equals("Self Work", ignoreCase = true) }
        val others = sites.filter { !it.name.equals("Self Work", ignoreCase = true) }
        if (selfWork != null) listOf(selfWork) + others else others
    }

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
                    onValueChange = { 
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Name") },
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
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
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
                        label = { Text("Daily Rate") },
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

                // Site selection
                ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = !siteExpanded }) {
                    OutlinedTextField(
                        value = sites.find { it.id == siteId }?.name ?: "Select Site",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        isError = siteError != null,
                        supportingText = siteError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        displaySites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name, fontWeight = FontWeight.Medium) }, 
                                onClick = { 
                                    siteId = site.id
                                    siteError = null
                                    siteExpanded = false 
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Add New Site",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                siteExpanded = false
                                newSiteName = ""
                                newSiteAddress = ""
                                newSiteManager = ""
                                newSiteError = null
                                showAddSiteDialog = true
                            }
                        )
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
                    var hasError = false

                    if (name.isBlank()) {
                        nameError = "Name is required"
                        hasError = true
                    } else {
                        nameError = null
                    }

                    if (sites.isNotEmpty() && siteId == null) {
                        siteError = "Please select a site"
                        hasError = true
                    } else {
                        siteError = null
                    }

                    if (!hasError) {
                        onConfirm(name, phone, type, dRate, oRate, joiningDate, siteId, notes, isActive)
                    }
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )

    if (showAddSiteDialog) {
        AlertDialog(
            onDismissRequest = { showAddSiteDialog = false },
            title = { Text("Add New Site", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSiteName,
                        onValueChange = { 
                            newSiteName = it
                            if (it.isNotBlank()) newSiteError = null
                        },
                        label = { Text("Site Name") },
                        singleLine = true,
                        isError = newSiteError != null,
                        supportingText = newSiteError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSiteAddress,
                        onValueChange = { newSiteAddress = it },
                        label = { Text("Site Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSiteManager,
                        onValueChange = { newSiteManager = it },
                        label = { Text("Site Manager") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = newSiteName.trim()
                        val trimmedAddress = newSiteAddress.trim()
                        val trimmedManager = newSiteManager.trim()

                        if (trimmedName.isEmpty()) {
                            newSiteError = "Site name cannot be empty"
                        } else if (sites.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                            newSiteError = "A site with this name already exists"
                        } else {
                            onAddNewSite(trimmedName, trimmedAddress, trimmedManager) { createdSiteId ->
                                siteId = createdSiteId
                                siteError = null
                                showAddSiteDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save Site", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSiteDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

private fun processContactUri(context: Context, contactUri: Uri): Pair<String?, String?> {
    var fetchedName: String? = null
    var fetchedNumber: String? = null

    try {
        // Attempt 1: Query as CommonDataKinds.Phone URI
        val phoneCursor = context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        phoneCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                if (nameIdx >= 0) fetchedName = cursor.getString(nameIdx)
                if (numIdx >= 0) fetchedNumber = cursor.getString(numIdx)
            }
        }

        // Attempt 2: If phone number was not retrieved, query Contacts table & Phone table
        if (fetchedNumber.isNullOrBlank()) {
            val contactCursor = context.contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null,
                null,
                null
            )

            contactCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    if (fetchedName.isNullOrBlank() && nameIdx >= 0) {
                        fetchedName = cursor.getString(nameIdx)
                    }

                    val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                    val hasPhone = if (hasPhoneIdx >= 0) cursor.getInt(hasPhoneIdx) else 0

                    if (hasPhone > 0 && contactId != null) {
                        val pCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )
                        pCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val numIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numIdx >= 0) {
                                    fetchedNumber = pc.getString(numIdx)
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("WorkCraftContact", "Error reading contact details for URI: $contactUri", e)
    }

    return Pair(fetchedName, fetchedNumber)
}

private fun normalizePhoneNumber(rawNumber: String): String {
    val trimmed = rawNumber.trim()
    if (trimmed.isEmpty()) return ""

    val hasPlus = trimmed.startsWith("+")
    val digitsOnly = trimmed.filter { it.isDigit() }
    
    return if (hasPlus) "+$digitsOnly" else digitsOnly
}

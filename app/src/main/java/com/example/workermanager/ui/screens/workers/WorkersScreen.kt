package com.example.workermanager.ui.screens.workers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerType
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerViewModel
import java.io.File
import java.time.LocalDate

private const val NONE_SITE_ID = -1L

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
    val selectedSiteFilterId by viewModel.selectedSiteFilterId.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var workerToEdit by remember { mutableStateOf<WorkerEntity?>(null) }
    var workerToDelete by remember { mutableStateOf<WorkerEntity?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF1F6FD)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF17243A)
    val textSecondary = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

    Scaffold(
        topBar = {
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Workers",
                screenIcon = Icons.Default.Groups
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1677F8),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Worker", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(28.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.height(42.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(screenBg)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 1: SEARCH BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = BorderStroke(1.dp, Color(0xFF1677F8).copy(alpha = 0.2f)),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF536789),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF26344A)),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search by name or phone", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF536789))
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF536789), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SECTION 2: HORIZONTAL SITE FILTER CARDS
            if (sites.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val isAllSelected = selectedSiteFilterId == null
                        val totalWorkersCount = workers.size
                        WorkerSiteFilterCard(
                            title = "All Sites",
                            workerCount = totalWorkersCount,
                            isSelected = isAllSelected,
                            onClick = { viewModel.setSelectedSiteFilter(null) },
                            isDark = isDark
                        )
                    }
                    items(sites) { site ->
                        val isSiteSelected = selectedSiteFilterId == site.id
                        val siteWorkersCount = workers.count { it.currentSiteId == site.id }
                        WorkerSiteFilterCard(
                            title = site.name,
                            workerCount = siteWorkersCount,
                            isSelected = isSiteSelected,
                            onClick = { viewModel.setSelectedSiteFilter(site.id) },
                            isDark = isDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 3: WORKER LIST CARDS
            if (workers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedSiteFilterId != null) "No workers assigned to this site." else "No workers added yet.",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedSiteFilterId != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.setSelectedSiteFilter(null) }) {
                                Text("Show All Workers", fontWeight = FontWeight.Bold, color = Color(0xFF1677F8))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
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
                            onDelete = { workerToDelete = worker },
                            isDark = isDark,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }
        }

        // Add / Edit Worker Dialog
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

        // Delete Confirmation Dialog
        if (workerToDelete != null) {
            AlertDialog(
                onDismissRequest = { workerToDelete = null },
                title = { Text("Delete Worker?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this worker?", color = textPrimary) },
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
                        Text("Cancel", color = textPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}

@Composable
fun WorkerSiteFilterCard(
    title: String,
    workerCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1677F8) else (if (isDark) MaterialTheme.colorScheme.surface else Color.White),
        contentColor = if (isSelected) Color.White else (if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF26344A)),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF1677F8) else (if (isDark) Color(0xFF333A45) else Color(0xFFCBD5E1))),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier.height(43.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF1677F8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val countText = if (workerCount == 1) "1 Worker" else "$workerCount Workers"
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun WorkerAvatar(
    photoPath: String?,
    name: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val bitmap = remember(photoPath) {
        if (!photoPath.isNullOrBlank()) {
            val file = File(photoPath)
            if (file.exists() && file.length() > 0) {
                try {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(photoPath, options)
                    var sampleSize = 1
                    while (options.outWidth / sampleSize > 300 || options.outHeight / sampleSize > 300) {
                        sampleSize *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    BitmapFactory.decodeFile(photoPath, decodeOptions)
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    Surface(
        modifier = modifier.size(42.dp),
        shape = CircleShape,
        color = Color(0xFFDFEBFF),
        contentColor = Color(0xFF0965E8),
        border = BorderStroke(1.dp, Color(0xFF0965E8).copy(alpha = 0.2f))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.trim().take(1).uppercase(),
                    style = textStyle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
    onDelete: () -> Unit,
    isDark: Boolean = false,
    cardBg: Color = Color.White,
    textPrimary: Color = Color(0xFF17243A),
    textSecondary: Color = Color(0xFF64748B)
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333A45) else Color(0xFF0965E8).copy(alpha = 0.15f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // TOP ROW: Avatar, Name, Type, Site & Three-Dot Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    WorkerAvatar(
                        photoPath = worker.photoPath,
                        name = worker.name
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Construction,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = worker.workerType.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = textSecondary
                            )

                            Text("•", style = MaterialTheme.typography.bodySmall, color = textSecondary, fontSize = 10.sp)

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF0866E8),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = siteName,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0866E8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Three-Dot Options Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF536789)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF0866E8), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Worker") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0866E8), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Payment History") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF0866E8), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Worker", color = Color(0xFFC62828)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BOTTOM ROW: CLEAN BALANCE BAR (TRANSPARENT BACKGROUND, NO SEPARATE COLORED FILL)
            val (balColor, balText) = when {
                pendingBalance > 0.001 -> Pair(
                    if (isDark) Color(0xFFFF5252) else Color(0xFFD90016),
                    "${Utils.formatCurrency(pendingBalance)} Due"
                )
                pendingBalance < -0.001 -> Pair(
                    if (isDark) Color(0xFF2ECC71) else Color(0xFF078B35),
                    "${Utils.formatCurrency(-pendingBalance)} Advance"
                )
                else -> Pair(
                    textSecondary,
                    "₹0.00"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = balColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Balance",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = balText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = balColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
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
    var dailyRate by remember { mutableStateOf(worker?.dailyRate?.let { if (it > 0) it.toString() else "" } ?: "") }
    var otRate by remember { mutableStateOf(worker?.overtimeRate?.let { if (it > 0) it.toString() else "" } ?: "") }
    var joiningDate by remember { mutableStateOf(worker?.joiningDate ?: LocalDate.now()) }
    var siteId by remember { mutableStateOf(worker?.currentSiteId) }
    var notes by remember { mutableStateOf(worker?.notes ?: "") }
    var isActive by remember { mutableStateOf(worker?.isActive ?: true) }

    var nameError by remember { mutableStateOf<String?>(null) }

    var typeExpanded by remember { mutableStateOf(false) }
    var siteExpanded by remember { mutableStateOf(false) }

    var showAddSiteDialog by remember { mutableStateOf(false) }
    var newSiteName by remember { mutableStateOf("") }
    var newSiteAddress by remember { mutableStateOf("") }
    var newSiteManager by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dialogBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFE6F2FF)
    val fieldContainerBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val titleTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF101426)
    val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF101426)
    val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF596580)
    val primaryColor = Color(0xFF1976ED)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fieldContainerBg,
        unfocusedContainerColor = fieldContainerBg,
        disabledContainerColor = fieldContainerBg,
        errorContainerColor = fieldContainerBg,
        focusedTextColor = primaryTextColor,
        unfocusedTextColor = primaryTextColor,
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = secondaryTextColor,
        focusedLeadingIconColor = primaryColor,
        unfocusedLeadingIconColor = primaryColor,
        focusedTrailingIconColor = primaryColor,
        unfocusedTrailingIconColor = primaryColor,
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFCBD5E1)
    )

    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                val (fetchedName, fetchedNumber) = com.example.workermanager.util.ContactUtils.processContactUri(context, contactUri)
                if (!fetchedName.isNullOrBlank()) {
                    name = fetchedName
                    nameError = null
                }
                if (!fetchedNumber.isNullOrBlank()) {
                    phone = com.example.workermanager.util.ContactUtils.normalizePhoneNumber(fetchedNumber)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Text(
                    text = if (worker == null) "Add Worker" else "Edit Worker",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleTextColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Worker Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("Worker Name *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                    try {
                                        contactPickerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val fallbackIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                            contactPickerLauncher.launch(fallbackIntent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Unable to open contact picker.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PermContactCalendar,
                                    contentDescription = "Select Contact",
                                    tint = primaryColor
                                )
                            }
                        },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it) } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Phone Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Worker Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Worker Type *") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier = Modifier.background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                        ) {
                            WorkerType.entries.forEach { wType ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = wType.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontWeight = FontWeight.Medium,
                                            color = primaryTextColor
                                        )
                                    },
                                    onClick = {
                                        type = wType
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. Daily Rate & OT Rate/Hr (Side-by-side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Daily Wage Rate
                        OutlinedTextField(
                            value = dailyRate,
                            onValueChange = { dailyRate = it },
                            label = { Text("Daily Rate (₹)*") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f)
                        )

                        // OT Rate/Hr
                        OutlinedTextField(
                            value = otRate,
                            onValueChange = { otRate = it },
                            label = { Text("OT Rate/Hr (₹)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 5. Assign to Site Dropdown
                    ExposedDropdownMenuBox(
                        expanded = siteExpanded,
                        onExpandedChange = { siteExpanded = !siteExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentSiteText = sites.find { it.id == siteId }?.name ?: "Assign to Site"
                        OutlinedTextField(
                            value = currentSiteText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign to Site") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = siteExpanded,
                            onDismissRequest = { siteExpanded = false },
                            modifier = Modifier.background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "No Site Assigned",
                                        fontWeight = FontWeight.Normal,
                                        color = primaryTextColor
                                    )
                                },
                                onClick = {
                                    siteId = null
                                    siteExpanded = false
                                }
                            )
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = site.name,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryTextColor
                                        )
                                    },
                                    onClick = {
                                        siteId = site.id
                                        siteExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Add New Site", color = primaryColor, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    siteExpanded = false
                                    showAddSiteDialog = true
                                }
                            )
                        }
                    }

                    // 6. Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 7. Active Status Checkbox
                    Surface(
                        color = fieldContainerBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isActive = !isActive }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isActive,
                                onCheckedChange = { isActive = it },
                                colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active Worker",
                                fontWeight = FontWeight.SemiBold,
                                color = titleTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons (Bottom Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontWeight = FontWeight.Bold, color = secondaryTextColor)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = "Worker name is required"
                                return@Button
                            }
                            val dRate = dailyRate.toDoubleOrNull() ?: 0.0
                            val oRate = otRate.toDoubleOrNull() ?: (dRate / 8.0)
                            onConfirm(name.trim(), phone.trim(), type, dRate, oRate, joiningDate, siteId, notes.trim(), isActive)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text(if (worker == null) "Add Worker" else "Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Add New Site Quick Dialog
    if (showAddSiteDialog) {
        AlertDialog(
            onDismissRequest = { showAddSiteDialog = false },
            title = { Text("Add New Site", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSiteName,
                        onValueChange = { newSiteName = it },
                        label = { Text("Site Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSiteAddress,
                        onValueChange = { newSiteAddress = it },
                        label = { Text("Location / Address") },
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
                        if (newSiteName.isNotBlank()) {
                            onAddNewSite(newSiteName.trim(), newSiteAddress.trim(), newSiteManager.trim()) { createdId ->
                                siteId = createdId
                            }
                            newSiteName = ""
                            newSiteAddress = ""
                            newSiteManager = ""
                            showAddSiteDialog = false
                        }
                    },
                    enabled = newSiteName.isNotBlank()
                ) {
                    Text("Add Site", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSiteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

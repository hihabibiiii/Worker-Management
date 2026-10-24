package com.example.workermanager.ui.screens.payments

import android.app.DatePickerDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<PaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF1F6FD)

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
                containerColor = Color(0xFF0875F5),
                contentColor = Color.White,
                shape = CircleShape,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Add Payment", fontWeight = FontWeight.Bold, color = Color.White) },
                modifier = Modifier.padding(bottom = 80.dp)
            )
        },
        containerColor = screenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(screenBg)
        ) {
            // Search Bar & Site Filter Chips Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                PaymentSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        SiteFilterChip(
                            name = "All Sites",
                            isSelected = selectedSiteId == null,
                            onClick = { viewModel.updateSelectedSite(null) }
                        )
                    }
                    items(sites) { site ->
                        SiteFilterChip(
                            name = site.name,
                            isSelected = selectedSiteId == site.id,
                            onClick = { viewModel.updateSelectedSite(site.id) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is PaymentsUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF0875F5)
                        )
                    }
                    is PaymentsUiState.Empty -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFF0875F5).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF0875F5),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "No payments recorded for this selection.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF526581)
                            )
                        }
                    }
                    is PaymentsUiState.Success -> {
                        val filteredPayments = remember(state.payments, searchQuery) {
                            if (searchQuery.isBlank()) {
                                state.payments
                            } else {
                                val q = searchQuery.trim().lowercase()
                                state.payments.filter { model ->
                                    model.workerName.lowercase().contains(q) ||
                                    model.siteName.lowercase().contains(q) ||
                                    model.payment.paymentType.name.lowercase().contains(q) ||
                                    model.payment.method.name.lowercase().contains(q) ||
                                    model.payment.note.lowercase().contains(q)
                                }
                            }
                        }

                        if (filteredPayments.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFF0875F5).copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color(0xFF0875F5),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No payments found." else "No payments recorded for this selection.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF526581)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = filteredPayments,
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
fun PaymentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val border = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else Color(0xFFD9E3F2)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF0875F5),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Search payments...",
                        fontSize = 14.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF526581)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF172554),
                    unfocusedTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF172554)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF526581),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SiteFilterChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = if (isSelected) Color(0xFF0875F5) else if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isSelected) Color.White else if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF172554)
    val iconColor = if (isSelected) Color.White else if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF526581)
    val border = if (isSelected) null else BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else Color(0xFFD9E3F2))

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = border,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = name,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else Color(0xFFD9E3F2))
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF172554)
    val secondaryText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF526581)

    val (typeBgColor, typeIconColor, typeIcon) = when (payment.paymentType) {
        PaymentType.PAYMENT, PaymentType.SALARY -> Triple(
            if (isDark) Color(0xFF1A2C42) else Color(0xFFE2EDFF),
            Color(0xFF0875F5),
            Icons.Default.AccountBalanceWallet
        )
        PaymentType.ADVANCE -> Triple(
            if (isDark) Color(0xFF123320) else Color(0xFFE3F7EB),
            Color(0xFF0A963D),
            Icons.Default.Payments
        )
        PaymentType.BONUS -> Triple(
            if (isDark) Color(0xFF2E1E42) else Color(0xFFF2E8FF),
            Color(0xFF8B36E8),
            Icons.Default.CardGiftcard
        )
        else -> Triple(
            if (isDark) Color(0xFF252A36) else Color(0xFFF1F5FB),
            Color(0xFF526581),
            Icons.Default.Payments
        )
    }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true }
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Type Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(typeBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = payment.paymentType.name,
                        tint = typeIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = workerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Site Name with Location Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Site",
                            tint = Color(0xFF0875F5),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = siteName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0875F5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Date",
                            tint = secondaryText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = Utils.formatDate(payment.date),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryText
                        )
                    }

                    // Metadata Pill Row
                    Surface(
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5FB),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = payment.paymentType.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeIconColor
                            )
                            Text(
                                text = "·",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText
                            )
                            Text(
                                text = payment.method.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryText
                            )
                        }
                    }

                    if (payment.note.isNotEmpty()) {
                        Text(
                            text = "Note: ${payment.note}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Amount Badge
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = if (isDark) Color(0xFF0F3818) else Color(0xFFE4F9ED),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = Utils.formatCurrency(payment.amount),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4CAF50) else Color(0xFF0A963D),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Options",
                        tint = secondaryText,
                        modifier = Modifier.size(16.dp)
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
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Make Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
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
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Edit Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
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

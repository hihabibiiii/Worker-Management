package com.example.workermanager.ui.screens.workers

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.workermanager.data.entity.*
import com.example.workermanager.util.PdfReportGenerator
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.*
import java.net.URLEncoder
import java.time.LocalDate

enum class WorkerHistoryTab(val title: String) {
    SITE("Site History"),
    PAYMENT("Payment History"),
    ATTENDANCE("Attendance History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailsScreen(
    workerId: Long,
    viewModel: WorkerViewModel,
    attendanceViewModel: AttendanceViewModel,
    paymentViewModel: PaymentViewModel,
    siteViewModel: SiteViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val worker by viewModel.getWorkerById(workerId).collectAsState(initial = null)
    val stats by viewModel.getWorkerStats(workerId).collectAsState(initial = WorkerStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0))
    val attendanceHistory by attendanceViewModel.getAttendanceByWorker(workerId).collectAsState(initial = emptyList())
    val paymentHistory by paymentViewModel.getPaymentsByWorker(workerId).collectAsState(initial = emptyList())
    val siteHistory by viewModel.getWorkerSiteHistory(workerId).collectAsState(initial = emptyList())
    val sites by siteViewModel.allSites.collectAsState()

    var showTransferDialog by remember { mutableStateOf(false) }
    var paymentDialogType by remember { mutableStateOf<PaymentType?>(null) }
    var showShareReportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(WorkerHistoryTab.ATTENDANCE) }

    var paymentToEdit by remember { mutableStateOf<PaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(worker?.name ?: "Worker Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (worker == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WorkerProfileHeader(worker!!, sites)
                }
                
                item {
                    WorkerStatsSection(
                        stats = stats,
                        onAdvanceClick = { paymentDialogType = PaymentType.ADVANCE },
                        onPaymentClick = { paymentDialogType = PaymentType.PAYMENT },
                        onWhatsAppReportClick = { showShareReportDialog = true }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkerHistoryTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                onClick = { selectedTab = tab },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = if (isSelected) 4.dp else 1.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = tab.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTab == WorkerHistoryTab.SITE) {
                    if (siteHistory.isEmpty()) {
                        item {
                            Text(
                                "No site history recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(siteHistory) { history ->
                            val siteName = sites.find { it.id == history.siteId }?.name ?: "Unknown Site"
                            SiteHistoryItem(history, siteName)
                        }
                    }
                }

                if (selectedTab == WorkerHistoryTab.ATTENDANCE) {
                    if (attendanceHistory.isEmpty()) {
                        item {
                            Text(
                                "No attendance history recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(attendanceHistory) { attendance ->
                            val siteName = sites.find { it.id == attendance.siteId }?.name ?: "Unknown"
                            AttendanceHistoryItem(attendance, siteName)
                        }
                    }
                }

                if (selectedTab == WorkerHistoryTab.PAYMENT) {
                    if (paymentHistory.isEmpty()) {
                        item {
                            Text(
                                "No payment history recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(paymentHistory) { payment ->
                            val siteName = sites.find { it.id == payment.siteId }?.name ?: "Unknown"
                            PaymentHistoryItem(
                                payment = payment,
                                siteName = siteName,
                                onEdit = { paymentToEdit = payment },
                                onDelete = { paymentToDelete = payment }
                            )
                        }
                    }
                }
            }
        }

        if (showTransferDialog && worker != null) {
            TransferWorkerDialog(
                worker = worker!!,
                sites = sites,
                onDismiss = { showTransferDialog = false },
                onConfirm = { newSiteId, date ->
                    viewModel.transferWorker(workerId, newSiteId, date)
                    showTransferDialog = false
                }
            )
        }

        if (paymentDialogType != null && worker != null) {
            RecordWorkerPaymentDialog(
                worker = worker!!,
                sites = sites,
                initialType = paymentDialogType!!,
                onDismiss = { paymentDialogType = null },
                onConfirm = { siteId, amount, method, type, date, note ->
                    paymentViewModel.addPayment(
                        workerId = worker!!.id,
                        siteId = siteId,
                        amount = amount,
                        method = method,
                        type = type,
                        date = date,
                        note = note,
                        onSuccess = {
                            paymentDialogType = null
                            if (method == PaymentMethod.UPI) {
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
            com.example.workermanager.ui.screens.payments.EditPaymentDialog(
                payment = paymentToEdit!!,
                sites = sites,
                onDismiss = { paymentToEdit = null },
                onConfirm = { updatedPayment ->
                    paymentViewModel.updatePayment(
                        updatedPayment,
                        onSuccess = {
                            paymentToEdit = null
                            if (updatedPayment.method == PaymentMethod.UPI) {
                                com.example.workermanager.util.UpiUtils.launchUpiApp(context)
                            }
                        },
                        onError = { error ->
                            Toast.makeText(context, "Failed to update payment: ${error.localizedMessage ?: "Database error"}", Toast.LENGTH_LONG).show()
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
                        paymentViewModel.deletePayment(paymentToDelete!!)
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

        if (showShareReportDialog && worker != null) {
            ShareWorkerReportDialog(
                worker = worker!!,
                stats = stats,
                attendanceHistory = attendanceHistory,
                paymentHistory = paymentHistory,
                sites = sites,
                onDismiss = { showShareReportDialog = false }
            )
        }
    }
}

@Composable
fun WorkerProfileHeader(worker: WorkerEntity, sites: List<SiteEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        worker.name, 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        worker.workerType.name, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                StatusChip(isActive = worker.isActive)
            }
            Spacer(modifier = Modifier.height(16.dp))
            DetailRow("Phone", worker.phone.ifEmpty { "N/A" })
            DetailRow("Daily Rate", Utils.formatCurrency(worker.dailyRate))
            DetailRow("OT Rate / Hr", Utils.formatCurrency(worker.overtimeRate))
            
            val currentSiteName = sites.find { it.id == worker.currentSiteId }?.name ?: "Not Assigned"
            DetailRow("Current Site", currentSiteName)
        }
    }
}

@Composable
fun StatusChip(isActive: Boolean) {
    val color = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = if (isActive) "Active" else "Inactive",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun WorkerStatsSection(
    stats: WorkerStats,
    onAdvanceClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onWhatsAppReportClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatItem("Present", stats.presentDays.toString(), Modifier.weight(1f))
            StatItem("Half Day", stats.halfDays.toString(), Modifier.weight(1f))
            StatItem("Absent", stats.absentDays.toString(), Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryStatRow("Total Earned", Utils.formatCurrency(stats.totalEarned))
                SummaryStatRow("Total Paid", Utils.formatCurrency(stats.totalPaid))
                SummaryStatRow("Total Advance", Utils.formatCurrency(stats.totalAdvance))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                SummaryStatRow(
                    label = "Pending Balance",
                    value = Utils.formatCurrency(stats.pendingBalance),
                    isBold = true,
                    valueColor = if (stats.pendingBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
            }
        }

        // Action Buttons (Advance, Payment, WhatsApp - Positioned directly above History Buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAdvanceClick,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Advance", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
            Button(
                onClick = onPaymentClick,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Payment", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
            Button(
                onClick = onWhatsAppReportClick,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                value, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SummaryStatRow(label: String, value: String, isBold: Boolean = false, valueColor: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label, 
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun SiteHistoryItem(history: WorkerSiteHistoryEntity, siteName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                siteName, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val period = if (history.endDate == null) {
                "${Utils.formatDate(history.startDate)} → Present"
            } else {
                "${Utils.formatDate(history.startDate)} → ${Utils.formatDate(history.endDate)}"
            }
            Text(
                period, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AttendanceHistoryItem(attendance: AttendanceEntity, siteName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    Utils.formatDate(attendance.date), 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    siteName, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    attendance.status.name, 
                    color = getStatusColor(attendance.status), 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 12.sp
                )
                Text(
                    Utils.formatCurrency(attendance.amount), 
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentHistoryItem(
    payment: PaymentEntity,
    siteName: String,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true }
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        Utils.formatDate(payment.date), 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${payment.paymentType.name} - $siteName", 
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
                Text(
                    Utils.formatCurrency(payment.amount), 
                    fontWeight = FontWeight.ExtraBold, 
                    color = Color(0xFF2E7D32)
                )
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

fun getStatusColor(status: AttendanceStatus): Color {
    return when (status) {
        AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
        AttendanceStatus.HALF_DAY -> Color(0xFFEF6C00)
        AttendanceStatus.ABSENT -> Color(0xFFC62828)
        AttendanceStatus.OVERTIME -> Color(0xFF1565C0)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label, 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            value, 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferWorkerDialog(
    worker: WorkerEntity,
    sites: List<SiteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, LocalDate) -> Unit
) {
    var selectedSiteId by remember { mutableStateOf<Long?>(null) }
    var transferDate by remember { mutableStateOf(LocalDate.now()) }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            transferDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        transferDate.year,
        transferDate.monthValue - 1,
        transferDate.dayOfMonth
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Worker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Worker: ${worker.name}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transfer To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        sites.filter { it.id != worker.currentSiteId }.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedSiteId = site.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Text(
                    "Date: ${Utils.formatDate(transferDate)}", 
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(onClick = { datePickerDialog.show() }) {
                    Text("Select Transfer Date", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedSiteId?.let { onConfirm(it, transferDate) } },
                enabled = selectedSiteId != null
            ) {
                Text("Transfer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordWorkerPaymentDialog(
    worker: WorkerEntity,
    sites: List<SiteEntity>,
    initialType: PaymentType,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, PaymentMethod, PaymentType, LocalDate, String) -> Unit
) {
    var selectedSiteId by remember { mutableStateOf<Long>(worker.currentSiteId ?: sites.firstOrNull()?.id ?: 0L) }
    var amount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    val selectedType = initialType
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
        title = {
            Text(
                if (selectedType == PaymentType.ADVANCE) "Record Advance for ${worker.name}" else "Record Payment for ${worker.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fixed Payment Type Indicator
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Type: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (selectedType == PaymentType.ADVANCE) "Advance" else "Payment",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedType == PaymentType.ADVANCE) Color(0xFFEF6C00) else Color(0xFF1565C0)
                        )
                    }
                }

                // Site selection
                ExposedDropdownMenuBox(
                    expanded = siteExpanded,
                    onExpandedChange = { siteExpanded = !siteExpanded }
                ) {
                    OutlinedTextField(
                        value = sites.find { it.id == selectedSiteId }?.name ?: "Select Site",
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
                    label = { Text("Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = Utils.formatDate(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.name, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description (Optional)") },
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
                    if (selectedSiteId != 0L && amountVal > 0 && !isSaving) {
                        isSaving = true
                        onConfirm(selectedSiteId, amountVal, selectedMethod, selectedType, selectedDate, note)
                    }
                },
                enabled = !isSaving && selectedSiteId != 0L && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareWorkerReportDialog(
    worker: WorkerEntity,
    stats: WorkerStats,
    attendanceHistory: List<AttendanceEntity>,
    paymentHistory: List<PaymentEntity>,
    sites: List<SiteEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf(worker.phone) }
    val currentSiteName = sites.find { it.id == worker.currentSiteId }?.name ?: "N/A"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Worker Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Worker: ${worker.name}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Recipient Phone Number (WhatsApp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Report Summary Preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("• Site: $currentSiteName", style = MaterialTheme.typography.bodySmall)
                            Text("• Working Days: ${stats.presentDays + stats.halfDays} Days (Present: ${stats.presentDays}, Half: ${stats.halfDays}, Absent: ${stats.absentDays})", style = MaterialTheme.typography.bodySmall)
                            Text("• Total Earned: ${Utils.formatCurrency(stats.totalEarned)}", style = MaterialTheme.typography.bodySmall)
                            Text("• Total Paid: ${Utils.formatCurrency(stats.totalPaid)}", style = MaterialTheme.typography.bodySmall)
                            Text("• Pending Balance: ${Utils.formatCurrency(stats.pendingBalance)}", style = MaterialTheme.typography.bodySmall)
                            Text("• Records: ${attendanceHistory.size} Attendance entries, ${paymentHistory.size} Payment entries", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Action buttons stacked vertically in precise order with separate layout space
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareWorkerReport(
                                context = context,
                                worker = worker,
                                phone = phoneInput,
                                currentSiteName = currentSiteName,
                                stats = stats,
                                attendanceHistory = attendanceHistory,
                                paymentHistory = paymentHistory,
                                sites = sites,
                                targetWhatsApp = true
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            shareWorkerReport(
                                context = context,
                                worker = worker,
                                phone = phoneInput,
                                currentSiteName = currentSiteName,
                                stats = stats,
                                attendanceHistory = attendanceHistory,
                                paymentHistory = paymentHistory,
                                sites = sites,
                                targetWhatsApp = false
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF Report", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

fun shareWorkerReport(
    context: Context,
    worker: WorkerEntity,
    phone: String,
    currentSiteName: String,
    stats: WorkerStats,
    attendanceHistory: List<AttendanceEntity>,
    paymentHistory: List<PaymentEntity>,
    sites: List<SiteEntity>,
    targetWhatsApp: Boolean
) {
    try {
        val pdfFile = PdfReportGenerator.generateWorkerReportPdf(
            context = context,
            worker = worker,
            siteName = currentSiteName,
            stats = stats,
            attendanceHistory = attendanceHistory,
            paymentHistory = paymentHistory,
            sites = sites
        )

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val totalWorkingDays = stats.presentDays + stats.halfDays
        val messageText = """
            *Worker Statement: ${worker.name}*
            Phone: ${if (worker.phone.isBlank()) "N/A" else worker.phone}
            Current Site: $currentSiteName
            
            *Summary:*
            • Present Days: ${stats.presentDays}
            • Half Days: ${stats.halfDays}
            • Absent Days: ${stats.absentDays}
            • Total Working Days: $totalWorkingDays
            • Total Earned: ${Utils.formatCurrency(stats.totalEarned)}
            • Total Paid: ${Utils.formatCurrency(stats.totalPaid)}
            • Pending Balance: ${Utils.formatCurrency(stats.pendingBalance)}
            
            Attached complete PDF report for full details.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, messageText)
            putExtra(Intent.EXTRA_SUBJECT, "Worker Statement - ${worker.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val formattedPhone = phone.replace(Regex("[^0-9]"), "")

        if (targetWhatsApp && formattedPhone.isNotBlank()) {
            val waPhone = if (formattedPhone.length == 10) "91$formattedPhone" else formattedPhone
            try {
                intent.setPackage("com.whatsapp")
                intent.putExtra("jid", "$waPhone@s.whatsapp.net")
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    intent.setPackage("com.whatsapp.w4b")
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$waPhone&text=${URLEncoder.encode(messageText, "UTF-8")}")
                    val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                    try {
                        context.startActivity(waIntent)
                    } catch (_: Exception) {
                        intent.setPackage(null)
                        intent.removeExtra("jid")
                        val chooser = Intent.createChooser(intent, "Share Worker Report")
                        context.startActivity(chooser)
                    }
                }
            }
        } else {
            val chooser = Intent.createChooser(intent, "Share Worker Report")
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate report: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

package com.example.workermanager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.*
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.*
import java.time.LocalDate

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
    val worker by viewModel.getWorkerById(workerId).collectAsState(initial = null)
    val stats by viewModel.getWorkerStats(workerId).collectAsState(initial = WorkerStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0))
    val attendanceHistory by attendanceViewModel.getAttendanceByWorker(workerId).collectAsState(initial = emptyList())
    val paymentHistory by paymentViewModel.getPaymentsByWorker(workerId).collectAsState(initial = emptyList())
    val siteHistory by viewModel.getWorkerSiteHistory(workerId).collectAsState(initial = emptyList())
    val sites by siteViewModel.allSites.collectAsState()

    var showTransferDialog by remember { mutableStateOf(false) }

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
                    WorkerStatsSection(stats)
                }

                item {
                    Text(
                        "Site History", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(siteHistory) { history ->
                    val siteName = sites.find { it.id == history.siteId }?.name ?: "Unknown Site"
                    SiteHistoryItem(history, siteName)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Attendance History", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                items(attendanceHistory) { attendance ->
                    val siteName = sites.find { it.id == attendance.siteId }?.name ?: "Unknown"
                    AttendanceHistoryItem(attendance, siteName)
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Payment History", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                items(paymentHistory) { payment ->
                    val siteName = sites.find { it.id == payment.siteId }?.name ?: "Unknown"
                    PaymentHistoryItem(payment, siteName)
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
fun WorkerStatsSection(stats: WorkerStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun PaymentHistoryItem(payment: PaymentEntity, siteName: String) {
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
            }
            Text(
                Utils.formatCurrency(payment.amount), 
                fontWeight = FontWeight.ExtraBold, 
                color = Color(0xFF2E7D32)
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val datePickerDialog = android.app.DatePickerDialog(
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
                
                // Simplified date selection for prompt fulfillment
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

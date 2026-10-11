package com.example.workermanager.ui.screens.attendance

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.AttendanceViewModel
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerAttendance
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    siteViewModel: SiteViewModel
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val sites by siteViewModel.allSites.collectAsState()
    val workersWithAttendance by viewModel.workersWithAttendance.collectAsState()
    val context = LocalContext.current

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.updateSelectedDate(LocalDate.of(year, month + 1, dayOfMonth))
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Attendance", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Date and Site Selector Section
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Utils.formatDate(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }

            if (workersWithAttendance.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedSiteId == null) "No active workers found." 
                        else "No workers assigned to this site on this date.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(workersWithAttendance) { item ->
                        AttendanceWorkerCard(
                            item = item,
                            onStatusChange = { status, otHours, note ->
                                viewModel.markAttendance(item.worker, status, otHours, note)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SiteChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = name,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AttendanceWorkerCard(
    item: WorkerAttendance,
    onStatusChange: (AttendanceStatus, Double, String) -> Unit
) {
    var showOtDialog by remember { mutableStateOf(false) }
    var pendingStatusConfirm by remember { mutableStateOf<AttendanceStatus?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        item.worker.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        item.worker.workerType.name, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (item.attendance != null) {
                    Text(
                        text = Utils.formatCurrency(item.attendance.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusButton("P", AttendanceStatus.PRESENT, item.attendance?.status, Modifier.weight(1f)) {
                    pendingStatusConfirm = AttendanceStatus.PRESENT
                }
                StatusButton("HD", AttendanceStatus.HALF_DAY, item.attendance?.status, Modifier.weight(1f)) {
                    pendingStatusConfirm = AttendanceStatus.HALF_DAY
                }
                StatusButton("A", AttendanceStatus.ABSENT, item.attendance?.status, Modifier.weight(1f)) {
                    pendingStatusConfirm = AttendanceStatus.ABSENT
                }
                StatusButton("OT", AttendanceStatus.OVERTIME, item.attendance?.status, Modifier.weight(1f)) {
                    showOtDialog = true
                }
            }

            if (item.attendance?.status == AttendanceStatus.OVERTIME) {
                Text(
                    "OT: ${item.attendance.overtimeHours} hrs | ₹${item.attendance.overtimeAmount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (pendingStatusConfirm != null) {
        AttendanceConfirmDialog(
            workerName = item.worker.name,
            status = pendingStatusConfirm!!,
            onDismiss = { pendingStatusConfirm = null },
            onConfirm = {
                val statusToSave = pendingStatusConfirm!!
                pendingStatusConfirm = null
                onStatusChange(statusToSave, 0.0, "")
            }
        )
    }

    if (showOtDialog) {
        OvertimeDialog(
            workerName = item.worker.name,
            onDismiss = { showOtDialog = false },
            onConfirm = { hours, note ->
                onStatusChange(AttendanceStatus.OVERTIME, hours, note)
                showOtDialog = false
            }
        )
    }
}

@Composable
fun AttendanceConfirmDialog(
    workerName: String,
    status: AttendanceStatus,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val statusText = when (status) {
        AttendanceStatus.PRESENT -> "Present"
        AttendanceStatus.HALF_DAY -> "Half Day"
        AttendanceStatus.ABSENT -> "Absent"
        AttendanceStatus.OVERTIME -> "Overtime"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Attendance", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Worker: $workerName",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Attendance: $statusText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Are you sure you want to mark this attendance?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Composable
fun StatusButton(
    label: String,
    status: AttendanceStatus,
    selectedStatus: AttendanceStatus?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSelected = status == selectedStatus
    val color = when (status) {
        AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
        AttendanceStatus.HALF_DAY -> Color(0xFFEF6C00)
        AttendanceStatus.ABSENT -> Color(0xFFC62828)
        AttendanceStatus.OVERTIME -> Color(0xFF1565C0)
        else -> Color(0xFF424242)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun OvertimeDialog(
    workerName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var hours by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overtime for $workerName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("OT Hours") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val h = hours.toDoubleOrNull() ?: 0.0
                onConfirm(h, note)
            }) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

package com.example.workermanager.ui.screens.attendance

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
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
            val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
            if (!pickedDate.isAfter(LocalDate.now())) {
                viewModel.updateSelectedDate(pickedDate)
            } else {
                Toast.makeText(context, "Future dates are not allowed for attendance.", Toast.LENGTH_SHORT).show()
            }
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }

    Scaffold(
        topBar = {
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Attendance",
                screenIcon = Icons.Default.History
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Interactive Clickable Attendance Date Bar
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    onClick = { datePickerDialog.show() },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Change Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = Utils.formatDate(selectedDate),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val isToday = selectedDate == LocalDate.now()
                                Text(
                                    text = if (isToday) "Today's Attendance" else "Past Date Attendance (${Utils.formatDate(selectedDate)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "Pick Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = workersWithAttendance,
                        key = { it.worker.id }
                    ) { item ->
                        AttendanceWorkerCard(
                            workerAttendance = item,
                            onStatusChange = { status, overtime, note ->
                                if (selectedDate.isAfter(LocalDate.now())) {
                                    Toast.makeText(context, "Cannot record attendance for future dates.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.markAttendance(item.worker, status, overtime, note)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceWorkerCard(
    workerAttendance: WorkerAttendance,
    onStatusChange: (AttendanceStatus, Double, String) -> Unit
) {
    val worker = workerAttendance.worker
    val attendance = workerAttendance.attendance
    var showOvertimeDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily Rate: ${Utils.formatCurrency(worker.dailyRate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (attendance?.overtimeHours != null && attendance.overtimeHours > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "OT: ${attendance.overtimeHours} hrs (${Utils.formatCurrency(attendance.overtimeAmount)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attendance Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusButton(
                    text = "Present",
                    isSelected = attendance?.status == AttendanceStatus.PRESENT,
                    color = Color(0xFF2E7D32),
                    onClick = { onStatusChange(AttendanceStatus.PRESENT, attendance?.overtimeHours ?: 0.0, attendance?.note ?: "") },
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    text = "Half Day",
                    isSelected = attendance?.status == AttendanceStatus.HALF_DAY,
                    color = Color(0xFFEF6C00),
                    onClick = { onStatusChange(AttendanceStatus.HALF_DAY, attendance?.overtimeHours ?: 0.0, attendance?.note ?: "") },
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    text = "Absent",
                    isSelected = attendance?.status == AttendanceStatus.ABSENT,
                    color = Color(0xFFC62828),
                    onClick = { onStatusChange(AttendanceStatus.ABSENT, 0.0, attendance?.note ?: "") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { showOvertimeDialog = true },
                    contentPadding = PaddingValues(2.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("OT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showOvertimeDialog) {
        OvertimeDialog(
            currentOvertime = attendance?.overtimeHours ?: 0.0,
            onDismiss = { showOvertimeDialog = false },
            onConfirm = { hours ->
                val currentStatus = attendance?.status ?: AttendanceStatus.PRESENT
                onStatusChange(currentStatus, hours, attendance?.note ?: "")
                showOvertimeDialog = false
            }
        )
    }
}

@Composable
fun StatusButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OvertimeDialog(
    currentOvertime: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var hoursText by remember { mutableStateOf(if (currentOvertime > 0) currentOvertime.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overtime Hours", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = hoursText,
                onValueChange = { hoursText = it },
                label = { Text("Hours") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = hoursText.toDoubleOrNull() ?: 0.0
                    onConfirm(hours)
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SiteChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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

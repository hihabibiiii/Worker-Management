package com.example.workermanager.ui.screens.sites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.AttendanceViewModel
import com.example.workermanager.viewmodel.PaymentViewModel
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailsScreen(
    siteId: Long,
    viewModel: SiteViewModel,
    workerViewModel: WorkerViewModel,
    attendanceViewModel: AttendanceViewModel,
    paymentViewModel: PaymentViewModel,
    onBack: () -> Unit,
    onWorkerClick: (Long) -> Unit
) {
    val site by viewModel.getSiteById(siteId).collectAsState(initial = null)
    val workers by workerViewModel.getWorkersBySite(siteId).collectAsState(initial = emptyList())
    val attendance by attendanceViewModel.getAttendanceBySite(siteId).collectAsState(initial = emptyList())
    val payments by paymentViewModel.getPaymentsBySite(siteId).collectAsState(initial = emptyList())
    val todaySiteAttendance by attendanceViewModel.getSiteAttendanceForDate(siteId, java.time.LocalDate.now()).collectAsState(initial = null)

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(site?.name ?: "Site Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (site != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Site")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Site")
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
        }
    ) { padding ->
        site?.let { currentSite ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SiteInfoSection(currentSite, todaySiteAttendance)
                }

                item {
                    SiteStatsSection(workers, attendance, payments)
                }

                item {
                    Text(
                        "Workers at this Site", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(workers) { worker ->
                    WorkerListItem(worker, onClick = { onWorkerClick(worker.id) })
                }
                
                if (workers.isEmpty()) {
                    item {
                        Text(
                            "No workers currently assigned to this site.", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showEditDialog) {
                EditSiteDialog(
                    site = currentSite,
                    onDismiss = { showEditDialog = false },
                    onConfirm = { name, address, manager ->
                        viewModel.updateSite(currentSite.copy(name = name, address = address, manager = manager))
                        showEditDialog = false
                    }
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Site") },
                    text = { Text("Are you sure you want to delete this site? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteSite(currentSite)
                                showDeleteDialog = false
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun SiteInfoSection(site: SiteEntity, todaySiteAttendance: com.example.workermanager.data.entity.SiteAttendanceEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Details", 
                style = MaterialTheme.typography.titleSmall, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow("Address", site.address)
            DetailRow("Manager", site.manager)
            DetailRow("Started", Utils.formatDate(site.startDate))
            DetailRow("Status", site.status.name)
            val attendanceStr = if (todaySiteAttendance?.isPresent == true) "Present" else "Absent"
            DetailRow("Today's Attendance", attendanceStr)
        }
    }
}

@Composable
fun SiteStatsSection(workers: List<WorkerEntity>, attendance: List<com.example.workermanager.data.entity.AttendanceEntity>, payments: List<com.example.workermanager.data.entity.PaymentEntity>) {
    val totalEarned = attendance.sumOf { it.amount }
    val totalPaid = payments.sumOf { it.amount }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Workers", workers.size.toString(), Icons.Default.Groups, Modifier.weight(1f))
        StatCard("Earned", Utils.formatCurrency(totalEarned), Icons.Default.History, Modifier.weight(1f))
        StatCard("Paid", Utils.formatCurrency(totalPaid), Icons.Default.Payments, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(
                value, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold, 
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun WorkerListItem(worker: WorkerEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(worker.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    worker.workerType.name, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                Utils.formatCurrency(worker.dailyRate), 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSiteDialog(site: SiteEntity, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(site.name) }
    var address by remember { mutableStateOf(site.address) }
    var manager by remember { mutableStateOf(site.manager) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Site", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Site Name") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                OutlinedTextField(value = manager, onValueChange = { manager = it }, label = { Text("Site Manager") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotEmpty()) onConfirm(name, address, manager) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
        }
    )
}

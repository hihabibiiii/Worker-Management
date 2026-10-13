package com.example.workermanager.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.R
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.ContractViewModel
import com.example.workermanager.viewmodel.DashboardViewModel
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    siteViewModel: SiteViewModel,
    workerViewModel: WorkerViewModel,
    contractViewModel: ContractViewModel? = null,
    onAddWorker: () -> Unit = {},
    onMarkAttendance: () -> Unit,
    onMakePayment: () -> Unit = {},
    onNavigateToRecycleBin: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSiteClick: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.example.workermanager.WorkerApplication
    val scope = rememberCoroutineScope()
    val stats by viewModel.stats.collectAsState()
    val sites by siteViewModel.allSites.collectAsState()
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val totalSiteWorkingDays by viewModel.totalSiteWorkingDays.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var showAddContractDialog by remember { mutableStateOf(false) }
    var existingAutoBackup by remember { mutableStateOf<com.example.workermanager.util.BackupPayload?>(null) }

    val selectBackupFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val payload = context.contentResolver.openInputStream(it)?.use { stream ->
                        com.example.workermanager.util.BackupManager.validateAndParseBackup(stream)
                    }
                    if (payload != null) {
                        existingAutoBackup = payload
                    } else {
                        android.widget.Toast.makeText(context, "Invalid backup file format.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Backup error: ${e.localizedMessage ?: "Invalid file"}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val prefManager = com.example.workermanager.data.preference.AutoBackupPreferenceManager(context)
        val backup = com.example.workermanager.util.BackupManager.findExistingAutoBackup(context, prefManager)
        if (backup != null && sites.isEmpty() && stats.totalContracts == 0) {
            existingAutoBackup = backup
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center

                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dashlogo),
                            contentDescription = "WorkCraft Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Work", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                                Text("Craft", fontWeight = FontWeight.Bold,fontSize = 24.sp, color = Color.White)
                            }
                            Text(
                                "Smart Work Management",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Site Selector Section
            Column(modifier = Modifier.padding(16.dp)) {


                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SiteChip(
                            name = "All Sites",
                            isSelected = selectedSiteId == null,
                            onClick = { viewModel.updateSelectedSite(null) },
                            workDays = totalSiteWorkingDays
                        )
                    }
                    items(sites) { site ->
                        val workDays by viewModel.getSiteWorkingDays(site.id).collectAsState(initial = 0)
                        SiteChip(
                            name = site.name,
                            isSelected = selectedSiteId == site.id,
                            onClick = { viewModel.updateSelectedSite(site.id) },
                            workDays = workDays
                        )
                    }
                }
            }

            // Stats Grid
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatBox(
                    label = "Present",
                    value = stats.presentWorkers.toString(),
                    color = Color(0xFF2E7D32), // Darker Green
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Absent",
                    value = stats.absentWorkers.toString(),
                    color = Color(0xFFC62828), // Darker Red
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Half Day",
                    value = stats.halfDayWorkers.toString(),
                    color = Color(0xFFEF6C00), // Darker Orange
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Overtime",
                    value = stats.overtimeWorkers.toString(),
                    color = Color(0xFF1565C0), // Darker Blue
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Financial Summary
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "Financial Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SummaryRow("Today's Earnings", Utils.formatCurrency(stats.todayEarned))
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryRow("Total Earned", Utils.formatCurrency(stats.totalEarned))
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryRow("Total Paid", Utils.formatCurrency(stats.totalPaid))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    SummaryRow(
                        label = "Pending Balance",
                        value = Utils.formatCurrency(stats.totalPending),
                        isBold = true,
                        valueColor = if (stats.totalPending > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }

            // Client Payments Summary (Expandable dropdown card, visible when contracts exist)
            if (stats.totalContracts > 0) {
                var isClientSummaryExpanded by remember { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isClientSummaryExpanded = !isClientSummaryExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Client Payments Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = if (isClientSummaryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isClientSummaryExpanded) "Collapse Summary" else "Expand Summary",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        AnimatedVisibility(visible = isClientSummaryExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                SummaryRow("Total Contracts", stats.totalContracts.toString())
                                SummaryRow("Total Receivable", Utils.formatCurrency(stats.totalContractReceivable))
                                SummaryRow("Total Received", Utils.formatCurrency(stats.totalContractReceived))
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                )
                                SummaryRow(
                                    label = "Total Remaining",
                                    value = Utils.formatCurrency(stats.totalContractRemaining),
                                    isBold = true,
                                    valueColor = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Find Existing Backup Prompt (Visible when database is empty)
            if (sites.isEmpty() && stats.totalContracts == 0 && existingAutoBackup == null) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find Existing Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "If you backed up your WorkCraft data on this device previously, locate your backup file in Documents/WorkCraftBackups to restore your records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                selectBackupFileLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Locate Backup File", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Quick Actions
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionCard("Add New Worker", Icons.Default.PersonAdd, { showAddWorkerDialog = true }, Modifier.weight(1f))
                    ActionCard("Attendance", Icons.Default.EventAvailable, onMarkAttendance, Modifier.weight(1f))
                    ActionCard("Add New Site", Icons.Default.AddLocation, { showAddSiteDialog = true }, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionCard("Add Contract", Icons.AutoMirrored.Filled.Assignment, { showAddContractDialog = true }, Modifier.weight(1f))
                    ActionCard("Reports", Icons.Default.Assessment, onNavigateToReports, Modifier.weight(1f))
                    ActionCard("Recycle Bin", Icons.Default.DeleteSweep, onNavigateToRecycleBin, Modifier.weight(1f))

                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showAddWorkerDialog) {
            com.example.workermanager.ui.screens.workers.WorkerDialog(
                worker = null,
                sites = sites,
                onDismiss = { showAddWorkerDialog = false },
                onAddNewSite = { siteName, siteAddress, siteManager, onCreated ->
                    siteViewModel.addSite(name = siteName, address = siteAddress, manager = siteManager) { newId ->
                        onCreated(newId)
                    }
                },
                onConfirm = { name, phone, type, dailyRate, otRate, date, siteId, notes, active ->
                    workerViewModel.addWorker(name, phone, type, dailyRate, otRate, date, siteId, notes)
                    showAddWorkerDialog = false
                }
            )
        }

        if (showAddSiteDialog) {
            com.example.workermanager.ui.screens.sites.AddSiteDialog(
                sites = sites,
                onDismiss = { showAddSiteDialog = false },
                onConfirm = { name, address, manager ->
                    siteViewModel.addSite(name, address, manager)
                    showAddSiteDialog = false
                }
            )
        }

        if (showAddContractDialog && contractViewModel != null) {
            com.example.workermanager.ui.screens.contracts.AddContractDialog(
                sites = sites,
                onDismiss = { showAddContractDialog = false },
                onConfirm = { name, mobile, siteId, amount, date, notes ->
                    contractViewModel.addContract(name, mobile, siteId, amount, date, notes)
                    showAddContractDialog = false
                }
            )
        }

        if (existingAutoBackup != null) {
            val payload = existingAutoBackup!!
            AlertDialog(
                onDismissRequest = { existingAutoBackup = null },
                title = { Text("Existing WorkCraft Backup Found", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "We found a previous WorkCraft backup containing ${payload.workers.size} workers and ${payload.sites.size} sites. Would you like to restore your records?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetPayload = payload
                            existingAutoBackup = null
                            scope.launch {
                                try {
                                    com.example.workermanager.util.BackupManager.createSafetyBackupInternal(context, app.database)
                                    com.example.workermanager.util.BackupManager.restoreBackup(app.database, targetPayload)
                                    android.widget.Toast.makeText(context, "Data restored successfully!", android.widget.Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Restoration failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Restore Data", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { existingAutoBackup = null }) {
                        Text("Start Fresh", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}

@Composable
fun SiteChip(
    name: String, 
    isSelected: Boolean, 
    onClick: () -> Unit,
    workDays: Int? = null
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.defaultMinSize(minHeight = if (workDays != null) 48.dp else 40.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                softWrap = true,
                maxLines = 2
            )
            if (workDays != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Work : $workDays Days",
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label, 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ActionCard(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

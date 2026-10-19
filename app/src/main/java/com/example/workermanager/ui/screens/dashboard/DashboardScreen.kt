package com.example.workermanager.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.ContractViewModel
import com.example.workermanager.viewmodel.DashboardViewModel
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    siteViewModel: SiteViewModel,
    workerViewModel: WorkerViewModel,
    contractViewModel: ContractViewModel? = null,
    siteExpenseViewModel: com.example.workermanager.viewmodel.SiteExpenseViewModel? = null,
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

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val dashBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF7F9FE)
    val dashPrimary = MaterialTheme.colorScheme.primary
    val darkText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val subtitleText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var showAddContractDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
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
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Dashboard",
                screenIcon = Icons.Default.Dashboard,
                onNotificationClick = onSettingsClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(dashBg)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 1: SITE SELECTOR CARDS (Horizontally Scrollable)
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SiteSelectorCard(
                            name = "All Sites",
                            isSelected = selectedSiteId == null,
                            onClick = { viewModel.updateSelectedSite(null) },
                            workDays = totalSiteWorkingDays,
                            isDark = isDark
                        )
                    }
                    items(sites) { site ->
                        val workDays by viewModel.getSiteWorkingDays(site.id).collectAsState(initial = 0)
                        SiteSelectorCard(
                            name = site.name,
                            isSelected = selectedSiteId == site.id,
                            onClick = { viewModel.updateSelectedSite(site.id) },
                            workDays = workDays,
                            isDark = isDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: ATTENDANCE SUMMARY CARDS (4 Equal Grid Boxes)
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AttendanceStatCard(
                        label = "Present",
                        value = stats.presentWorkers.toString(),
                        icon = Icons.Default.Groups,
                        bgColor = if (isDark) Color(0xFF1B382B) else Color(0xFFE8F8F0),
                        iconColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF0AA65B),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceStatCard(
                        label = "Absent",
                        value = stats.absentWorkers.toString(),
                        icon = Icons.Default.PersonOff,
                        bgColor = if (isDark) Color(0xFF3E1F24) else Color(0xFFFDE8EC),
                        iconColor = if (isDark) Color(0xFFFF5252) else Color(0xFFE63946),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceStatCard(
                        label = "Half Day",
                        value = stats.halfDayWorkers.toString(),
                        icon = Icons.Default.Schedule,
                        bgColor = if (isDark) Color(0xFF3B2E19) else Color(0xFFFFF3DB),
                        iconColor = if (isDark) Color(0xFFFFA726) else Color(0xFFF28C00),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceStatCard(
                        label = "Overtime",
                        value = stats.overtimeWorkers.toString(),
                        icon = Icons.Default.Timelapse,
                        bgColor = if (isDark) Color(0xFF1E2A3A) else Color(0xFFE2EEFF),
                        iconColor = if (isDark) Color(0xFF4285F4) else Color(0xFF0866E8),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 3: EXPANDABLE SUMMARY SECTIONS (Worker, Client, Site Expense)
            var isWorkerSummaryExpanded by remember { mutableStateOf(false) }
            var isClientSummaryExpanded by remember { mutableStateOf(false) }
            var isExpenseSummaryExpanded by remember { mutableStateOf(false) }

            // 1. Worker Payment Summary
            ExpandableSummaryCard(
                title = "Worker Payment Summary",
                subtitle = "View worker wise payment details",
                icon = Icons.Default.AccountBalanceWallet,
                cardBg = if (isDark) Color(0xFF1E2A3A) else Color(0xFFE2F0FF),
                accentColor = if (isDark) Color(0xFF4285F4) else Color(0xFF0866E8),
                isExpanded = isWorkerSummaryExpanded,
                isDark = isDark,
                onToggle = { isWorkerSummaryExpanded = !isWorkerSummaryExpanded }
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                SummaryRow("Today's Wages", Utils.formatCurrency(stats.todayEarned), darkText = darkText)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow("Total Wages", Utils.formatCurrency(stats.totalEarned), darkText = darkText)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow("Total Paid", Utils.formatCurrency(stats.totalPaid), darkText = darkText)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = dashPrimary.copy(alpha = 0.2f))
                SummaryRow(
                    label = "Pending Balance",
                    value = Utils.formatCurrency(stats.totalPending),
                    isBold = true,
                    valueColor = if (stats.totalPending > 0) Color(0xFFFF5252) else Color(0xFF2ECC71),
                    darkText = darkText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Client Payments Summary
            ExpandableSummaryCard(
                title = "Client Payments Summary",
                subtitle = "View client wise payment details",
                icon = Icons.Default.Person,
                cardBg = if (isDark) Color(0xFF1B382B) else Color(0xFFDDF8F1),
                accentColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF0BAA75),
                isExpanded = isClientSummaryExpanded,
                isDark = isDark,
                onToggle = { isClientSummaryExpanded = !isClientSummaryExpanded }
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                if (stats.totalContracts == 0) {
                    Text(
                        "No clients or contracts added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = darkText.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                SummaryRow("Total Contracts", stats.totalContracts.toString(), darkText = darkText)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow("Total Receivable", Utils.formatCurrency(stats.totalContractReceivable), darkText = darkText)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow("Total Received", Utils.formatCurrency(stats.totalContractReceived), darkText = darkText)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF0BAA75).copy(alpha = 0.2f))
                SummaryRow(
                    label = "Total Remaining",
                    value = Utils.formatCurrency(stats.totalContractRemaining),
                    isBold = true,
                    valueColor = Color(0xFFFF5252),
                    darkText = darkText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Sites Expense Summary
            ExpandableSummaryCard(
                title = "Sites Expense Summary",
                subtitle = "View site wise expenses details",
                icon = Icons.Default.CurrencyRupee,
                cardBg = if (isDark) Color(0xFF2B1F47) else Color(0xFFEEE8FF),
                accentColor = if (isDark) Color(0xFFB388FF) else Color(0xFF6938E8),
                isExpanded = isExpenseSummaryExpanded,
                isDark = isDark,
                onToggle = { isExpenseSummaryExpanded = !isExpenseSummaryExpanded }
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                SummaryRow("Today Expense", Utils.formatCurrency(stats.todayExpense), darkText = darkText)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow("Total Expense", Utils.formatCurrency(stats.totalExpense), darkText = darkText)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow("Total Paid", Utils.formatCurrency(stats.totalExpensePaid), darkText = darkText)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF6938E8).copy(alpha = 0.2f))
                SummaryRow(
                    label = "Total Remain",
                    value = Utils.formatCurrency(stats.totalExpenseRemain),
                    isBold = true,
                    valueColor = if (stats.totalExpenseRemain > 0) Color(0xFFFF5252) else Color(0xFF2ECC71),
                    darkText = darkText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Find Existing Backup Prompt (Visible when database is empty)
            if (sites.isEmpty() && stats.totalContracts == 0 && existingAutoBackup == null) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    border = BorderStroke(1.dp, dashPrimary.copy(alpha = 0.2f)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = dashPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find Existing Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "If you backed up your WorkCraft data on this device previously, locate your backup file in Documents/WorkCraftBackups to restore your records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { selectBackupFileLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Locate Backup File", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // SECTION 4: QUICK ACTIONS SECTION (3x2 GRID)
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = dashPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)
                    }

                    TextButton(onClick = onNavigateToReports) {
                        Text("More", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = dashPrimary)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = dashPrimary, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 1 (Add Worker, Attendance, Add Site)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionGridCard(
                        label = "Add New Worker",
                        icon = Icons.Default.PersonAdd,
                        cardBg = if (isDark) Color(0xFF1E2A3A) else Color(0xFFE2EEFF),
                        iconColor = if (isDark) Color(0xFF4285F4) else Color(0xFF0866E8),
                        darkText = darkText,
                        onClick = { showAddWorkerDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionGridCard(
                        label = "Attendance",
                        icon = Icons.Default.EventAvailable,
                        cardBg = if (isDark) Color(0xFF1B382B) else Color(0xFFDFF8EC),
                        iconColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF0B9650),
                        darkText = darkText,
                        onClick = onMarkAttendance,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionGridCard(
                        label = "Add New Site",
                        icon = Icons.Default.AddLocationAlt,
                        cardBg = if (isDark) Color(0xFF2B1F47) else Color(0xFFF0E9FF),
                        iconColor = if (isDark) Color(0xFFB388FF) else Color(0xFF6938E8),
                        darkText = darkText,
                        onClick = { showAddSiteDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2 (Add Contract, Reports, Add Expense)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionGridCard(
                        label = "Add Contract",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        cardBg = if (isDark) Color(0xFF3E2C1B) else Color(0xFFFFF0D9),
                        iconColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFEF8500),
                        darkText = darkText,
                        onClick = { showAddContractDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionGridCard(
                        label = "Reports",
                        icon = Icons.Default.BarChart,
                        cardBg = if (isDark) Color(0xFF1B323D) else Color(0xFFDFF5FC),
                        iconColor = if (isDark) Color(0xFF4DD0E1) else Color(0xFF08A3C7),
                        darkText = darkText,
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionGridCard(
                        label = "Add Expense",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        cardBg = if (isDark) Color(0xFF3A1E27) else Color(0xFFFCE5EB),
                        iconColor = if (isDark) Color(0xFFFF4081) else Color(0xFFD83D65),
                        darkText = darkText,
                        onClick = { showAddExpenseDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Dialogs & Prompts
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

        if (showAddExpenseDialog && siteExpenseViewModel != null) {
            com.example.workermanager.ui.screens.expenses.AddExpenseDialog(
                sites = sites,
                initialSiteId = selectedSiteId,
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { siteId, date, cat, desc, qty, rate, amt, paidBy, method, notes, receiptPath, initialPaid ->
                    siteExpenseViewModel.addExpense(siteId, date, cat, desc, qty, rate, amt, paidBy, method, notes, receiptPath, initialPaid)
                    showAddExpenseDialog = false
                    android.widget.Toast.makeText(context, "Expense recorded successfully!", android.widget.Toast.LENGTH_SHORT).show()
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
                            color = darkText
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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dashPrimary)
                    ) {
                        Text("Restore Data", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { existingAutoBackup = null }) {
                        Text("Start Fresh", color = darkText)
                    }
                }
            )
        }
    }
}

@Composable
fun SiteSelectorCard(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    workDays: Int? = null,
    isDark: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF1677F2) else (if (isDark) MaterialTheme.colorScheme.surface else Color.White),
        contentColor = if (isSelected) Color.White else (if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF1677F2) else (if (isDark) Color(0xFF333A45) else Color(0xFFCBD5E1))),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier.defaultMinSize(minHeight = 52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF1677F2),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (workDays != null) {
                    Text(
                        text = "Work : $workDays Days",
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.35f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (MaterialTheme.colorScheme.surface.luminance() < 0.4f) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = iconColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ExpandableSummaryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    cardBg: Color,
    accentColor: Color,
    isExpanded: Boolean,
    isDark: Boolean = false,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Colored Vertical Accent on the Left
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = cardBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A),
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionGridCard(
    label: String,
    icon: ImageVector,
    cardBg: Color,
    iconColor: Color,
    darkText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = iconColor, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false, valueColor: Color? = null, darkText: Color = Color(0xFF0F172A)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = darkText
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor ?: darkText
        )
    }
}

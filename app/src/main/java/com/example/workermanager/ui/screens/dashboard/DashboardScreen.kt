package com.example.workermanager.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.workermanager.data.entity.*
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
    onSiteClick: (Long) -> Unit,
    onOpenSubscription: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.example.workermanager.WorkerApplication
    val scope = rememberCoroutineScope()
    val stats by viewModel.stats.collectAsState()
    val sites by siteViewModel.allSites.collectAsState()
    val workers by workerViewModel.workers.collectAsState()
    val totalActiveWorkerCount by workerViewModel.totalActiveWorkerCount.collectAsState()
    val totalActiveSiteCount by siteViewModel.totalActiveSiteCount.collectAsState()
    val proAccessLevel by app.subscriptionRepository.proAccessLevel.collectAsState()
    val isProUnlocked = proAccessLevel.isProUnlocked
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val totalSiteWorkingDays by viewModel.totalSiteWorkingDays.collectAsState()

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val dashBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF7F9FE)
    val dashPrimary = MaterialTheme.colorScheme.primary
    val darkText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var showSiteQuotaDialog by remember { mutableStateOf(false) }
    var showWorkerQuotaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(siteViewModel) {
        siteViewModel.quotaExceededEvent.collect {
            showSiteQuotaDialog = true
        }
    }
    LaunchedEffect(workerViewModel) {
        workerViewModel.quotaExceededEvent.collect {
            showWorkerQuotaDialog = true
        }
    }
    var showAddContractDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var existingAutoBackup by remember { mutableStateOf<com.example.workermanager.util.BackupPayload?>(null) }

    LaunchedEffect(Unit) {
        val prefManager = com.example.workermanager.data.preference.AutoBackupPreferenceManager(context)
        val backup = com.example.workermanager.util.BackupManager.findExistingAutoBackup(context, prefManager)
        if (backup != null && sites.isEmpty() && stats.totalContracts == 0) {
            val validation = com.example.workermanager.util.BackupManager.validateBackupOwnership(backup)
            if (validation is com.example.workermanager.util.OwnershipValidationResult.Valid) {
                existingAutoBackup = backup
            }
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
                    items(sites, key = { it.id }) { site ->
                        val workDays by remember(site.id) { viewModel.getSiteWorkingDays(site.id) }.collectAsState(initial = 0)
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
            DashboardSummaryCard(
                title = "Worker Payment Summary",
                subtitle = "View worker wise payment details",
                headerIcon = Icons.Default.AccountBalanceWallet,
                accentColor = if (isDark) Color(0xFF4285F4) else Color(0xFF0866E9),
                headerBgColor = if (isDark) Color(0xFF1A283C) else Color(0xFFEAF2FD),
                isExpanded = isWorkerSummaryExpanded,
                isDark = isDark,
                onToggle = { isWorkerSummaryExpanded = !isWorkerSummaryExpanded }
            ) {
                DashboardSummaryRow(
                    label = "Today's Wages",
                    value = Utils.formatCurrency(stats.todayEarned),
                    icon = Icons.Default.Today,
                    iconColor = if (isDark) Color(0xFF4285F4) else Color(0xFF0866E9),
                    isDark = isDark,
                    valueColor = if (isDark) Color(0xFF4285F4) else Color(0xFF0866E9),
                    isBoldValue = true
                )

                DashboardSummaryRow(
                    label = "Total Wages",
                    value = Utils.formatCurrency(stats.totalEarned),
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = if (isDark) Color(0xFF4285F4) else Color(0xFF0866E9),
                    isDark = isDark
                )

                DashboardSummaryRow(
                    label = "Total Paid",
                    value = Utils.formatCurrency(stats.totalPaid),
                    icon = Icons.Default.Payments,
                    iconColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF0A963D),
                    isDark = isDark
                )

                val pendingRedBg = if (isDark) Color(0xFF381014) else Color(0xFFFDE8EC)
                val pendingRedBorder = if (isDark) Color(0xFF5C1B24) else Color(0xFFF9C0C9)
                val pendingRedTextColor = if (isDark) Color(0xFFE57373) else Color(0xFFD7192D)

                DashboardSummaryRow(
                    label = "Pending Balance",
                    value = Utils.formatCurrency(stats.totalPending),
                    icon = Icons.Default.Pending,
                    iconColor = pendingRedTextColor,
                    isDark = isDark,
                    rowBgColor = pendingRedBg,
                    borderColor = pendingRedBorder,
                    valueColor = pendingRedTextColor,
                    isBoldValue = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Client Payments Summary
            DashboardSummaryCard(
                title = "Client Payments Summary",
                subtitle = "View client wise payment details",
                headerIcon = Icons.Default.Person,
                accentColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF0BAA75),
                headerBgColor = if (isDark) Color(0xFF122E22) else Color(0xFFE6F7F2),
                isExpanded = isClientSummaryExpanded,
                isDark = isDark,
                onToggle = { isClientSummaryExpanded = !isClientSummaryExpanded }
            ) {
                if (stats.totalContracts == 0) {
                    DashboardInfoRow(
                        message = "No clients or contracts added yet.",
                        icon = Icons.Default.Info,
                        accentColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF0BAA75),
                        isDark = isDark
                    )
                }

                DashboardSummaryRow(
                    label = "Total Contracts",
                    value = stats.totalContracts.toString(),
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    iconColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF0BAA75),
                    isDark = isDark
                )

                DashboardSummaryRow(
                    label = "Total Receivable",
                    value = Utils.formatCurrency(stats.totalContractReceivable),
                    icon = Icons.Default.Payments,
                    iconColor = if (isDark) Color(0xFF2ECC71) else Color(0xFF0BAA75),
                    isDark = isDark
                )

                DashboardSummaryRow(
                    label = "Total Received",
                    value = Utils.formatCurrency(stats.totalContractReceived),
                    icon = Icons.Default.CheckCircle,
                    iconColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF0A963D),
                    isDark = isDark
                )

                val remainingRedBg = if (isDark) Color(0xFF381014) else Color(0xFFFDE8EC)
                val remainingRedBorder = if (isDark) Color(0xFF5C1B24) else Color(0xFFF9C0C9)
                val remainingRedTextColor = if (isDark) Color(0xFFE57373) else Color(0xFFD7192D)

                DashboardSummaryRow(
                    label = "Total Remaining",
                    value = Utils.formatCurrency(stats.totalContractRemaining),
                    icon = Icons.Default.HourglassBottom,
                    iconColor = remainingRedTextColor,
                    isDark = isDark,
                    rowBgColor = remainingRedBg,
                    borderColor = remainingRedBorder,
                    valueColor = remainingRedTextColor,
                    isBoldValue = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Sites Expense Summary
            DashboardSummaryCard(
                title = "Sites Expense Summary",
                subtitle = "View site wise expenses details",
                headerIcon = Icons.Default.CurrencyRupee,
                accentColor = if (isDark) Color(0xFFB388FF) else Color(0xFF6938E8),
                headerBgColor = if (isDark) Color(0xFF23183B) else Color(0xFFF0EAFF),
                isExpanded = isExpenseSummaryExpanded,
                isDark = isDark,
                onToggle = { isExpenseSummaryExpanded = !isExpenseSummaryExpanded }
            ) {
                DashboardSummaryRow(
                    label = "Today Expense",
                    value = Utils.formatCurrency(stats.todayExpense),
                    icon = Icons.Default.Today,
                    iconColor = if (isDark) Color(0xFFB388FF) else Color(0xFF6938E8),
                    isDark = isDark,
                    valueColor = if (isDark) Color(0xFFB388FF) else Color(0xFF6938E8),
                    isBoldValue = true
                )

                DashboardSummaryRow(
                    label = "Total Expense",
                    value = Utils.formatCurrency(stats.totalExpense),
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    iconColor = if (isDark) Color(0xFFB388FF) else Color(0xFF6938E8),
                    isDark = isDark
                )

                DashboardSummaryRow(
                    label = "Total Paid",
                    value = Utils.formatCurrency(stats.totalExpensePaid),
                    icon = Icons.Default.Payments,
                    iconColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF0A963D),
                    isDark = isDark
                )

                val remainGreenBg = if (isDark) Color(0xFF122E22) else Color(0xFFE6F7F2)
                val remainGreenBorder = if (isDark) Color(0xFF1B5E20) else Color(0xFFC2EFE1)
                val remainGreenTextColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF0BAA75)

                DashboardSummaryRow(
                    label = "Total Remain",
                    value = Utils.formatCurrency(stats.totalExpenseRemain),
                    icon = Icons.Default.AccountBalance,
                    iconColor = remainGreenTextColor,
                    isDark = isDark,
                    rowBgColor = remainGreenBg,
                    borderColor = remainGreenBorder,
                    valueColor = remainGreenTextColor,
                    isBoldValue = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 4: QUICK ACTIONS SECTION (3x2 GRID)
            val quickActionsHeaderBg = if (isDark) Color(0xFF1E2A3A) else Color(0xFFEBF2FD)

            // Full-width horizontal background strip with rounded corners behind Quick Actions heading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(color = quickActionsHeaderBg, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = dashPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)
                }

                TextButton(
                    onClick = onNavigateToReports,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Report", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = dashPrimary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = dashPrimary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

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
                        onClick = {
                            if (!isProUnlocked && totalActiveWorkerCount >= com.example.workermanager.data.billing.ProAccessLevel.FREE_WORKER_LIMIT) {
                                showWorkerQuotaDialog = true
                            } else {
                                showAddWorkerDialog = true
                            }
                        },
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
                        onClick = {
                            if (!isProUnlocked && totalActiveSiteCount >= com.example.workermanager.data.billing.ProAccessLevel.FREE_SITE_LIMIT) {
                                showSiteQuotaDialog = true
                            } else {
                                showAddSiteDialog = true
                            }
                        },
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
                        label = "Add Site Material",
                        icon = Icons.Default.Inventory2,
                        cardBg = if (isDark) Color(0xFF1B323D) else Color(0xFFDFF5FC),
                        iconColor = if (isDark) Color(0xFF4DD0E1) else Color(0xFF08A3C7),
                        darkText = darkText,
                        onClick = { showAddMaterialDialog = true },
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

            Spacer(modifier = Modifier.height(96.dp))
        }

        // Dialogs & Prompts
        if (showAddWorkerDialog) {
            com.example.workermanager.ui.screens.workers.WorkerDialog(
                worker = null,
                sites = sites,
                siteViewModel = siteViewModel,
                onDismiss = { showAddWorkerDialog = false },
                onAddNewSite = { siteName, siteAddress, siteManager, onCreated ->
                    if (!isProUnlocked && totalActiveSiteCount >= com.example.workermanager.data.billing.ProAccessLevel.FREE_SITE_LIMIT) {
                        showSiteQuotaDialog = true
                    } else {
                        siteViewModel.addSite(
                            name = siteName,
                            address = siteAddress,
                            manager = siteManager,
                            onCreated = { newId -> onCreated(newId) },
                            onQuotaExceeded = { showSiteQuotaDialog = true }
                        )
                    }
                },
                onConfirm = { name, phone, type, dailyRate, otRate, date, siteId, notes, active ->
                    if (!isProUnlocked && totalActiveWorkerCount >= com.example.workermanager.data.billing.ProAccessLevel.FREE_WORKER_LIMIT) {
                        showWorkerQuotaDialog = true
                        showAddWorkerDialog = false
                    } else {
                        workerViewModel.addWorker(
                            name, phone, type, dailyRate, otRate, date, siteId, notes,
                            onCreated = { showAddWorkerDialog = false },
                            onQuotaExceeded = {
                                showWorkerQuotaDialog = true
                                showAddWorkerDialog = false
                            }
                        )
                    }
                }
            )
        }

        if (showAddSiteDialog) {
            com.example.workermanager.ui.screens.sites.AddSiteDialog(
                sites = sites,
                onDismiss = { showAddSiteDialog = false },
                onConfirm = { name, address, manager ->
                    if (!isProUnlocked && totalActiveSiteCount >= com.example.workermanager.data.billing.ProAccessLevel.FREE_SITE_LIMIT) {
                        showSiteQuotaDialog = true
                        showAddSiteDialog = false
                    } else {
                        siteViewModel.addSite(
                            name, address, manager,
                            onCreated = { showAddSiteDialog = false },
                            onQuotaExceeded = {
                                showSiteQuotaDialog = true
                                showAddSiteDialog = false
                            }
                        )
                    }
                }
            )
        }

        if (showSiteQuotaDialog) {
            com.example.workermanager.ui.components.ProUpgradeDialog(
                title = "Site Quota Reached",
                description = "Free accounts can manage up to ${com.example.workermanager.data.billing.ProAccessLevel.FREE_SITE_LIMIT} active ${if (com.example.workermanager.data.billing.ProAccessLevel.FREE_SITE_LIMIT == 1) "site" else "sites"}. Upgrade to WorkCraft Pro for unlimited sites, advanced analytics, and performance reports.",
                onDismissRequest = { showSiteQuotaDialog = false },
                onUpgradeClick = {
                    showSiteQuotaDialog = false
                    onOpenSubscription()
                }
            )
        }

        if (showWorkerQuotaDialog) {
            com.example.workermanager.ui.components.ProUpgradeDialog(
                title = "Worker Quota Reached",
                description = "Free accounts can manage up to ${com.example.workermanager.data.billing.ProAccessLevel.FREE_WORKER_LIMIT} active workers. Upgrade to WorkCraft Pro for unlimited workers, advanced performance reports, and PDF statements.",
                onDismissRequest = { showWorkerQuotaDialog = false },
                onUpgradeClick = {
                    showWorkerQuotaDialog = false
                    onOpenSubscription()
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

        if (showAddMaterialDialog) {
            com.example.workermanager.ui.screens.materials.AddMaterialDialog(
                onDismiss = { showAddMaterialDialog = false },
                onConfirm = { name, category, unit, initialQty, minStock, notes ->
                    scope.launch {
                        try {
                            val mat = ContractorMaterialEntity(
                                name = name,
                                category = category,
                                unit = unit,
                                minStockLevel = minStock,
                                notes = notes
                            )
                            val matId = app.contractorMaterialRepository.insertMaterial(mat)
                            if (initialQty > 0.001) {
                                val tx = ContractorMaterialTransactionEntity(
                                    materialId = matId,
                                    siteId = null,
                                    transactionType = ContractorMaterialTransactionType.ADDED,
                                    quantity = initialQty,
                                    unit = unit.displayName,
                                    reasonOrNotes = "Initial Stock Added"
                                )
                                app.contractorMaterialRepository.insertTransaction(tx)
                            }
                            android.widget.Toast.makeText(context, "Construction Material saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            showAddMaterialDialog = false
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Error saving material: ${e.localizedMessage ?: "Database error"}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
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
fun DashboardSummaryCard(
    title: String,
    subtitle: String,
    headerIcon: ImageVector,
    accentColor: Color,
    headerBgColor: Color,
    isExpanded: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardBg =
        if (isDark) MaterialTheme.colorScheme.surface else Color.White

    val borderColor =
        if (isDark) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        } else {
            Color(0xFFDCE7F5)
        }

    val titleTextColor =
        if (isDark) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xFF0B1028)
        }

    val subtitleTextColor =
        if (isDark) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color(0xFF71809A)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Accent Bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Compact Header Container
                Surface(
                    onClick = onToggle,
                    shape = RoundedCornerShape(11.dp),
                    color = headerBgColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 6.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Header Icon Container
                            Box(
                                modifier = Modifier
                                    .size(35.dp)
                                    .background(
                                        accentColor,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = headerIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            // Title and Subtitle
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = titleTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = subtitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = subtitleTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        // Expand / Collapse Button
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    accentColor.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (isExpanded) {
                                    "Collapse"
                                } else {
                                    "Expand"
                                },
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Expanded Content
                AnimatedVisibility(
                    visible = isExpanded
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSummaryRow(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    isDark: Boolean,
    rowBgColor: Color? = null,
    borderColor: Color? = null,
    valueColor: Color? = null,
    isBoldValue: Boolean = false
) {
    val defaultRowBg =
        if (isDark) MaterialTheme.colorScheme.surfaceVariant
        else Color(0xFFF4F7FC)

    val defaultBorder =
        if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        else Color(0xFFE8EEF7)

    val primaryText =
        if (isDark) MaterialTheme.colorScheme.onSurface
        else Color(0xFF0B1028)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = rowBgColor ?: defaultRowBg,
        border = BorderStroke(1.dp, borderColor ?: defaultBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp), // Height reduced
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp), // Reduced from 38.dp
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                iconColor.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = if (isBoldValue) FontWeight.Bold
                else FontWeight.SemiBold,
                color = valueColor ?: primaryText,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DashboardInfoRow(
    message: String,
    icon: ImageVector = Icons.Default.Info,
    accentColor: Color,
    isDark: Boolean
) {
    val bg = if (isDark) accentColor.copy(alpha = 0.15f) else Color(0xFFE6F7F2)
    val border = if (isDark) accentColor.copy(alpha = 0.3f) else Color(0xFFC2EFE1)
    val textAndIconColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF0BAA75)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textAndIconColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textAndIconColor
            )
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
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

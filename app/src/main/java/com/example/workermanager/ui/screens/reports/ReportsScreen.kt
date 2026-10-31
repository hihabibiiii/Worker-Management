package com.example.workermanager.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.util.ShareUtils
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.MonthSummaryModel
import com.example.workermanager.viewmodel.ReportType
import com.example.workermanager.viewmodel.ReportViewModel
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerMonthlyReport
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import com.example.workermanager.WorkerApplication
import com.example.workermanager.ui.components.LockedProFeatureScreen
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportViewModel,
    siteViewModel: SiteViewModel,
    onBack: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? WorkerApplication
    val proAccessLevel by (app?.subscriptionRepository?.proAccessLevel ?: MutableStateFlow(com.example.workermanager.data.billing.ProAccessLevel.FREE)).collectAsState()
    val isProUnlocked = proAccessLevel.isProUnlocked

    if (!isProUnlocked) {
        LockedProFeatureScreen(
            title = "Reports & Analytics are Pro Features",
            subtitle = "Upgrade to WorkCraft Pro to unlock weekly, monthly, and yearly worker performance reports, attendance analytics, expense breakdowns, and automated data exports.",
            onUpgradeClick = onUpgradeClick,
            onBack = onBack
        )
        return
    }
    val selectedReportType by viewModel.selectedReportType.collectAsState()
    val selectedWeekStartDate by viewModel.selectedWeekStartDate.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val sites by siteViewModel.allSites.collectAsState()

    val weeklyReports by viewModel.weeklyReport.collectAsState()
    val monthlyReports by viewModel.monthlyReport.collectAsState()
    val yearlySummaryReport by viewModel.yearlySummaryReport.collectAsState()

    val currentReports = if (selectedReportType == ReportType.WEEKLY) weeklyReports else monthlyReports

    Scaffold(
        topBar = {
            com.example.workermanager.ui.components.WorkCraftTopHeader(
                screenTitle = "Performance Reports",
                screenIcon = Icons.Default.Assessment,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Report Type Toggle Tabs (3 Options: Weekly, Monthly, Yearly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isWeeklySelected = selectedReportType == ReportType.WEEKLY
                FilterChip(
                    selected = isWeeklySelected,
                    onClick = { viewModel.updateReportType(ReportType.WEEKLY) },
                    label = {
                        Text(
                            text = "Weekly",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isWeeklySelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )

                val isMonthlySelected = selectedReportType == ReportType.MONTHLY
                FilterChip(
                    selected = isMonthlySelected,
                    onClick = { viewModel.updateReportType(ReportType.MONTHLY) },
                    label = {
                        Text(
                            text = "Monthly",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isMonthlySelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )

                val isYearlySelected = selectedReportType == ReportType.YEARLY
                FilterChip(
                    selected = isYearlySelected,
                    onClick = { viewModel.updateReportType(ReportType.YEARLY) },
                    label = {
                        Text(
                            text = "Yearly",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isYearlySelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Site Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SiteChip(
                        name = "All Sites",
                        isSelected = selectedSiteId == null,
                        onClick = { viewModel.updateSite(null) }
                    )
                }
                items(sites) { site ->
                    SiteChip(
                        name = site.name,
                        isSelected = selectedSiteId == site.id,
                        onClick = { viewModel.updateSite(site.id) }
                    )
                }
            }

            if (selectedReportType == ReportType.YEARLY) {
                // Yearly Summary View
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    YearSelector(
                        selectedYear = selectedYear,
                        onYearChange = { viewModel.updateYear(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Year ${yearlySummaryReport.year} Overall Summary", 
                                    style = MaterialTheme.typography.titleSmall, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ReportStatRow("Total Attendance (Days)", "${yearlySummaryReport.totalPresent} Present | ${yearlySummaryReport.totalHalfDay} Half Day | ${yearlySummaryReport.totalAbsent} Absent")
                                ReportStatRow("Total Overtime Hours", "${yearlySummaryReport.totalOvertimeHours} hrs")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                ReportStatRow("Total Paid to Workers", Utils.formatCurrency(yearlySummaryReport.totalWorkerPaid), isBold = true)
                                ReportStatRow("Salary Paid", Utils.formatCurrency(yearlySummaryReport.salaryPaid))
                                ReportStatRow("Advances Paid", Utils.formatCurrency(yearlySummaryReport.advancePaid))
                                ReportStatRow("Bonuses Paid", Utils.formatCurrency(yearlySummaryReport.bonusPaid))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                ReportStatRow("Client Payments Received", Utils.formatCurrency(yearlySummaryReport.totalContractReceived), isBold = true, valueColor = Color(0xFF2E7D32))
                            }
                        }
                    }

                    item {
                        Text(
                            "Month-wise Breakdown (${yearlySummaryReport.year})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(yearlySummaryReport.monthSummaries) { monthSummary ->
                        MonthSummaryCard(monthSummary)
                    }
                }
            } else {
                // Date Selectors Section for Weekly & Monthly
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (selectedReportType == ReportType.WEEKLY) {
                        WeekSelector(
                            selectedWeekStartDate = selectedWeekStartDate,
                            onWeekChange = { viewModel.updateWeek(it) }
                        )
                    } else {
                        MonthSelector(
                            selectedMonth = selectedMonth,
                            onMonthChange = { viewModel.updateMonth(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SummarySection(
                    reports = currentReports,
                    title = if (selectedReportType == ReportType.WEEKLY) "Weekly Summary" else "Monthly Summary"
                )

                Text(
                    "Worker Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentReports) { report ->
                        ReportItem(report)
                    }

                    if (currentReports.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No data available for this selection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearSelector(
    selectedYear: Year,
    onYearChange: (Year) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onYearChange(selectedYear.minusYears(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                    contentDescription = "Previous Year",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Year ${selectedYear.value}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { onYearChange(selectedYear.plusYears(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                    contentDescription = "Next Year",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MonthSummaryCard(summary: MonthSummaryModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                summary.monthName, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "P: ${summary.presentDays} | H: ${summary.halfDays} | A: ${summary.absentDays} | OT: ${summary.overtimeHours}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Earned: ${Utils.formatCurrency(summary.totalEarned)}", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Paid: ${Utils.formatCurrency(summary.totalPaid)}", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Due: ${Utils.formatCurrency(summary.pendingBalance)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.pendingBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
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

@Composable
fun WeekSelector(
    selectedWeekStartDate: LocalDate,
    onWeekChange: (LocalDate) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()) }
    val startDateStr = selectedWeekStartDate.format(formatter)
    val endDateStr = selectedWeekStartDate.plusDays(6).format(formatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onWeekChange(selectedWeekStartDate.minusWeeks(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                    contentDescription = "Previous Week",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$startDateStr – $endDateStr",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Weekly Period (Mon - Sun)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onWeekChange(selectedWeekStartDate.plusWeeks(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                    contentDescription = "Next Week",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: java.time.YearMonth,
    onMonthChange: (java.time.YearMonth) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                    contentDescription = "Prev",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { onMonthChange(selectedMonth.plusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SummarySection(reports: List<WorkerMonthlyReport>, title: String = "Summary") {
    val totalEarned = reports.sumOf { it.totalEarned }
    val totalPaid = reports.sumOf { it.totalPaid }
    val totalPending = totalEarned - totalPaid

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title, 
                style = MaterialTheme.typography.titleSmall, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReportStatRow("Total Earnings", Utils.formatCurrency(totalEarned))
            ReportStatRow("Total Payments", Utils.formatCurrency(totalPaid))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp), 
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            ReportStatRow(
                label = "Net Due", 
                value = Utils.formatCurrency(totalPending), 
                isBold = true,
                valueColor = if (totalPending > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
fun ReportStatRow(label: String, value: String, isBold: Boolean = false, valueColor: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label, 
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            value, 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor ?: MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ReportItem(report: WorkerMonthlyReport) {
    val context = LocalContext.current
    val app = context.applicationContext as? WorkerApplication
    val isProUnlocked = app?.subscriptionRepository?.canAccessProFeatures() == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    report.workerName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        if (isProUnlocked) {
                            ShareUtils.shareWorkerReportToWhatsApp(context, report)
                        } else {
                            android.widget.Toast.makeText(context, "Exporting reports is a WorkCraft Pro feature.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share via WhatsApp",
                        tint = Color(0xFF25D366) // WhatsApp Green
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "P: ${report.presentDays} | H: ${report.halfDays} | A: ${report.absentDays} | OT: ${report.overtimeHours}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Earned: ${Utils.formatCurrency(report.totalEarned)}", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Paid: ${Utils.formatCurrency(report.totalPaid)}", 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Due: ${Utils.formatCurrency(report.pendingBalance)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (report.pendingBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
            }
        }
    }
}

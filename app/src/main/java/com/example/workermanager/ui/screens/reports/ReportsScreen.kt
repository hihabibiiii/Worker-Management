package com.example.workermanager.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.ReportViewModel
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.WorkerMonthlyReport
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportViewModel,
    siteViewModel: SiteViewModel
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val sites by siteViewModel.allSites.collectAsState()
    val reports by viewModel.monthlyReport.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Reports", fontWeight = FontWeight.Bold) },
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
            // Selectors Section
            Column(modifier = Modifier.padding(16.dp)) {
                MonthSelector(
                    selectedMonth = selectedMonth,
                    onMonthChange = { viewModel.updateMonth(it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
            
            SummarySection(reports)
            
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
                items(reports) { report ->
                    ReportItem(report)
                }
                
                if (reports.isEmpty()) {
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
fun SummarySection(reports: List<WorkerMonthlyReport>) {
    val totalEarned = reports.sumOf { it.totalEarned }
    val totalPaid = reports.sumOf { it.totalPaid }
    val totalPending = totalEarned - totalPaid

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Monthly Summary", 
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                report.workerName, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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

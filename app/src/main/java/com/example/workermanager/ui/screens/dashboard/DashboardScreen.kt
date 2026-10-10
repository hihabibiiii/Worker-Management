package com.example.workermanager.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.util.Utils
import com.example.workermanager.viewmodel.DashboardViewModel
import com.example.workermanager.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    siteViewModel: SiteViewModel,
    onAddWorker: () -> Unit,
    onMarkAttendance: () -> Unit,
    onMakePayment: () -> Unit,
    onSettingsClick: () -> Unit,
    onSiteClick: (Long) -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val sites by siteViewModel.allSites.collectAsState()
    val selectedSiteId by viewModel.selectedSiteId.collectAsState()
    val totalSiteWorkingDays by viewModel.totalSiteWorkingDays.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WorkCraft", fontWeight = FontWeight.Bold)
                        Text(
                            "Smart Work Management",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                Column(modifier = Modifier.padding(16.dp)) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Client Payments Summary
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Client Payments Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SummaryRow("Total Contracts", stats.totalContracts.toString())
                    SummaryRow("Total Receivable", Utils.formatCurrency(stats.totalContractReceivable))
                    SummaryRow("Total Received", Utils.formatCurrency(stats.totalContractReceived))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    SummaryRow(
                        label = "Total Remaining",
                        value = Utils.formatCurrency(stats.totalContractRemaining),
                        isBold = true,
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard("Add Worker", Icons.Default.PersonAdd, onAddWorker, Modifier.weight(1f))
                ActionCard("Attendance", Icons.Default.EventAvailable, onMarkAttendance, Modifier.weight(1f))
                ActionCard("Payment", Icons.Default.AccountBalanceWallet, onMakePayment, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package com.example.workermanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Sites : Screen("sites", "All Sites", Icons.Default.LocationOn)
    data object Workers : Screen("workers", "Workers", Icons.Default.Groups)
    data object Attendance : Screen("attendance", "Attendance", Icons.Default.History)
    data object Payments : Screen("payments", "Payments", Icons.Default.Payments)
    data object Contracts : Screen("contracts", "Contracts", Icons.Default.Assignment)
    data object SiteExpenses : Screen("site_expenses", "Site Expenses", Icons.Default.ReceiptLong)
    data object MaterialManagement : Screen("material_management", "Material Management", Icons.Default.Inventory2)
    data object Reports : Screen("reports", "Reports", Icons.Default.Assessment)
    data object RecycleBin : Screen("recycle_bin", "Recycle Bin", Icons.Default.DeleteSweep)
    
    data object SiteDetails : Screen("site_details/{siteId}", "Site Details") {
        fun createRoute(siteId: Long) = "site_details/$siteId"
    }
    
    data object WorkerDetails : Screen("worker_details/{workerId}", "Worker Details") {
        fun createRoute(workerId: Long) = "worker_details/$workerId"
    }

    data object ContractDetails : Screen("contract_details/{contractId}", "Contract Details") {
        fun createRoute(contractId: Long) = "contract_details/$contractId"
    }

    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

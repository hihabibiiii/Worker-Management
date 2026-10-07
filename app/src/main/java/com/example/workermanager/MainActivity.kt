package com.example.workermanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.workermanager.ui.navigation.Screen
import com.example.workermanager.ui.screens.attendance.AttendanceScreen
import com.example.workermanager.ui.screens.contracts.ContractDetailsScreen
import com.example.workermanager.ui.screens.contracts.ContractsScreen
import com.example.workermanager.ui.screens.dashboard.DashboardScreen
import com.example.workermanager.ui.screens.payments.PaymentsScreen
import com.example.workermanager.ui.screens.reports.ReportsScreen
import com.example.workermanager.ui.screens.settings.SettingsScreen
import com.example.workermanager.ui.screens.sites.SiteDetailsScreen
import com.example.workermanager.ui.screens.sites.SitesScreen
import com.example.workermanager.ui.screens.workers.WorkerDetailsScreen
import com.example.workermanager.ui.screens.workers.WorkersScreen
import com.example.workermanager.ui.theme.WorkerManagerTheme
import com.example.workermanager.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkerManagerTheme {
                val navController = rememberNavController()
                val app = application as WorkerApplication
                
                val siteViewModel: SiteViewModel = viewModel(
                    factory = SiteViewModelFactory(app.siteRepository)
                )
                val workerViewModel: WorkerViewModel = viewModel(
                    factory = WorkerViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository)
                )
                val attendanceViewModel: AttendanceViewModel = viewModel(
                    factory = AttendanceViewModelFactory(app.workerRepository, app.attendanceRepository)
                )
                val paymentViewModel: PaymentViewModel = viewModel(
                    factory = PaymentViewModelFactory(app.paymentRepository, app.workerRepository, app.siteRepository)
                )
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository, app.contractRepository, app.clientPaymentRepository)
                )
                val reportViewModel: ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository, app.contractRepository, app.clientPaymentRepository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository)
                )
                val contractViewModel: ContractViewModel = viewModel(
                    factory = ContractViewModelFactory(app.contractRepository, app.clientPaymentRepository, app.siteRepository)
                )

                val navItems = listOf(
                    Screen.Dashboard,
                    Screen.Sites,
                    Screen.Workers,
                    Screen.Attendance,
                    Screen.Payments,
                    Screen.Contracts,
                    Screen.Reports
                )

                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        // Only show bottom bar for main screens
                        if (navItems.any { it.route == currentDestination?.route }) {
                            NavigationBar {
                                navItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                                        label = { 
                                            Text(
                                                text = screen.title,
                                                maxLines = 1,
                                                softWrap = false,
                                                fontSize = 10.sp
                                            ) 
                                        },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                siteViewModel = siteViewModel,
                                onAddWorker = { navController.navigate(Screen.Workers.route) },
                                onMarkAttendance = { navController.navigate(Screen.Attendance.route) },
                                onMakePayment = { navController.navigate(Screen.Payments.route) },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                onSiteClick = { siteId ->
                                    navController.navigate(Screen.SiteDetails.createRoute(siteId))
                                }
                            )
                        }
                        composable(Screen.Sites.route) {
                            SitesScreen(
                                viewModel = siteViewModel,
                                onSiteClick = { siteId ->
                                    navController.navigate(Screen.SiteDetails.createRoute(siteId))
                                }
                            )
                        }
                        composable(
                            route = Screen.SiteDetails.route,
                            arguments = listOf(navArgument("siteId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val siteId = backStackEntry.arguments?.getLong("siteId") ?: 0L
                            SiteDetailsScreen(
                                siteId = siteId,
                                viewModel = siteViewModel,
                                workerViewModel = workerViewModel,
                                attendanceViewModel = attendanceViewModel,
                                paymentViewModel = paymentViewModel,
                                onBack = { navController.popBackStack() },
                                onWorkerClick = { workerId ->
                                    navController.navigate(Screen.WorkerDetails.createRoute(workerId))
                                }
                            )
                        }
                        composable(Screen.Workers.route) {
                            WorkersScreen(
                                viewModel = workerViewModel,
                                siteViewModel = siteViewModel,
                                onWorkerClick = { workerId ->
                                    navController.navigate(Screen.WorkerDetails.createRoute(workerId))
                                }
                            )
                        }
                        composable(
                            route = Screen.WorkerDetails.route,
                            arguments = listOf(navArgument("workerId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val workerId = backStackEntry.arguments?.getLong("workerId") ?: 0L
                            WorkerDetailsScreen(
                                workerId = workerId,
                                viewModel = workerViewModel,
                                attendanceViewModel = attendanceViewModel,
                                paymentViewModel = paymentViewModel,
                                siteViewModel = siteViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Attendance.route) {
                            AttendanceScreen(
                                viewModel = attendanceViewModel,
                                siteViewModel = siteViewModel
                            )
                        }
                        composable(Screen.Payments.route) {
                            PaymentsScreen(
                                viewModel = paymentViewModel,
                                siteViewModel = siteViewModel
                            )
                        }
                        composable(Screen.Contracts.route) {
                            val sitesList by siteViewModel.allSites.collectAsState(initial = emptyList())
                            ContractsScreen(
                                viewModel = contractViewModel,
                                sites = sitesList,
                                onContractClick = { contractId ->
                                    navController.navigate(Screen.ContractDetails.createRoute(contractId))
                                }
                            )
                        }
                        composable(
                            route = Screen.ContractDetails.route,
                            arguments = listOf(navArgument("contractId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val contractId = backStackEntry.arguments?.getLong("contractId") ?: 0L
                            ContractDetailsScreen(
                                contractId = contractId,
                                viewModel = contractViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Reports.route) {
                            ReportsScreen(
                                viewModel = reportViewModel,
                                siteViewModel = siteViewModel
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

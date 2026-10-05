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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.workermanager.ui.screens.dashboard.DashboardScreen
import com.example.workermanager.ui.screens.payments.PaymentsScreen
import com.example.workermanager.ui.screens.reports.ReportsScreen
import com.example.workermanager.ui.screens.settings.SettingsScreen
import com.example.workermanager.ui.screens.sites.SiteDetailsScreen
import com.example.workermanager.ui.screens.sites.SitesScreen
import com.example.workermanager.ui.screens.workers.WorkerDetailsScreen
import com.example.workermanager.ui.screens.workers.WorkersScreen
import com.example.workermanager.ui.theme.WorkerManagerTheme
import com.example.workermanager.viewmodel.AttendanceViewModel
import com.example.workermanager.viewmodel.AttendanceViewModelFactory
import com.example.workermanager.viewmodel.DashboardViewModel
import com.example.workermanager.viewmodel.DashboardViewModelFactory
import com.example.workermanager.viewmodel.PaymentViewModel
import com.example.workermanager.viewmodel.PaymentViewModelFactory
import com.example.workermanager.viewmodel.ReportViewModel
import com.example.workermanager.viewmodel.ReportViewModelFactory
import com.example.workermanager.viewmodel.SettingsViewModel
import com.example.workermanager.viewmodel.SettingsViewModelFactory
import com.example.workermanager.viewmodel.SiteViewModel
import com.example.workermanager.viewmodel.SiteViewModelFactory
import com.example.workermanager.viewmodel.WorkerViewModel
import com.example.workermanager.viewmodel.WorkerViewModelFactory

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
                    factory = DashboardViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository)
                )
                val reportViewModel: ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository)
                )

                val navItems = listOf(
                    Screen.Dashboard,
                    Screen.Sites,
                    Screen.Workers,
                    Screen.Attendance,
                    Screen.Payments,
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
                                        label = { Text(screen.title) },
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

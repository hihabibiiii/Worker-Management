package com.example.workermanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.example.workermanager.ui.screens.recyclebin.RecycleBinScreen
import com.example.workermanager.ui.screens.reports.ReportsScreen
import com.example.workermanager.ui.screens.settings.SettingsScreen
import com.example.workermanager.ui.screens.sites.SiteDetailsScreen
import com.example.workermanager.ui.screens.sites.SitesScreen
import com.example.workermanager.ui.screens.workers.WorkerDetailsScreen
import com.example.workermanager.ui.screens.workers.WorkersScreen
import com.example.workermanager.ui.theme.WorkerManagerTheme
import com.example.workermanager.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as WorkerApplication
            val themeMode by app.themePreferenceManager.themeMode.collectAsState()

            WorkerManagerTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                
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
                    factory = DashboardViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository, app.contractRepository, app.clientPaymentRepository, app.siteExpenseRepository)
                )
                val reportViewModel: ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository, app.contractRepository, app.clientPaymentRepository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository, app.themePreferenceManager)
                )
                val contractViewModel: ContractViewModel = viewModel(
                    factory = ContractViewModelFactory(app.contractRepository, app.clientPaymentRepository, app.siteRepository)
                )
                val recycleBinViewModel: RecycleBinViewModel = viewModel(
                    factory = RecycleBinViewModelFactory(
                        app.workerRepository,
                        app.siteRepository,
                        app.contractRepository,
                        app.paymentRepository,
                        app.clientPaymentRepository
                    )
                )
                val siteExpenseViewModel: SiteExpenseViewModel = viewModel(
                    factory = SiteExpenseViewModelFactory(app.siteExpenseRepository, app.siteRepository)
                )
                val contractorMaterialViewModel: ContractorMaterialViewModel = viewModel(
                    factory = ContractorMaterialViewModelFactory(app.contractorMaterialRepository, app.siteRepository)
                )

                val primaryNavItems = listOf(
                    Screen.Dashboard,
                    Screen.Workers,
                    Screen.Attendance,
                    Screen.Payments
                )
                
                val moreNavItems = listOf(
                    Screen.Sites,
                    Screen.Contracts,
                    Screen.SiteExpenses,
                    Screen.MaterialManagement,
                    Screen.Reports,
                    Screen.RecycleBin,
                    Screen.Settings
                )
                
                val allNavItems = primaryNavItems + moreNavItems

                var showMoreSheet by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState()

                val navigateToTab: (String) -> Unit = { targetRoute ->
                    val startDestinationRoute = navController.graph.findStartDestination().route
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = (targetRoute != startDestinationRoute)
                    }
                }

                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        // Only show bottom bar for main screens
                        if (allNavItems.any { it.route == currentDestination?.route }) {
                            val isMoreSelected = moreNavItems.any { currentDestination?.hierarchy?.any { h -> h.route == it.route } == true }
                            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                            val navBg = if (isDark) Color(0xFF1E222A) else Color.White
                            val navBorder = if (isDark) Color(0xFF333A48) else Color(0xFFCBD5E1)
                            val unselectedContentColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF526581)
                            val selectedBg = Color(0xFF0866E8)
                            val selectedContentColor = Color.White

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(32.dp),
                                color = navBg,
                                border = BorderStroke(1.dp, navBorder),
                                shadowElevation = 8.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    val totalTabs = primaryNavItems.size + 1
                                    val targetSelectedIndex = if (isMoreSelected) {
                                        primaryNavItems.size
                                    } else {
                                        primaryNavItems.indexOfFirst { screen ->
                                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                        }.takeIf { it >= 0 } ?: 0
                                    }

                                    val animatedIndex by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = targetSelectedIndex.toFloat(),
                                        animationSpec = androidx.compose.animation.core.spring(
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
                                        ),
                                        label = "animatedIndex"
                                    )

                                    BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                                        val tabWidth = maxWidth / totalTabs

                                        Box(
                                            modifier = Modifier
                                                .offset {
                                                    androidx.compose.ui.unit.IntOffset(
                                                        (tabWidth * animatedIndex).roundToPx(),
                                                        0
                                                    )
                                                }
                                                .width(tabWidth)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(selectedBg)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        primaryNavItems.forEachIndexed { index, screen ->
                                            val selectionFraction = (1.0f - kotlin.math.abs(animatedIndex - index)).coerceIn(0f, 1f)

                                            AnimatedNavItem(
                                                title = screen.title,
                                                icon = screen.icon,
                                                selectionFraction = selectionFraction,
                                                selectedContentColor = selectedContentColor,
                                                unselectedContentColor = unselectedContentColor,
                                                onClick = { navigateToTab(screen.route) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // More Tab
                                        val moreSelectionFraction = (1.0f - kotlin.math.abs(animatedIndex - primaryNavItems.size)).coerceIn(0f, 1f)

                                        AnimatedNavItem(
                                            title = "More",
                                            icon = Icons.Default.GridView,
                                            selectionFraction = moreSelectionFraction,
                                            selectedContentColor = selectedContentColor,
                                            unselectedContentColor = unselectedContentColor,
                                            onClick = { showMoreSheet = true },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
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
                                workerViewModel = workerViewModel,
                                contractViewModel = contractViewModel,
                                siteExpenseViewModel = siteExpenseViewModel,
                                onAddWorker = { navigateToTab(Screen.Workers.route) },
                                onMarkAttendance = { navigateToTab(Screen.Attendance.route) },
                                onNavigateToRecycleBin = { navController.navigate(Screen.RecycleBin.route) },
                                onNavigateToReports = { navigateToTab(Screen.Reports.route) },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                onSiteClick = { siteId ->
                                    navController.navigate(Screen.SiteDetails.createRoute(siteId))
                                }
                            )
                        }
                        composable(Screen.Sites.route) {
                            SitesScreen(
                                viewModel = siteViewModel,
                                onBack = { navController.popBackStack() },
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
                                onBack = { navController.popBackStack() },
                                onContractClick = { contractId ->
                                    navController.navigate(Screen.ContractDetails.createRoute(contractId))
                                }
                            )
                        }
                        composable(Screen.SiteExpenses.route) {
                            com.example.workermanager.ui.screens.expenses.SiteExpensesScreen(
                                viewModel = siteExpenseViewModel,
                                siteViewModel = siteViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.MaterialManagement.route) {
                            com.example.workermanager.ui.screens.materials.ContractorMaterialScreen(
                                viewModel = contractorMaterialViewModel,
                                siteViewModel = siteViewModel,
                                onBack = { navController.popBackStack() }
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
                                siteViewModel = siteViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.RecycleBin.route) {
                            RecycleBinScreen(
                                viewModel = recycleBinViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenRecycleBin = { navController.navigate(Screen.RecycleBin.route) }
                            )
                        }
                    }
                    
                    if (showMoreSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showMoreSheet = false },
                            sheetState = sheetState
                        ) {
                            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                                moreNavItems.forEach { screen ->
                                    ListItem(
                                        headlineContent = { Text(screen.title) },
                                        leadingContent = { 
                                            screen.icon?.let { Icon(it, contentDescription = null) } 
                                        },
                                        modifier = Modifier.clickable {
                                            showMoreSheet = false
                                            navigateToTab(screen.route)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedNavItem(
    title: String,
    icon: ImageVector?,
    selectionFraction: Float,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = androidx.compose.ui.graphics.lerp(
        unselectedContentColor,
        selectedContentColor,
        selectionFraction
    )

    val scale = 1.0f + 0.12f * selectionFraction
    val isSelected = selectionFraction > 0.5f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

package com.example.workermanager

import android.app.Activity
import android.os.Bundle
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
                val contractViewModel: ContractViewModel = viewModel(
                    factory = ContractViewModelFactory(app.contractRepository, app.clientPaymentRepository, app.siteRepository)
                )
                val siteExpenseViewModel: SiteExpenseViewModel = viewModel(
                    factory = SiteExpenseViewModelFactory(app.siteExpenseRepository, app.siteRepository)
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

                var showMoreSheet by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

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

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val currentRoute = currentDestination?.route

                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val screenBg = when {
                    isDark -> MaterialTheme.colorScheme.background
                    currentRoute == Screen.Dashboard.route -> Color(0xFFF7F9FE)
                    currentRoute == Screen.Workers.route ||
                            currentRoute == Screen.Payments.route ||
                            currentRoute == Screen.MaterialManagement.route -> Color(0xFFF1F6FD)
                    else -> MaterialTheme.colorScheme.background
                }

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as? Activity)?.window
                        window?.let {
                            it.statusBarColor = Color.Transparent.toArgb()
                            it.navigationBarColor = screenBg.toArgb()
                            val insetsController = WindowCompat.getInsetsController(it, view)
                            insetsController.isAppearanceLightStatusBars = false
                            insetsController.isAppearanceLightNavigationBars = !isDark
                        }
                    }
                }

                Scaffold(
                    containerColor = screenBg,
                    bottomBar = {}
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route,
                            modifier = Modifier.fillMaxSize()
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
                                val contractorMaterialViewModel: ContractorMaterialViewModel = viewModel(
                                    factory = ContractorMaterialViewModelFactory(app.contractorMaterialRepository, app.siteRepository)
                                )
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
                                val reportViewModel: ReportViewModel = viewModel(
                                    factory = ReportViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository, app.contractRepository, app.clientPaymentRepository)
                                )
                                ReportsScreen(
                                    viewModel = reportViewModel,
                                    siteViewModel = siteViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.RecycleBin.route) {
                                val recycleBinViewModel: RecycleBinViewModel = viewModel(
                                    factory = RecycleBinViewModelFactory(
                                        app.workerRepository,
                                        app.siteRepository,
                                        app.contractRepository,
                                        app.paymentRepository,
                                        app.clientPaymentRepository
                                    )
                                )
                                RecycleBinScreen(
                                    viewModel = recycleBinViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Settings.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = SettingsViewModelFactory(app.workerRepository, app.attendanceRepository, app.paymentRepository, app.themePreferenceManager)
                                )
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenRecycleBin = { navController.navigate(Screen.RecycleBin.route) }
                                )
                            }
                        }

                        if (allNavItems.any { it.route == currentRoute }) {
                            val isMoreSelected = showMoreSheet || moreNavItems.any { currentDestination?.hierarchy?.any { h -> h.route == it.route } == true }

                            val unselectedContentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF526581)
                            val selectedContentColor = Color.White
                            val indicatorColor = Color(0xFF0866E8)

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Solid, premium floating navbar Surface
                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .widthIn(max = 420.dp)
                                        .fillMaxWidth()
                                        .height(70.dp),
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF1E2638) else Color.White,
                                    shadowElevation = 10.dp,
                                    tonalElevation = 2.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
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
                                                    .clip(CircleShape)
                                                    .background(indicatorColor)
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
                                                    onClick = {
                                                        if (showMoreSheet) showMoreSheet = false
                                                        navigateToTab(screen.route)
                                                    },
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
                                                onClick = { showMoreSheet = !showMoreSheet },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }


                        androidx.compose.animation.AnimatedVisibility(
                            visible = showMoreSheet,
                            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            androidx.activity.compose.BackHandler { showMoreSheet = false }

                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showMoreSheet = false }
                                )

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp,
                                    shadowElevation = 16.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp, bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                .align(Alignment.CenterHorizontally)
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "More Options",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        moreNavItems.forEach { screen ->
                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        screen.title,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                leadingContent = {
                                                    screen.icon?.let {
                                                        Icon(
                                                            it,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
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
            .clip(CircleShape)
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

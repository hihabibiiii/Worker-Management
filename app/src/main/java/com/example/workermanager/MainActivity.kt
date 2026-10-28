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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(app.authPreferenceManager)
                )
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                val startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route

                val primaryNavItems = listOf(
                    Screen.Dashboard,
                    Screen.Workers,
                    Screen.Attendance,
                    Screen.Payments
                )

                val moreNavItems = listOf(
                    Screen.Profile,
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
                            startDestination = startDestination,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Screen.Login.route) {
                                com.example.workermanager.ui.screens.auth.LoginScreen(
                                    authViewModel = authViewModel,
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Profile.route) {
                                com.example.workermanager.ui.screens.profile.ProfileScreen(
                                    authViewModel = authViewModel,
                                    onBack = { navController.popBackStack() },
                                    onLogout = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
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
                                    onOpenRecycleBin = { navController.navigate(Screen.RecycleBin.route) },
                                    onOpenProfile = { navController.navigate(Screen.Profile.route) }
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
                                        .padding(horizontal = 10.dp)
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
                                            .padding(horizontal = 2.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            primaryNavItems.forEach { screen ->
                                                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                                                AnimatedNavItem(
                                                    title = screen.title,
                                                    icon = screen.icon,
                                                    isSelected = isSelected,
                                                    selectedContentColor = selectedContentColor,
                                                    unselectedContentColor = unselectedContentColor,
                                                    indicatorColor = indicatorColor,
                                                    onClick = {
                                                        if (showMoreSheet) showMoreSheet = false
                                                        navigateToTab(screen.route)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            // More Tab
                                            AnimatedNavItem(
                                                title = "More",
                                                icon = Icons.Default.GridView,
                                                isSelected = isMoreSelected,
                                                selectedContentColor = selectedContentColor,
                                                unselectedContentColor = unselectedContentColor,
                                                indicatorColor = indicatorColor,
                                                onClick = { showMoreSheet = !showMoreSheet },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showMoreSheet) {
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
                                        .widthIn(max = 420.dp)
                                        .navigationBarsPadding()
                                        .padding(bottom = 88.dp, start = 14.dp, end = 14.dp),
                                    shape = RoundedCornerShape(26.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp,
                                    shadowElevation = 16.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(start = 4.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Spacious single vertical column: all 7 options top-to-bottom simultaneously
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            moreNavItems.forEach { screen ->
                                                val isProfile = screen == Screen.Profile
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(if (isProfile) 56.dp else 48.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable {
                                                            showMoreSheet = false
                                                            navigateToTab(screen.route)
                                                        },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = if (isProfile) {
                                                        if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color(0xFFEBF3FF)
                                                    } else Color.Transparent,
                                                    border = if (isProfile) {
                                                        BorderStroke(1.dp, Color(0xFF1677F8).copy(alpha = 0.3f))
                                                    } else null
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(if (isProfile) 36.dp else 32.dp)
                                                                .background(
                                                                    if (isProfile) Color(0xFF1677F8) else (if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F6FD)),
                                                                    CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            screen.icon?.let {
                                                                Icon(
                                                                    imageVector = it,
                                                                    contentDescription = null,
                                                                    tint = if (isProfile) Color.White else MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(if (isProfile) 20.dp else 18.dp)
                                                                )
                                                            }
                                                        }

                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Text(
                                                                text = screen.title,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isProfile) FontWeight.Bold else FontWeight.SemiBold,
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (isProfile) {
                                                                Text(
                                                                    text = "Manage your account",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                            contentDescription = null,
                                                            tint = if (isProfile) Color(0xFF1677F8) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(20.dp)
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
        }
    }
}

@Composable
fun AnimatedNavItem(
    title: String,
    icon: ImageVector?,
    isSelected: Boolean,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    indicatorColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isSelected) selectedContentColor else unselectedContentColor
    val labelColor = if (isSelected) indicatorColor else unselectedContentColor

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp, horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Instant Highlight container ONLY behind the icon!
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) indicatorColor else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Text label sits below with NO highlight pill/background behind it
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Visible
            )
        }
    }
}

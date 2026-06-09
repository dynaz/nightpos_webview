package com.nightpos.app.ui.pos

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nightpos.app.data.repository.OdooRepository
import com.nightpos.app.ui.pos.activity.PosActivityScreen
import com.nightpos.app.ui.pos.activity.PosActivityViewModel
import com.nightpos.app.ui.pos.home.PosHomeScreen
import com.nightpos.app.ui.pos.home.PosHomeViewModel
import com.nightpos.app.ui.pos.notification.PosNotificationScreen
import com.nightpos.app.ui.pos.reports.PosReportsScreen

private enum class PosTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("pos_home", "Dashboard", Icons.Filled.Dashboard),
    Reports("pos_reports", "Reports", Icons.Filled.BarChart),
    Activity("pos_activity", "Activity", Icons.Filled.List),
    Notification("pos_notification", "Notification", Icons.Filled.Notifications),
}

@Composable
fun PosMainScreen(
    employeeName: String,
    isManager: Boolean,
    repository: OdooRepository,
    baseUrl: String,
    onBack: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val tabs = if (isManager) PosTab.entries else listOf(PosTab.Dashboard, PosTab.Activity, PosTab.Notification)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PosTab.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(PosTab.Dashboard.route) {
                val vm = remember { PosHomeViewModel(repository) }
                PosHomeScreen(viewModel = vm, shopName = employeeName)
            }
            composable(PosTab.Reports.route) {
                PosReportsScreen(baseUrl = baseUrl)
            }
            composable(PosTab.Activity.route) {
                val vm = remember { PosActivityViewModel(repository) }
                PosActivityScreen(viewModel = vm)
            }
            composable(PosTab.Notification.route) {
                PosNotificationScreen()
            }
        }
    }
}

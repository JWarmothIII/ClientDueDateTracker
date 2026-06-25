@file:Suppress("ktlint:standard:function-naming")

package dev.jwarmothiii.clientduedatetracker.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui.DashboardScreen

@Composable
fun ClientDueDateTrackerNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Dashboard.route,
        modifier = modifier,
    ) {
        composable(route = AppRoute.Dashboard.route) {
            DashboardScreen()
        }
    }
}

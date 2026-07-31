@file:Suppress("ktlint:standard:function-naming")

package dev.jwarmothiii.clientduedatetracker.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.jwarmothiii.clientduedatetracker.domain.client.ui.OnboardingScreen
import dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui.DashboardScreen

@Composable
fun ClientDueDateTrackerNavigation(
    notificationsDenied: Boolean,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Dashboard.route,
        modifier = modifier,
    ) {
        composable(route = AppRoute.Dashboard.route) {
            DashboardScreen(
                onAddClient = { navController.navigate(AppRoute.Onboarding.route) },
                onOpenNotificationSettings = onOpenNotificationSettings,
                notificationsDenied = notificationsDenied,
            )
        }
        composable(route = AppRoute.Onboarding.route) {
            OnboardingScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}

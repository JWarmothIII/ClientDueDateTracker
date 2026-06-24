package dev.jwarmothiii.clientduedatetracker.shared.navigation

sealed interface AppRoute {
    val route: String

    data object Dashboard : AppRoute {
        override val route: String = "dashboard"
    }
}

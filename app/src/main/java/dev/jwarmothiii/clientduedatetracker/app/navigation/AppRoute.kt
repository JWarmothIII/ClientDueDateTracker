package dev.jwarmothiii.clientduedatetracker.app.navigation

sealed interface AppRoute {
    val route: String

    data object Dashboard : AppRoute {
        override val route: String = "dashboard"
    }

    data object Onboarding : AppRoute {
        override val route: String = "onboarding"
    }
}

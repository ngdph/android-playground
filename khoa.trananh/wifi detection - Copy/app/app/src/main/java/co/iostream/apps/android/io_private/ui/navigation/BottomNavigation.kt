package co.iostream.apps.android.io_private.ui.navigation

import co.iostream.apps.android.io_private.R

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(
        name = "Home",
        route = MainNavigatorGraph.FileManager,
        icon = R.drawable.baseline_home_24,
    ),
    BottomNavItem(
        name = "Settings",
        route = SettingsNavigatorGraph.Settings,
        icon = R.drawable.baseline_settings_24,
    ),
)
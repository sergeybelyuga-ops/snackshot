package app.snackshot.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.snackshot.ui.R
import app.snackshot.ui.privacy.PrivacyDetailsScreen
import app.snackshot.ui.tabs.DashboardScreen
import app.snackshot.ui.tabs.HistoryScreen
import app.snackshot.ui.tabs.HomeScreen
import app.snackshot.ui.tabs.SettingsScreen
import app.snackshot.ui.welcome.WelcomeScreen
import kotlin.reflect.KClass

internal enum class Tab(val route: Any, @StringRes val label: Int, @DrawableRes val icon: Int, @DrawableRes val selectedIcon: Int) {
    HOME(HomeRoute, R.string.nav_home, R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
    DASHBOARD(DashboardRoute, R.string.nav_dashboard, R.drawable.ic_nav_bar_chart, R.drawable.ic_nav_bar_chart_filled),
    HISTORY(HistoryRoute, R.string.nav_history, R.drawable.ic_nav_history, R.drawable.ic_nav_history_filled),
    SETTINGS(SettingsRoute, R.string.nav_settings, R.drawable.ic_nav_settings, R.drawable.ic_nav_settings_filled),
    ;

    val routeClass: KClass<*> get() = route::class
}

/**
 * Global navigation (doc 10 §2.2): S-01 Welcome → S-02 Home; Home, Dashboard, History and
 * Settings as NavigationBar tabs; S-01A Privacy details from Welcome and Settings.
 *
 * @param welcomeCompleted decides the start destination once; it is not observed afterwards.
 */
@Composable
fun SnackshotNavHost(
    welcomeCompleted: Boolean,
    onWelcomeCompleted: () -> Unit,
    environmentName: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hasRoute(tab.routeClass) == true }

    Column(modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (welcomeCompleted) HomeRoute else WelcomeRoute,
            modifier = Modifier.weight(1f),
            // Screens switch instantly: no fade or slide between destinations, including predictive Back.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            sizeTransform = null,
        ) {
            composable<WelcomeRoute> {
                WelcomeScreen(
                    onContinue = {
                        onWelcomeCompleted()
                        navController.navigate(HomeRoute) { popUpTo<WelcomeRoute> { inclusive = true } }
                    },
                    onPrivacyDetails = { navController.navigate(PrivacyDetailsRoute) },
                )
            }
            composable<PrivacyDetailsRoute> { PrivacyDetailsScreen(onBack = { navController.popBackStack() }) }
            composable<HomeRoute> { HomeScreen() }
            composable<DashboardRoute> { DashboardScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<SettingsRoute> {
                SettingsScreen(environmentName = environmentName, onPrivacyDetails = { navController.navigate(PrivacyDetailsRoute) })
            }
        }
        if (currentTab != null) {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTab(tab) },
                        icon = { Icon(painterResource(if (selected) tab.selectedIcon else tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        }
    }
}

/** Standard tab switching: one copy of each tab, state saved and restored, Back from a tab returns to Home. */
private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

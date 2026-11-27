package app.snackshot.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.ui.R
import app.snackshot.ui.capture.CameraRoute as CameraScreenRoute
import app.snackshot.ui.capture.RecognitionRoute as RecognitionScreenRoute
import app.snackshot.ui.goals.GoalDetailRoute as GoalDetailScreenRoute
import app.snackshot.ui.goals.NutritionPreferencesRoute as NutritionPreferencesScreenRoute
import app.snackshot.ui.home.HomeRoute
import app.snackshot.ui.privacy.PrivacyDetailsScreen
import app.snackshot.ui.quickadd.QuickAddRoute as QuickAddScreenRoute
import app.snackshot.ui.review.MealReviewRoute as MealReviewScreenRoute
import app.snackshot.ui.settings.SettingsScreen
import app.snackshot.ui.tabs.DashboardScreen
import app.snackshot.ui.tabs.HistoryScreen
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
    viewModels: ScreenViewModels = HiltScreenViewModels,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hasRoute(tab.routeClass) == true }

    Column(modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (welcomeCompleted) HomeRoute else WelcomeRoute,
            modifier = Modifier.weight(1f),
            // Screens switch instantly: no fade, slide or scale between destinations.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            // The Back gesture has its own transitions with a non-empty default; switch them off too.
            predictivePopEnterTransition = { EnterTransition.None },
            predictivePopExitTransition = { ExitTransition.None },
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
            composable<HomeRoute> { entry ->
                val saved by entry.savedStateHandle.getStateFlow<String?>(SAVED_MEAL_ID, null).collectAsStateWithLifecycle()
                HomeRoute(
                    viewModel = viewModels.home(),
                    savedMealId = saved?.let(::MealId),
                    onSavedMealConsumed = { entry.savedStateHandle[SAVED_MEAL_ID] = null },
                    onManualAdd = { navController.navigate(QuickAddRoute()) },
                    onTakePhoto = { navController.navigate(CameraRoute) },
                    onPicked = { uri -> navController.navigate(RecognitionRoute(MealSource.GALLERY.name, pickedUri = uri.toString())) },
                    onOpenDashboard = { navController.navigateToTab(Tab.DASHBOARD) },
                    onOpenHistory = { navController.navigateToTab(Tab.HISTORY) },
                )
            }
            composable<NutritionPreferencesRoute> {
                NutritionPreferencesScreenRoute(
                    viewModel = viewModels.nutritionPreferences(),
                    onGoal = { navController.navigate(GoalRoute(it.name)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<GoalRoute> {
                GoalDetailScreenRoute(
                    viewModel = viewModels.goalDetail(),
                    onDone = { navController.popBackStack<NutritionPreferencesRoute>(inclusive = false) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<QuickAddRoute> {
                QuickAddScreenRoute(
                    viewModel = viewModels.quickAdd(),
                    onSaved = { id -> navController.backToHomeWith(id) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<CameraRoute> {
                val gallery = rememberGalleryPicker { uri ->
                    navController.navigate(RecognitionRoute(MealSource.GALLERY.name, pickedUri = uri.toString())) {
                        popUpTo<CameraRoute> { inclusive = true }
                    }
                }
                CameraScreenRoute(
                    viewModel = viewModels.camera(),
                    onCaptured = { file ->
                        navController.navigate(RecognitionRoute(MealSource.PHOTO.name, capturePath = file.path)) {
                            popUpTo<CameraRoute> { inclusive = true }
                        }
                    },
                    onGallery = gallery,
                    onClose = { navController.popBackStack() },
                )
            }
            composable<RecognitionRoute> {
                val viewModel = viewModels.recognition()
                val gallery = rememberGalleryPicker { uri ->
                    navController.navigate(RecognitionRoute(MealSource.GALLERY.name, pickedUri = uri.toString())) {
                        popUpTo<HomeRoute> { inclusive = false }
                    }
                }
                RecognitionScreenRoute(
                    viewModel = viewModel,
                    onRecognized = {
                        navController.navigate(MealReviewRoute) { popUpTo<RecognitionRoute> { inclusive = true } }
                    },
                    // "Cancel and keep draft": the pending photo stays on the device.
                    onClose = { navController.popBackStack<HomeRoute>(inclusive = false) },
                    onQuickAdd = { navController.navigate(QuickAddRoute(fromDraft = true)) },
                    onRetake = {
                        viewModel.discard {
                            navController.navigate(CameraRoute) { popUpTo<HomeRoute> { inclusive = false } }
                        }
                    },
                    onGallery = { viewModel.discard(gallery) },
                )
            }
            composable<MealReviewRoute> {
                MealReviewScreenRoute(
                    viewModel = viewModels.review(),
                    onSaved = { id -> navController.backToHomeWith(id) },
                    onClosed = { navController.popBackStack<HomeRoute>(inclusive = false) },
                    onRetake = { navController.navigate(CameraRoute) { popUpTo<HomeRoute> { inclusive = false } } },
                    onQuickAdd = { navController.navigate(QuickAddRoute(fromDraft = true)) },
                )
            }
            composable<DashboardRoute> { DashboardScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<SettingsRoute> {
                val goals by viewModels.nutritionPreferences().goals.collectAsStateWithLifecycle()
                SettingsScreen(
                    goals = goals,
                    environmentName = environmentName,
                    onPrivacyDetails = { navController.navigate(PrivacyDetailsRoute) },
                    onNutritionPreferences = { navController.navigate(NutritionPreferencesRoute) },
                )
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

/** Set on Home's back stack entry by a screen that saved a meal, so Home can highlight it. */
internal const val SAVED_MEAL_ID = "saved_meal_id"

/** Back to Home, which highlights the meal just saved (doc 10 §4.4). */
private fun NavHostController.backToHomeWith(id: MealId) {
    getBackStackEntry<HomeRoute>().savedStateHandle[SAVED_MEAL_ID] = id.value
    popBackStack<HomeRoute>(inclusive = false)
}

/** S-04: the system Photo Picker, images only; no storage permission is needed. */
@Composable
private fun rememberGalleryPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Standard tab switching: one copy of each tab, state saved and restored, Back from a tab returns to Home. */
private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

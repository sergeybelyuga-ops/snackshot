package app.snackshot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.snackshot.ui.goals.GoalDetailViewModel
import app.snackshot.ui.goals.NutritionPreferencesViewModel
import app.snackshot.ui.home.HomeViewModel
import app.snackshot.ui.quickadd.QuickAddViewModel

/**
 * Where screens get their ViewModels: Hilt in the app, plain constructors with fakes in tests.
 * Each call is scoped to the current navigation destination.
 */
interface ScreenViewModels {
    @Composable
    fun home(): HomeViewModel

    @Composable
    fun quickAdd(): QuickAddViewModel

    @Composable
    fun nutritionPreferences(): NutritionPreferencesViewModel

    @Composable
    fun goalDetail(): GoalDetailViewModel
}

object HiltScreenViewModels : ScreenViewModels {
    @Composable
    override fun home(): HomeViewModel = hiltViewModel()

    @Composable
    override fun quickAdd(): QuickAddViewModel = hiltViewModel()

    @Composable
    override fun nutritionPreferences(): NutritionPreferencesViewModel = hiltViewModel()

    @Composable
    override fun goalDetail(): GoalDetailViewModel = hiltViewModel()
}

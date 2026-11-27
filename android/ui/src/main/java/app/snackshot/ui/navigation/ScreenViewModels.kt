package app.snackshot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
}

object HiltScreenViewModels : ScreenViewModels {
    @Composable
    override fun home(): HomeViewModel = hiltViewModel()

    @Composable
    override fun quickAdd(): QuickAddViewModel = hiltViewModel()
}

package app.snackshot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.ui.navigation.SnackshotNavHost
import app.snackshot.ui.start.AppStartViewModel
import app.snackshot.ui.start.StartState
import app.snackshot.ui.welcome.StartErrorScreen
import app.snackshot.ui.welcome.StartLoadingScreen

/** App root: waits for the start decision, then shows the navigation host. */
@Composable
fun SnackshotApp(environmentName: String, modifier: Modifier = Modifier, viewModel: AppStartViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        StartState.Loading -> StartLoadingScreen(modifier)
        StartState.StorageError -> StartErrorScreen(onRetry = viewModel::retry, modifier = modifier)
        is StartState.Ready -> SnackshotNavHost(
            welcomeCompleted = s.welcomeCompleted,
            onWelcomeCompleted = viewModel::completeWelcome,
            environmentName = environmentName,
            modifier = modifier,
        )
    }
}

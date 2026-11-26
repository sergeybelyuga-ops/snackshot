package app.snackshot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.snackshot.domain.EnvironmentConfig
import app.snackshot.ui.PlaceholderScreen
import app.snackshot.ui.theme.SnackshotTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var environment: EnvironmentConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnackshotTheme {
                PlaceholderScreen(environmentName = environment.name)
            }
        }
    }
}

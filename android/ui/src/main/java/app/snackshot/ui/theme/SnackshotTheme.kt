package app.snackshot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Snackshot theme: Material 3 roles, Health typography, shapes, and the extra
 * chart/macro/status colors. Follows the system light/dark setting; no dynamic
 * color, so the brand blue and chart colors stay stable.
 */
@Composable
fun SnackshotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSnackshotColors provides if (darkTheme) DarkSnackshotColors else LightSnackshotColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = HealthTypography,
            shapes = SnackshotShapes,
            content = content,
        )
    }
}

/** Access to the non-Material colors: `SnackshotTheme.colors.macroProtein`. */
object SnackshotTheme {
    val colors: SnackshotColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSnackshotColors.current
}

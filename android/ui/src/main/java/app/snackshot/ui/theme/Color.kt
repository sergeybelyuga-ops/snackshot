package app.snackshot.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Material 3 color roles: docs/Architecture/10_architecture_ux_spec.md §3.1,
// completed from docs/UX_UI/Prototype/prototype-theme.css (onPrimaryContainer, outlineVariant).
// Roles not listed there keep the Material 3 baseline.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF075985),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E9FA),
    onPrimaryContainer = Color(0xFF202124),
    secondary = Color(0xFF5D6165),
    // Selection indicators (NavigationBar, chips) use the sky-blue family, not the M3 baseline lavender.
    secondaryContainer = Color(0xFFC9E9FA),
    onSecondaryContainer = Color(0xFF073B58),
    tertiary = Color(0xFF356B45),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF202124),
    surface = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F3F4),
    onSurface = Color(0xFF202124),
    onSurfaceVariant = Color(0xFF5F6368),
    error = Color(0xFFB3261E),
    outline = Color(0xFF74777A),
    outlineVariant = Color(0xFFC4C7C5),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF79C8F5),
    onPrimary = Color(0xFF073B58),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFC8C9CB),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF79D889),
    background = Color(0xFF121314),
    onBackground = Color(0xFFF1F3F4),
    surface = Color(0xFF121314),
    surfaceContainer = Color(0xFF1E2022),
    surfaceContainerHigh = Color(0xFF292B2E),
    onSurface = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFFC5C7C9),
    error = Color(0xFFFFB4AB),
    outline = Color(0xFF8E918F),
    outlineVariant = Color(0xFF444746),
)

/** A badge: background + foreground. Always shown with a text label, never color alone (doc 10 §3.1, §5.2). */
@Immutable
data class StatusColors(val container: Color, val content: Color)

/** Colors outside the Material 3 roles: charts, macros, range status. */
@Immutable
data class SnackshotColors(
    val chartIntake: Color,
    val chartInRange: Color,
    val chartAboveRange: Color,
    val chartBelowRange: Color,
    val chartWarning: Color,
    val macroCarbs: Color,
    val macroFat: Color,
    val macroProtein: Color,
    val macroTrack: Color,
    val statusBelow: StatusColors,
    val statusInRange: StatusColors,
    val statusAbove: StatusColors,
    val statusUnderTarget: StatusColors,
)

internal val LightSnackshotColors = SnackshotColors(
    chartIntake = Color(0xFF32C5F0),
    chartInRange = Color(0xFF6FD77C),
    chartAboveRange = Color(0xFFFF9A68),
    chartBelowRange = Color(0xFF8CB5FF),
    chartWarning = Color(0xFFFFC15A),
    macroCarbs = Color(0xFF2D9CDB),
    macroFat = Color(0xFFE5675C),
    macroProtein = Color(0xFFC77700),
    macroTrack = Color(0xFFE3E5E8),
    statusBelow = StatusColors(Color(0xFFFFF0C7), Color(0xFF6B4E00)),
    statusInRange = StatusColors(Color(0xFFD6F5DA), Color(0xFF1E5B2A)),
    statusAbove = StatusColors(Color(0xFFFFE1D2), Color(0xFF8A3A10)),
    statusUnderTarget = StatusColors(Color(0xFFD3EEF8), Color(0xFF0B4F63)),
)

internal val DarkSnackshotColors = SnackshotColors(
    chartIntake = Color(0xFF32C5F0),
    chartInRange = Color(0xFF6FD77C),
    chartAboveRange = Color(0xFFFF9A68),
    chartBelowRange = Color(0xFF8CB5FF),
    chartWarning = Color(0xFFFFC15A),
    macroCarbs = Color(0xFF8FD3FF),
    macroFat = Color(0xFFFF8A85),
    macroProtein = Color(0xFFFFBA66),
    macroTrack = Color(0xFF2A2C2F),
    statusBelow = StatusColors(Color(0xFF5A4300), Color(0xFFFFD27A)),
    statusInRange = StatusColors(Color(0xFF1E4A27), Color(0xFF8EE29A)),
    statusAbove = StatusColors(Color(0xFF5A2A12), Color(0xFFFFB38A)),
    statusUnderTarget = StatusColors(Color(0xFF0B4A5C), Color(0xFF8FDDF5)),
)

val LocalSnackshotColors = staticCompositionLocalOf { LightSnackshotColors }

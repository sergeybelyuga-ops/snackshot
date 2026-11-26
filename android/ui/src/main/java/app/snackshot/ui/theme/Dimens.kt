package app.snackshot.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Spacing, shapes, elevation: docs/Architecture/10_architecture_ux_spec.md §3.4.

object Spacing {
    /** Base grid. */
    val grid = 4.dp
    val small = 8.dp
    val medium = 12.dp

    /** Standard content padding. */
    val content = 16.dp

    /** Between sections. */
    val section = 24.dp
}

object Size {
    /** Minimum touch target, including icon-only buttons (doc 10 §5.1). */
    val minTouchTarget = 48.dp

    /** Bottom inset that keeps content clear of the NavigationBar. */
    val navigationBarInset = 64.dp
}

object Elevation {
    val page = 0.dp
    val card = 1.dp
    val fab = 3.dp
    val dialog = 6.dp
    val menu = 8.dp
}

val SnackshotShapes = Shapes(
    // Text fields and compact chips.
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    // Cards, grouped rows, bottom sheets.
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    // Full-height modal sheets.
    extraLarge = RoundedCornerShape(28.dp),
)

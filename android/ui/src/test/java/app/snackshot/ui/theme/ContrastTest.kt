package app.snackshot.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.2 AA contrast for the theme tokens (doc 10 §5.2):
 * 4.5:1 for normal text, 3:1 for large text and essential graphics.
 */
class ContrastTest {
    private fun channel(c: Float): Double = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> if (la > lb) la to lb else lb to la } }
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun textPairs(s: ColorScheme) = mapOf(
        "onPrimary/primary" to (s.onPrimary to s.primary),
        "onPrimaryContainer/primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
        "onSurface/surface" to (s.onSurface to s.surface),
        "onSurface/surfaceContainer" to (s.onSurface to s.surfaceContainer),
        "onSurface/surfaceContainerHigh" to (s.onSurface to s.surfaceContainerHigh),
        "onSurfaceVariant/surface" to (s.onSurfaceVariant to s.surface),
        "onSurfaceVariant/surfaceContainer" to (s.onSurfaceVariant to s.surfaceContainer),
        "onSurfaceVariant/surfaceContainerHigh" to (s.onSurfaceVariant to s.surfaceContainerHigh),
        "primary/surface (text buttons)" to (s.primary to s.surface),
        "error/surface" to (s.error to s.surface),
    )

    private fun graphicPairs(s: ColorScheme, x: SnackshotColors) = mapOf(
        "primary/surfaceContainerHigh" to (s.primary to s.surfaceContainerHigh),
        "tertiary/surface" to (s.tertiary to s.surface),
        "error/surfaceContainerHigh" to (s.error to s.surfaceContainerHigh),
        "outline/surface (field borders)" to (s.outline to s.surface),
        "macroCarbs/surfaceContainer" to (x.macroCarbs to s.surfaceContainer),
        "macroFat/surfaceContainer" to (x.macroFat to s.surfaceContainer),
        "macroProtein/surfaceContainer" to (x.macroProtein to s.surfaceContainer),
    )

    private fun statusPairs(x: SnackshotColors) = mapOf(
        "statusBelow" to x.statusBelow,
        "statusInRange" to x.statusInRange,
        "statusAbove" to x.statusAbove,
        "statusUnderTarget" to x.statusUnderTarget,
    ).mapValues { (_, c) -> c.content to c.container }

    private fun check(mode: String, pairs: Map<String, Pair<Color, Color>>, min: Double): List<String> = pairs.mapNotNull { (name, p) ->
        val r = contrast(p.first, p.second)
        if (r < min) "$mode $name = ${"%.2f".format(r)} (< $min)" else null
    }

    @Test
    fun `theme tokens meet WCAG AA`() {
        val failures = listOf(
            "light" to (LightColors to LightSnackshotColors),
            "dark" to (DarkColors to DarkSnackshotColors),
        ).flatMap { (mode, theme) ->
            val (scheme, extra) = theme
            check(mode, textPairs(scheme), 4.5) +
                check(mode, statusPairs(extra), 4.5) +
                check(mode, graphicPairs(scheme, extra), 3.0)
        }
        assertTrue("Contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}

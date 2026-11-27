package app.snackshot.ui.home

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionTotals
import app.snackshot.ui.R
import app.snackshot.ui.theme.GoogleSansFontFamily
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

// The Home daily summary in the style of the Google Health "Today" header
// (docs/competitors_UI/pictures/GoogleHealth (19).jpg): a thick ring with curved labels on the left,
// three tall macro pills on the right.

@Composable
internal fun DailySummary(totals: NutritionTotals, goals: NutritionGoals, onClick: () -> Unit) {
    val kcal = totals.energyKcal.roundToInt()
    val target = goals.calories?.targetKcal
    val colors = SnackshotTheme.colors
    val macros = listOf(
        MacroLine(R.string.macro_carbs, R.drawable.ic_grain, totals.carbsGrams, goals.carbsGrams, colors.macroCarbs),
        MacroLine(R.string.macro_fat, R.drawable.ic_water_drop, totals.fatGrams, goals.fatGrams, colors.macroFat),
        MacroLine(R.string.macro_protein, R.drawable.ic_egg_alt, totals.proteinGrams, goals.proteinGrams, colors.macroProtein),
    )
    val description = summaryDescription(kcal, target, macros)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = stringResource(R.string.home_open_dashboard), onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(vertical = Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            CalorieRing(kcal, target, Modifier.weight(1f))
            val sum = totals.carbsGrams + totals.fatGrams + totals.proteinGrams
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PILL_GAP)) {
                macros.forEach { MacroPill(it, sum) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.grid)) {
            Text(caption(kcal, target), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal data class MacroLine(@StringRes val name: Int, @DrawableRes val icon: Int, val grams: Double, val goal: Int?, val color: Color)

// ---------------------------------------------------------------- Ring

/**
 * The calorie ring: progress from 6 o'clock clockwise, "Calories" curved along the top of the hole,
 * the share of the target in the middle and "750 of 2,300 kcal" curved along the bottom.
 */
@Composable
private fun CalorieRing(kcal: Int, target: Int?, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val value = MaterialTheme.colorScheme.primary
    val titleColor = MaterialTheme.colorScheme.onSurface
    val title = stringResource(R.string.home_ring_title)
    val bottom = if (target != null) {
        stringResource(R.string.home_ring_of, formatInt(kcal), formatInt(target))
    } else {
        stringResource(R.string.home_ring_no_goal)
    }
    val context = LocalContext.current
    val typeface = remember(context) {
        runCatching { context.resources.getFont(R.font.google_sans_flex_medium) }.getOrDefault(Typeface.DEFAULT_BOLD)
    }
    Box(modifier.widthIn(max = RING).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = RING_STROKE.toPx()
            val inset = stroke / 2
            val arcSize = GeometrySize(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            if (target != null && kcal > 0) {
                val sweep = 360f * (kcal.toFloat() / target).coerceIn(0f, 1f)
                drawArc(
                    value,
                    90f,
                    sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            val inner = size.minDimension / 2 - stroke
            val textPx = 15.sp.toPx()
            val gap = 4.dp.toPx()
            curvedText(title, inner - gap - textPx * 0.72f, top = true, textPx, titleColor, typeface)
            curvedText(bottom, inner - gap - textPx * 0.2f, top = false, textPx, value, typeface)
        }
        Text(
            if (target != null) stringResource(R.string.home_ring_percent, (kcal * 100.0 / target).roundToInt()) else formatInt(kcal),
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = GoogleSansFontFamily, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

/** Text centered on the top or bottom half of a circle of [radius], upright in both cases. */
private fun DrawScope.curvedText(text: String, radius: Float, top: Boolean, textPx: Float, color: Color, typeface: Typeface) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = textPx
        this.color = color.toArgb()
    }
    val arc = (PI * radius).toFloat()
    val width = paint.measureText(text)
    // Long labels and translations shrink to fit 70% of the half circle.
    if (width > arc * 0.7f) paint.textSize = textPx * arc * 0.7f / width
    val c = center
    val path = Path().apply {
        // Top: 9 → 12 → 3 o'clock, glyphs face outward. Bottom: 9 → 6 → 3 o'clock, glyphs face inward.
        addArc(RectF(c.x - radius, c.y - radius, c.x + radius, c.y + radius), 180f, if (top) 180f else -180f)
    }
    val offset = (arc - paint.measureText(text)) / 2
    drawIntoCanvas { it.nativeCanvas.drawTextOnPath(text, path, offset, 0f, paint) }
}

// ---------------------------------------------------------------- Macro pills

/**
 * A tall pill: an icon capsule, the macro's name and today's grams. The lighter fill shows progress
 * to the goal, or the macro's share of today's grams when no goal is set.
 */
@Composable
private fun MacroPill(line: MacroLine, sumGrams: Double) {
    val grams = line.grams.roundToInt()
    val goal = line.goal?.takeIf { it > 0 }
    val fraction = when {
        goal != null -> (line.grams / goal).toFloat()
        sumGrams > 0 -> (line.grams / sumGrams).toFloat()
        else -> 0f
    }.coerceIn(0f, 1f)
    val over = goal != null && line.grams > goal

    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val base = scheme.surfaceContainer
    val trackColor = lerp(base, line.color, if (dark) 0.32f else 0.16f)
    val fillColor = lerp(base, line.color, if (dark) 0.62f else 0.38f)
    val iconTint = if (dark) line.color else lerp(line.color, Color.Black, 0.35f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT)
            .clip(RoundedCornerShape(PILL_CORNER))
            .background(trackColor),
    ) {
        if (fraction > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(fillColor))
        Row(
            Modifier.fillMaxSize().padding(horizontal = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(width = 32.dp, height = 42.dp).clip(RoundedCornerShape(16.dp)).background(base.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(line.icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                val name = stringResource(line.name)
                Text(
                    if (over) stringResource(R.string.home_macro_over, name) else name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (over) scheme.error else scheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildAnnotatedString {
                        append(formatInt(grams))
                        withStyle(SpanStyle(fontSize = 13.sp, color = scheme.onSurface.copy(alpha = 0.75f))) {
                            append(if (goal != null) " / ${formatInt(goal)} " else " ")
                            append(stringResource(R.string.unit_g))
                        }
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = GoogleSansFontFamily, lineHeight = 24.sp),
                    color = scheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Text

@Composable
private fun caption(kcal: Int, target: Int?) = buildAnnotatedString {
    if (target == null) {
        append(stringResource(R.string.home_no_goal))
        return@buildAnnotatedString
    }
    val left = target - kcal
    val amount = stringResource(R.string.home_kcal_amount, formatInt(abs(left)))
    val template = stringResource(if (left >= 0) R.string.home_kcal_left else R.string.home_kcal_over, amount)
    val start = template.indexOf(amount)
    append(template)
    if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, start + amount.length)
}

@Composable
private fun summaryDescription(kcal: Int, target: Int?, macros: List<MacroLine>): String {
    val parts = macros.map { m ->
        val name = stringResource(m.name)
        if (m.goal != null) {
            stringResource(R.string.home_a11y_macro_goal, name, m.grams.roundToInt(), m.goal)
        } else {
            stringResource(R.string.home_a11y_macro, name, m.grams.roundToInt())
        }
    }.joinToString(", ")
    return if (target != null) {
        stringResource(
            R.string.home_a11y_summary_goal,
            formatInt(kcal),
            formatInt(target),
            formatInt((target - kcal).coerceAtLeast(0)),
            parts,
        )
    } else {
        stringResource(R.string.home_a11y_summary, formatInt(kcal), parts)
    }
}

@Composable
internal fun SummarySkeleton() {
    val label = stringResource(R.string.state_loading)
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.size(RING).clip(CircleShape).background(placeholder))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PILL_GAP)) {
            repeat(3) {
                Box(Modifier.fillMaxWidth().height(PILL_HEIGHT).clip(RoundedCornerShape(PILL_CORNER)).background(placeholder))
            }
        }
    }
}

private val RING = 176.dp
private val RING_STROKE = 26.dp
private val PILL_HEIGHT = 56.dp
private val PILL_GAP = 8.dp
private val PILL_CORNER = 20.dp

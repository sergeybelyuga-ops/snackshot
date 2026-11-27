package app.snackshot.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.snackshot.ui.R
import app.snackshot.ui.theme.GoogleSansFontFamily
import app.snackshot.ui.theme.Spacing

// S-16 building blocks from docs/UX_UI/Prototype/s16_settings.html: top app bar, grouped list rows
// (2 dp gaps, 16 dp outer and 4 dp inner corners), section labels and footnotes.

/** A Settings page: top app bar (Back when [onBack] is set) over a scrolling 16 dp column. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = contentWindowInsets,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
        bottomBar = { bottomBar?.invoke() },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.content, end = Spacing.content, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.content),
        ) { content() }
    }
}

/** Rows of one group, 2 dp apart; [row] gets the shape for its position. */
@Composable
fun ListGroup(count: Int, row: @Composable (index: Int, shape: Shape) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(count) { i ->
            val top = if (i == 0) 16.dp else 4.dp
            val bottom = if (i == count - 1) 16.dp else 4.dp
            row(i, RoundedCornerShape(top, top, bottom, bottom))
        }
    }
}

/** A grouped list row: 72 dp, 40 dp leading slot, title and support text, trailing content. */
@Composable
fun ListRow(
    title: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    support: String? = null,
    supportColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.content, vertical = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.content),
    ) {
        if (leading != null) Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { leading() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.15.sp))
            if (support != null) {
                Text(
                    support,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.grid),
                content = trailing,
            )
        }
    }
}

/** The 40 dp round icon badge of a list row. */
@Composable
fun RowIcon(@DrawableRes icon: Int) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** The macro accent of a row: a colored bar, always next to the macro's name. */
@Composable
fun AccentBar(color: Color) {
    Box(Modifier.width(6.dp).height(36.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
fun Chevron() {
    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A large row value with a small unit: "2,300 kcal". */
@Composable
fun RowValue(value: String, unit: String) {
    Text(
        buildAnnotatedString {
            append(value)
            pushStyle(
                SpanStyle(
                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            append(" ")
            append(unit)
            pop()
        },
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = GoogleSansFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.grid, end = Spacing.grid, top = Spacing.small).semantics { heading() },
    )
}

@Composable
fun IntroText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.grid),
    )
}

/** A small icon and a 12 sp note under a group. */
@Composable
fun Footnote(@DrawableRes icon: Int, text: String) {
    Row(
        Modifier.padding(start = Spacing.grid, end = Spacing.grid, top = Spacing.grid),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** [template] with [part] in medium weight and [color], for "Target **2,300 kcal** · middle of your range". */
fun emphasize(template: String, part: String, color: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
    append(template)
    val start = template.indexOf(part)
    if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Medium, color = color), start, start + part.length)
}

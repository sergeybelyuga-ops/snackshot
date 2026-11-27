package app.snackshot.ui.capture

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.domain.meal.RecognitionFailure
import app.snackshot.ui.R
import app.snackshot.ui.meal.MealPhoto
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.SnackshotTheme
import app.snackshot.ui.theme.Spacing
import java.io.File

@Composable
fun RecognitionRoute(
    viewModel: RecognitionViewModel,
    onRecognized: () -> Unit,
    onClose: () -> Unit,
    onQuickAdd: () -> Unit,
    onRetake: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.recognized) { if (state.recognized) onRecognized() }
    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    RecognitionScreen(
        state = state,
        image = state.imageRef?.let(viewModel::fileFor),
        onRetry = viewModel::retry,
        onClose = onClose,
        onQuickAdd = onQuickAdd,
        onRetake = onRetake,
        onGallery = onGallery,
        modifier = modifier,
    )
}

/** S-05 Recognition progress (prototype `s05_progress.html`, doc 10 §4.6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    state: RecognitionState,
    image: File?,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    onQuickAdd: () -> Unit,
    onRetake: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unusable = state.unusable
    val failure = state.failure
    val title = when {
        unusable != null -> R.string.recognition_title_unusable
        state.recognized -> R.string.recognition_title_done
        state.retrying -> R.string.recognition_title_retrying
        failure != null && state.canRetry -> R.string.recognition_title_failed
        failure != null -> R.string.recognition_title_unavailable
        state.slow -> R.string.recognition_title_slow
        else -> R.string.recognition_title
    }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.recognition_cancel))
                    }
                },
            )
        },
        bottomBar = {
            Actions(state, onRetry, onClose, onQuickAdd, onRetake, onGallery, Modifier.navigationBarsPadding())
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.content),
        ) {
            MealPhoto(
                file = image,
                contentDescription = stringResource(R.string.recognition_photo),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Column {
                    CheckRow(state)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProviderRow(state)
                }
            }
        }
    }
}

@Composable
private fun CheckRow(state: RecognitionState) {
    val info = state.imageInfo
    when {
        state.unusable != null && state.unusable != Unusable.NO_FOOD -> StatusRow(
            tone = Tone.ERROR,
            title = stringResource(
                when (state.unusable) {
                    Unusable.TOO_SMALL -> R.string.recognition_too_small
                    Unusable.TOO_LARGE -> R.string.recognition_too_large
                    else -> R.string.recognition_unreadable
                },
            ),
            meta = stringResource(R.string.recognition_unusable_text),
        )
        state.check == StepState.DONE -> StatusRow(
            tone = Tone.OK,
            title = stringResource(R.string.recognition_checked),
            meta = info?.let { stringResource(R.string.recognition_checked_meta, it.longSidePx, it.kilobytes) },
        )
        else -> StatusRow(
            tone = Tone.BUSY,
            title = stringResource(R.string.recognition_checking),
            meta = stringResource(R.string.recognition_checking_meta),
        )
    }
}

@Composable
private fun ProviderRow(state: RecognitionState) {
    val failure = state.failure
    when {
        state.unusable == Unusable.NO_FOOD -> StatusRow(
            tone = Tone.ERROR,
            title = stringResource(R.string.recognition_no_food),
            meta = stringResource(R.string.recognition_no_food_text),
        )
        state.unusable != null -> StatusRow(
            tone = Tone.IDLE,
            icon = R.drawable.ic_cloud_off,
            title = stringResource(R.string.recognition_not_sent),
            meta = stringResource(R.string.recognition_not_sent_text),
        )
        state.recognized -> StatusRow(tone = Tone.OK, title = stringResource(R.string.recognition_complete), meta = null)
        failure != null -> {
            val (title, meta) = failureText(failure, state)
            StatusRow(tone = Tone.ERROR, title = title, meta = meta)
        }
        state.recognition == StepState.RUNNING -> StatusRow(
            tone = Tone.BUSY,
            icon = if (state.slow) R.drawable.ic_hourglass else null,
            title = stringResource(
                when {
                    state.retrying -> R.string.recognition_retrying
                    state.slow -> R.string.recognition_still
                    else -> R.string.recognition_running
                },
            ),
            meta = stringResource(if (state.slow) R.string.recognition_slow_text else R.string.recognition_running_meta),
            progress = true,
        )
        else -> StatusRow(
            tone = Tone.IDLE,
            icon = R.drawable.ic_schedule,
            title = stringResource(R.string.recognition_waiting),
            meta = stringResource(R.string.recognition_waiting_meta),
        )
    }
}

@Composable
private fun failureText(failure: RecognitionFailure, state: RecognitionState): Pair<String, String> {
    val retried = !state.canRetry
    return when (failure) {
        RecognitionFailure.OFFLINE -> stringResource(R.string.recognition_offline) to
            stringResource(if (retried) R.string.recognition_offline_retried else R.string.recognition_offline_text)
        RecognitionFailure.SERVICE -> stringResource(R.string.recognition_service) to
            stringResource(if (retried) R.string.recognition_service_retried else R.string.recognition_service_text)
        RecognitionFailure.DAILY_LIMIT -> {
            val hours = state.retryAfter?.let { ((it.toMinutes() + 59) / 60).toInt().coerceAtLeast(1) }
            stringResource(R.string.recognition_limit) to
                (
                    hours?.let {
                        stringResource(R.string.recognition_limit_text, it)
                    } ?: stringResource(R.string.recognition_limit_text_later)
                    )
        }
        RecognitionFailure.NO_ACCESS -> stringResource(R.string.recognition_no_access) to
            stringResource(R.string.recognition_no_access_text)
        RecognitionFailure.INVALID_IMAGE -> stringResource(R.string.recognition_refused) to
            stringResource(R.string.recognition_refused_text)
        RecognitionFailure.NO_FOOD -> stringResource(R.string.recognition_no_food) to stringResource(R.string.recognition_no_food_text)
    }
}

private enum class Tone { OK, BUSY, ERROR, IDLE }

@Composable
private fun StatusRow(tone: Tone, title: String, meta: String?, @DrawableRes icon: Int? = null, progress: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(Spacing.content), horizontalArrangement = Arrangement.spacedBy(Spacing.content)) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                tone == Tone.BUSY && icon == null -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                tone == Tone.OK -> Icon(
                    painterResource(R.drawable.ic_check_circle_filled),
                    null,
                    tint = SnackshotTheme.colors.statusInRange.content,
                )
                tone == Tone.ERROR -> Icon(painterResource(R.drawable.ic_error), null, tint = scheme.error)
                else -> Icon(painterResource(icon ?: R.drawable.ic_schedule), null, tint = scheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = when (tone) {
                    Tone.ERROR -> scheme.error
                    Tone.IDLE -> scheme.onSurfaceVariant
                    else -> scheme.onSurface
                },
            )
            if (progress) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = Spacing.grid))
            if (meta != null) Text(meta, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Actions(
    state: RecognitionState,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    onQuickAdd: () -> Unit,
    onRetake: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.recognized) return
    Column(
        modifier.fillMaxWidth().padding(Spacing.content),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val unusable = state.unusable
        if (unusable != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f).heightIn(min = Size.minTouchTarget)) {
                    ButtonContent(R.drawable.ic_photo_library, R.string.camera_gallery)
                }
                Button(onClick = onRetake, modifier = Modifier.weight(1f).heightIn(min = Size.minTouchTarget)) {
                    ButtonContent(R.drawable.ic_photo_camera, R.string.recognition_retake)
                }
            }
            if (unusable == Unusable.NO_FOOD) {
                FilledTonalButton(onClick = onQuickAdd, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                    ButtonContent(R.drawable.ic_edit_note, R.string.recognition_quick_add)
                }
            }
            return@Column
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
            Text(stringResource(R.string.recognition_cancel))
        }
        val failed = state.failure != null
        when {
            failed && state.canRetry -> Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                ButtonContent(R.drawable.ic_refresh, R.string.recognition_retry)
            }
            state.retrying -> Button(onClick = {
            }, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                ButtonContent(R.drawable.ic_refresh, R.string.recognition_retrying_button)
            }
            failed || state.slow -> FilledTonalButton(
                onClick = onQuickAdd,
                modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget),
            ) {
                ButtonContent(R.drawable.ic_edit_note, R.string.recognition_quick_add)
            }
        }
        if (failed || state.slow || state.retrying) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_draft),
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    stringResource(if (failed && state.canRetry) R.string.recognition_draft_retry else R.string.recognition_draft_kept),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ButtonContent(@DrawableRes icon: Int, @StringRes label: Int) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
    Text(stringResource(label), modifier = Modifier.padding(start = Spacing.small))
}

package app.snackshot.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.snackshot.ui.R
import kotlin.math.roundToInt

/** The three ways to log a meal, in carousel order (doc 10 §4.4). */
enum class EntryAction(@StringRes val label: Int, @DrawableRes val icon: Int) {
    TAKE_PHOTO(R.string.home_entry_photo, R.drawable.ic_photo_camera),
    GALLERY(R.string.home_entry_gallery, R.drawable.ic_photo_library),
    MANUAL_ADD(R.string.home_entry_manual, R.drawable.ic_edit_note),
}

/** A hold-and-slide gesture in progress: where it started, where the finger is, what it points at. */
private data class Joystick(val base: Offset, val finger: Offset, val start: Int, val offset: Int)

/**
 * The meal-entry carousel (doc 10 §4.4): a vertical, icon-only dock in the thumb zone. A tap on the
 * centered action starts it; the previous action is shown above it and the next one below.
 *
 * Holding the action (or sliding from it) opens a joystick: a ring under the finger and the three
 * actions with their names to the left of it, where the finger doesn't cover them. Sliding up or
 * down points at an action; lifting the finger makes it the centered one. TalkBack users get
 * "Switch to …" actions instead.
 */
@Composable
fun EntryDock(enabled: Boolean, busy: Boolean, onAction: (EntryAction) -> Unit, modifier: Modifier = Modifier) {
    val actions = EntryAction.entries
    val n = actions.size
    var index by rememberSaveable { mutableIntStateOf(0) }
    var joystick by remember { mutableStateOf<Joystick?>(null) }
    // Keeps the last position while the overlay fades out.
    var shown by remember { mutableStateOf<Joystick?>(null) }
    joystick?.let { shown = it }
    val overlay = remember { MutableTransitionState(false) }
    overlay.targetState = joystick != null

    val current = actions[index]
    val active by rememberUpdatedState(enabled && !busy)
    val launch by rememberUpdatedState { onAction(actions[index]) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rowPx = with(density) { PANEL_ROW.toPx() }
    val button = with(density) {
        val half = BUTTON.toPx() / 2 + 8.dp.toPx()
        val center = Offset(DOCK_WIDTH.toPx() / 2, DOCK_HEIGHT.toPx() / 2)
        Rect(center, half)
    }

    val label = stringResource(current.label)
    val toPrevious = stringResource(R.string.home_entry_switch_to, stringResource(actions[(index - 1 + n) % n].label))
    val toNext = stringResource(R.string.home_entry_switch_to, stringResource(actions[(index + 1) % n].label))
    val hint = stringResource(R.string.home_entry_hint)

    Box(
        modifier = modifier
            .width(DOCK_WIDTH)
            .height(DOCK_HEIGHT)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!active || !button.contains(down.position)) return@awaitEachGesture
                    down.consume()
                    var last = down.position
                    var outcome = Outcome.HOLD
                    withTimeoutOrNull(HOLD_MS) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                outcome = Outcome.CANCEL
                                return@withTimeoutOrNull
                            }
                            last = change.position
                            if (!change.pressed) {
                                outcome = Outcome.TAP
                                return@withTimeoutOrNull
                            }
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                outcome = Outcome.SLIDE
                                return@withTimeoutOrNull
                            }
                        }
                    }
                    when (outcome) {
                        Outcome.TAP -> launch()
                        Outcome.CANCEL -> Unit
                        Outcome.HOLD, Outcome.SLIDE -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            fun pointAt(y: Float) = ((y - down.position.y) / rowPx).roundToInt().coerceIn(-1, 1)
                            var stick = Joystick(down.position, last, index, pointAt(last.y))
                            joystick = stick
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                change.consume()
                                val offset = pointAt(change.position.y)
                                if (offset != stick.offset) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                stick = stick.copy(finger = change.position, offset = offset)
                                joystick = stick
                            }
                            index = (stick.start + stick.offset + n) % n
                            joystick = null
                        }
                    }
                }
            },
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            EntryPreview(actions[(index - 1 + n) % n])
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(BUTTON)
                    .alpha(if (enabled) 1f else 0.6f)
                    .semantics {
                        contentDescription = "$label. $hint"
                        role = Role.Button
                        if (!(enabled && !busy)) disabled()
                        onClick {
                            if (active) launch()
                            active
                        }
                        customActions = listOf(
                            CustomAccessibilityAction(toPrevious) {
                                index = (index - 1 + n) % n
                                true
                            },
                            CustomAccessibilityAction(toNext) {
                                index = (index + 1) % n
                                true
                            },
                        )
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                    } else {
                        Icon(painterResource(current.icon), contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            }
            EntryPreview(actions[(index + 1) % n])
        }

        val stick = shown
        if (stick != null && (overlay.currentState || overlay.targetState)) {
            JoystickOverlay(stick, overlay)
        }
    }
}

private enum class Outcome { TAP, HOLD, SLIDE, CANCEL }

/** The ring under the finger with its knob, and the action list to the left of it. */
@Composable
private fun JoystickOverlay(stick: Joystick, visible: MutableTransitionState<Boolean>) {
    val actions = EntryAction.entries
    val n = actions.size
    val density = LocalDensity.current
    val knobTravel = with(density) { (JOY_RING - JOY_KNOB).toPx() / 2 }
    val primary = MaterialTheme.colorScheme.primary

    // Ring: stays where the hold started.
    Placed(stick.base, JOY_RING, JOY_RING, anchorEnd = false) {
        AnimatedVisibility(visible, enter = fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.6f), exit = fadeOut(tween(120))) {
            Box(
                Modifier
                    .size(JOY_RING)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f))
                    .border(2.dp, primary.copy(alpha = 0.7f), CircleShape)
                    .clearAndSetSemantics { },
            )
        }
    }
    // Knob: follows the finger inside the ring.
    val delta = (stick.finger - stick.base).let { d ->
        val length = d.getDistance()
        if (length > knobTravel) d * (knobTravel / length) else d
    }
    Placed(stick.base + delta, JOY_KNOB, JOY_KNOB, anchorEnd = false) {
        AnimatedVisibility(visible, enter = fadeIn(tween(120)), exit = fadeOut(tween(120))) {
            Box(Modifier.size(JOY_KNOB).clip(CircleShape).background(primary.copy(alpha = 0.45f)).clearAndSetSemantics { })
        }
    }
    // Action list: right edge left of the ring, the starting action level with the finger.
    val gap = with(density) { (JOY_RING / 2 + 8.dp).toPx() }
    Placed(Offset(stick.base.x - gap, stick.base.y), PANEL_WIDTH, PANEL_HEIGHT, anchorEnd = true) {
        AnimatedVisibility(
            visible,
            enter = fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.85f, transformOrigin = TransformOrigin(1f, 0.5f)),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0.5f)),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
                modifier = Modifier.size(PANEL_WIDTH, PANEL_HEIGHT).clearAndSetSemantics { },
            ) {
                Column(Modifier.padding(PANEL_PADDING)) {
                    (-1..1).forEach { offset ->
                        JoystickRow(actions[(stick.start + offset + n) % n], selected = offset == stick.offset)
                    }
                }
            }
        }
    }
}

@Composable
private fun JoystickRow(action: EntryAction, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerHigh, tween(90), label = "row")
    val content by animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, tween(90), label = "content")
    Row(
        Modifier
            .fillMaxWidth()
            .height(PANEL_ROW)
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(action.icon), contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        Text(stringResource(action.label), style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/**
 * Places [content] of a fixed size at [at] in the dock's coordinates, centered on it, or with its
 * right edge on it when [anchorEnd]. Takes no space itself, so it may draw outside the dock.
 */
@Composable
private fun Placed(at: Offset, width: Dp, height: Dp, anchorEnd: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.layout { measurable, _ ->
            val w = width.roundToPx()
            val h = height.roundToPx()
            val placeable = measurable.measure(Constraints(maxWidth = w, maxHeight = h))
            layout(0, 0) {
                val x = if (anchorEnd) at.x.roundToInt() - w else at.x.roundToInt() - w / 2
                placeable.place(x, at.y.roundToInt() - h / 2)
            }
        },
    ) { content() }
}

@Composable
private fun EntryPreview(action: EntryAction) {
    Box(
        Modifier
            .size(40.dp)
            .alpha(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(action.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

internal val DOCK_WIDTH = 64.dp
internal val DOCK_HEIGHT = 168.dp
private val BUTTON = 60.dp
private val JOY_RING = 88.dp
private val JOY_KNOB = 40.dp
private val PANEL_ROW = 52.dp
private val PANEL_PADDING = 6.dp
private val PANEL_WIDTH = 188.dp
private val PANEL_HEIGHT = PANEL_ROW * 3 + PANEL_PADDING * 2

/** How long a press waits before it becomes a hold. */
private const val HOLD_MS = 250L

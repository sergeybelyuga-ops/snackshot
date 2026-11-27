package app.snackshot.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.zIndex
import app.snackshot.ui.R
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The three ways to log a meal, in carousel order (doc 10 §4.4). */
enum class EntryAction(@StringRes val label: Int, @DrawableRes val icon: Int) {
    TAKE_PHOTO(R.string.home_entry_photo, R.drawable.ic_photo_camera),
    GALLERY(R.string.home_entry_gallery, R.drawable.ic_photo_library),
    MANUAL_ADD(R.string.home_entry_manual, R.drawable.ic_edit_note),
}

/** A hold-and-slide gesture in progress: where it started, where the finger is, what it points at. */
private data class Joystick(val base: Offset, val finger: Offset, val start: Int, val offset: Int)

/**
 * The meal-entry carousel (doc 10 §4.4), in the thumb zone: the current action in front, the other
 * two stacked behind it. A tap on the front action starts it.
 *
 * Holding it (or sliding from it) fans the three actions out on an arc to the left of the finger,
 * where the finger doesn't cover them, and puts a joystick ring under the finger: the previous
 * action above it, the current one up-left, the next one to the left. Sliding toward an action
 * (up for the previous, down for the next) points at it; lifting the finger makes it the front one
 * without starting it, and the arc folds back into the stack. TalkBack users get "Switch to …"
 * actions instead.
 */
@Composable
fun EntryDock(enabled: Boolean, busy: Boolean, onAction: (EntryAction) -> Unit, modifier: Modifier = Modifier) {
    val actions = EntryAction.entries
    val n = actions.size
    var index by rememberSaveable { mutableIntStateOf(0) }
    var joystick by remember { mutableStateOf<Joystick?>(null) }
    // Keeps the arc in place while it folds back into the stack.
    var shown by remember { mutableStateOf<Joystick?>(null) }
    joystick?.let { shown = it }
    val fan by animateFloatAsState(
        targetValue = if (joystick != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "fan",
    )

    val current = actions[index]
    val active by rememberUpdatedState(enabled && !busy)
    val launch by rememberUpdatedState { onAction(actions[index]) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val deadZone = with(density) { DEAD_ZONE.toPx() }
    val center = with(density) { Offset(DOCK_WIDTH.toPx() / 2, DOCK_HEIGHT.toPx() / 2) }
    val button = with(density) { Rect(center, BUTTON.toPx() / 2 + 8.dp.toPx()) }

    val label = stringResource(current.label)
    val toPrevious = stringResource(R.string.home_entry_switch_to, stringResource(actions[(index - 1 + n) % n].label))
    val toNext = stringResource(R.string.home_entry_switch_to, stringResource(actions[(index + 1) % n].label))
    val hint = stringResource(R.string.home_entry_hint)

    Box(
        modifier = modifier
            .width(DOCK_WIDTH)
            .height(DOCK_HEIGHT)
            .alpha(if (enabled) 1f else 0.6f)
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
                            var stick = Joystick(down.position, last, index, pointAt(last - down.position, deadZone))
                            joystick = stick
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                change.consume()
                                val offset = pointAt(change.position - down.position, deadZone)
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
        val stick = shown
        val opacity = fan.coerceIn(0f, 1f)
        if (stick != null && opacity > 0f) JoystickRing(stick, opacity)

        val open = joystick
        actions.forEachIndexed { i, action ->
            val stackRel = relative(i, index, n)
            val arcRel = relative(i, stick?.start ?: index, n)
            val front = stackRel == 0
            val pointed = if (open != null) arcRel == open.offset else front
            // At rest: the front card in the middle, the others behind it, peeking out above and below.
            val rest = with(density) { center + Offset(if (front) 0f else STACK_DX.toPx(), stackRel * STACK_DY.toPx()) }
            val fanned = if (stick != null) with(density) { stick.base + arcSlot(arcRel, ARC_RADIUS.toPx()) } else rest
            val t = fan.coerceAtLeast(0f)
            val at = rest + (fanned - rest) * t
            val scale = mix(if (front) 1f else STACK_SCALE, if (pointed) 1f else ARC_SCALE, t)

            Placed(at, BUTTON, BUTTON, anchorEnd = false, modifier = Modifier.zIndex(if (front) 2f else 1f)) {
                DockAction(
                    action = action,
                    highlighted = pointed,
                    busy = busy && front && open == null,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(
                            if (front) {
                                Modifier.semantics {
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
                                }
                            } else {
                                Modifier.clearAndSetSemantics { }
                            },
                        ),
                )
            }
            // The pointed action's name, to the left of its card.
            if (stick != null && pointed && opacity > 0f) {
                val labelAt = with(density) { Offset(at.x - BUTTON.toPx() / 2 - 8.dp.toPx(), at.y) }
                Placed(labelAt, LABEL_WIDTH, LABEL_HEIGHT, anchorEnd = true, modifier = Modifier.zIndex(3f)) {
                    ActionLabel(stringResource(action.label), opacity)
                }
            }
        }
    }
}

private enum class Outcome { TAP, HOLD, SLIDE, CANCEL }

/** -1 for the action before [index], 0 for it, 1 for the one after. */
private fun relative(i: Int, index: Int, n: Int) = (i - index + n + 1) % n - 1

private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

/** The direction of the action at [rel] from the finger, in degrees counterclockwise from the right. */
private fun arcAngle(rel: Int) = ARC_CENTER_DEG + rel * ARC_STEP_DEG

/**
 * Where the action at [rel] sits on the arc around the finger: the previous one above it, the
 * current one up-left, the next one left and slightly below, clear of the navigation bar.
 */
private fun arcSlot(rel: Int, radius: Float): Offset {
    val angle = Math.toRadians(arcAngle(rel))
    return Offset((radius * cos(angle)).toFloat(), (-radius * sin(angle)).toFloat())
}

/** What a slide of [d] points at: the action whose direction is closest, or the current one inside the dead zone. */
private fun pointAt(d: Offset, deadZone: Float): Int {
    if (d.getDistance() < deadZone) return 0
    val toward = Math.toDegrees(atan2(-d.y, d.x).toDouble())
    return (-1..1).minBy { rel ->
        val diff = abs(toward - arcAngle(rel)) % 360.0
        minOf(diff, 360.0 - diff)
    }
}

/** The ring that stays where the hold started, and the knob that follows the finger inside it. */
@Composable
private fun JoystickRing(stick: Joystick, opacity: Float) {
    val density = LocalDensity.current
    val knobTravel = with(density) { (JOY_RING - JOY_KNOB).toPx() / 2 }
    val primary = MaterialTheme.colorScheme.primary
    Placed(stick.base, JOY_RING, JOY_RING, anchorEnd = false) {
        Box(
            Modifier
                .size(JOY_RING)
                .graphicsLayer {
                    alpha = opacity
                    scaleX = 0.6f + 0.4f * opacity
                    scaleY = 0.6f + 0.4f * opacity
                }
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.12f))
                .border(2.dp, primary.copy(alpha = 0.7f), CircleShape)
                .clearAndSetSemantics { },
        )
    }
    val delta = (stick.finger - stick.base).let { d ->
        val length = d.getDistance()
        if (length > knobTravel) d * (knobTravel / length) else d
    }
    Placed(stick.base + delta, JOY_KNOB, JOY_KNOB, anchorEnd = false) {
        Box(
            Modifier
                .size(JOY_KNOB)
                .alpha(opacity)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.45f))
                .clearAndSetSemantics { },
        )
    }
}

/** One action card: primary when it is the front (or pointed-at) one, a tonal card otherwise. */
@Composable
private fun DockAction(action: EntryAction, highlighted: Boolean, busy: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (highlighted) scheme.primary else scheme.surfaceContainerHighest, tween(120), label = "card")
    val content by animateColorAsState(if (highlighted) scheme.onPrimary else scheme.onSurfaceVariant, tween(120), label = "icon")
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        shadowElevation = if (highlighted) 6.dp else 3.dp,
        modifier = modifier.size(BUTTON),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(28.dp), color = content, strokeWidth = 3.dp)
            } else {
                Icon(painterResource(action.icon), contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    }
}

/** The pointed action's name in a small inverse pill, right-aligned in its slot. */
@Composable
private fun ActionLabel(text: String, opacity: Float) {
    Box(Modifier.size(LABEL_WIDTH, LABEL_HEIGHT), contentAlignment = Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.alpha(opacity).clearAndSetSemantics { },
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * Places [content] of a fixed size at [at] in the dock's coordinates, centered on it, or with its
 * right edge on it when [anchorEnd]. Takes no space itself, so it may draw outside the dock.
 */
@Composable
private fun Placed(at: Offset, width: Dp, height: Dp, anchorEnd: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.layout { measurable, _ ->
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

internal val DOCK_WIDTH = 64.dp
internal val DOCK_HEIGHT = 104.dp
private val BUTTON = 60.dp
private val JOY_RING = 88.dp
private val JOY_KNOB = 40.dp

/** The cards behind the front one peek out above and below it, toward the screen edge. */
private val STACK_DX = 6.dp
private val STACK_DY = 22.dp
private const val STACK_SCALE = 0.82f

/** The fanned-out arc: its radius around the finger, its middle direction, the angle between actions, the side cards' size. */
private val ARC_RADIUS = 100.dp
private const val ARC_CENTER_DEG = 140.0
private const val ARC_STEP_DEG = 50.0
private const val ARC_SCALE = 0.86f

/** A slide shorter than this keeps the action it started from. */
private val DEAD_ZONE = 28.dp
private val LABEL_WIDTH = 200.dp
private val LABEL_HEIGHT = 40.dp

/** How long a press waits before it becomes a hold. */
private const val HOLD_MS = 250L

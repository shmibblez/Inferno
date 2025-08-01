/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.shmibblez.inferno.compose.snackbar.AcornSnackbarHostState
import com.shmibblez.inferno.compose.snackbar.SnackbarHost
import com.shmibblez.inferno.compose.snackbar.SnackbarState
import com.shmibblez.inferno.theme.FirefoxTheme
import kotlin.math.roundToInt

/**
 * The anchors that establish the swipe gesture directions.
 */
enum class SwipeToDismissAnchor {
    Start,
    Default,
    End,
    ;

    companion object {

        /**
         * The anchors that establish the swipe gesture for both directions.
         */
        fun swipeBothDirectionsAnchors() = entries

        /**
         * The anchors that establish the swipe gesture for the start direction.
         */
        fun swipeToStartAnchors() = listOf(Start, Default)

        /**
         * The anchors that establish the swipe gesture for the end direction.
         */
        fun swipeToEndAnchors() = listOf(Default, End)
    }
}

/**
 * The percent of the screen an item has to be swiped before it is considered dismissed.
 */
private const val DISMISS_THRESHOLD = 0.5f

/**
 * The velocity (in DP per second) the item has to exceed in order to animate to the next state,
 * even if the [AnchoredDraggableState.positionalThreshold] has not been reached.
 */
private val VELOCITY_THRESHOLD_DP = 150.dp

/**
 * The length of time the swipe gesture will animate for after being initiated by the user.
 */
private const val SWIPE_ANIMATION_DURATION_MS = 230

@Composable
fun rememberSwipeToDismissState(
    key1: Any? = null,
    key2: Any? = null,
    enabled: Boolean = true,
    density: Density = LocalDensity.current,
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
    anchors: List<SwipeToDismissAnchor> = SwipeToDismissAnchor.swipeBothDirectionsAnchors(),
):SwipeToDismissState {
    val swipeWidth = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val draggableAnchors = DraggableAnchors {
        anchors.forEach { anchor ->
            val anchorPosition = when (anchor) {
                SwipeToDismissAnchor.Start -> -swipeWidth
                SwipeToDismissAnchor.Default -> 0f
                SwipeToDismissAnchor.End -> swipeWidth
            }
            anchor at anchorPosition
        }
    }
    val state = remember(key1, key2) {
        SwipeToDismissState(
            enabled = enabled,
            density = density,
            decayAnimationSpec = decayAnimationSpec,
            draggableAnchors = draggableAnchors,
        )
    }

    return state
}

/**
 * The UI state for [SwipeToDismissBox].
 *
 * @param density [Density] used to derive the underlying [AnchoredDraggableState.velocityThreshold].
 * @param decayAnimationSpec [DecayAnimationSpec] used to specify the animation parameters.
 * @property anchoredDraggableState [AnchoredDraggableState] for the underlying [Modifier.anchoredDraggable].
 * @property anchors A list of [SwipeToDismissAnchor] which establish the swipe directions of [SwipeToDismissBox].
 * @property enabled Whether the swipe gesture is active.
 */
class SwipeToDismissState(
    density: Density,
    decayAnimationSpec: DecayAnimationSpec<Float>,
    val draggableAnchors: DraggableAnchors<SwipeToDismissAnchor>,
    val anchoredDraggableState: AnchoredDraggableState<SwipeToDismissAnchor> = AnchoredDraggableState(
        initialValue = SwipeToDismissAnchor.Default,
        anchors = draggableAnchors,
    ),
    private val anchors: List<SwipeToDismissAnchor> = SwipeToDismissAnchor.swipeBothDirectionsAnchors(),
    val enabled: Boolean = true,
) {

    /**
     * Whether there is a swipe gesture in-progress.
     */
    val swipingActive: Boolean
        get() = !anchoredDraggableState.offset.isNaN() && anchoredDraggableState.offset != 0f

    /**
     * The [SwipeToDismissAnchor] the swipe gesture is targeting.
     */
    private val swipeDestination: SwipeToDismissAnchor
        get() = anchoredDraggableState.anchors.closestAnchor(
            position = anchoredDraggableState.offset,
            searchUpwards = anchoredDraggableState.offset > 0,
        ) ?: SwipeToDismissAnchor.Default

    /**
     * Whether the swipe gesture is in the start direction.
     */
    val isSwipingToStart: Boolean
        get() = swipeDestination == SwipeToDismissAnchor.Start

    /**
     * The current [IntOffset] of the swipe. If the X-offset is currently [Float.NaN], it will return 0.
     */
    val safeSwipeOffset: IntOffset
        get() {
            val xOffset = if (anchoredDraggableState.offset.isNaN()) {
                0
            } else {
                anchoredDraggableState.offset.roundToInt()
            }

            return IntOffset(x = xOffset, y = 0)
        }
}

/**
 * A container that can be dismissed by swiping left or right.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param state [SwipeToDismissState] containing the UI state of [SwipeToDismissBox].
 * @param onItemDismiss Invoked when the item is dismissed.
 * @param backgroundContent A composable that is stacked behind the primary content and is exposed
 * when the content is swiped. You can/should use the [state] to have different backgrounds on each side.
 * @param dismissContent The content that can be dismissed.
 */
@Composable
fun SwipeToDismissBox(
    modifier: Modifier = Modifier,
    state: SwipeToDismissState,
    onItemDismiss: () -> Unit,
    backgroundContent: @Composable BoxScope.() -> Unit,
    dismissContent: @Composable BoxScope.() -> Unit,
) {
    val draggableAnchors = state.draggableAnchors

    SideEffect {
        state.anchoredDraggableState.updateAnchors(draggableAnchors)
    }

    LaunchedEffect(state.anchoredDraggableState.currentValue) {
        when (state.anchoredDraggableState.currentValue) {
            SwipeToDismissAnchor.Start, SwipeToDismissAnchor.End -> onItemDismiss()
            SwipeToDismissAnchor.Default -> {} // no-op
        }
    }



    Box(
        modifier = Modifier
            .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = Orientation.Horizontal,
                enabled = state.enabled,
                reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                    state = state.anchoredDraggableState,
                    positionalThreshold = { distance: Float -> distance * DISMISS_THRESHOLD },
                    animationSpec = tween(
                        durationMillis = SWIPE_ANIMATION_DURATION_MS,
                    ),
                ),
                // todo:
//                velocityThreshold = { with(density) { VELOCITY_THRESHOLD_DP.toPx() } },
//                decayAnimationSpec = decayAnimationSpec, // property depracated
            )
            .then(modifier),
    ) {
        Box(
            modifier = Modifier.matchParentSize(),
            content = backgroundContent,
        )

        Box(
            modifier = Modifier.offset { state.safeSwipeOffset },
            content = dismissContent,
        )
    }
}

@Composable
@Preview
@Preview(locale = "ar", name = "RTL")
private fun SwipeToDismissBoxPreview() {
    val snackbarState = remember { AcornSnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    FirefoxTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                SwipeableItem(
                    anchors = SwipeToDismissAnchor.swipeToEndAnchors(),
                    text = "Swipe to right ->",
                    onSwipeToEnd = {
                        coroutineScope.launch {
                            snackbarState.showSnackbar(SnackbarState(message = "Dismiss"))
                        }
                    },
                )

                Spacer(Modifier.height(30.dp))

                SwipeableItem(
                    anchors = SwipeToDismissAnchor.swipeToStartAnchors(),
                    text = "<- Swipe to left",
                    onSwipeToStart = {
                        coroutineScope.launch {
                            snackbarState.showSnackbar(SnackbarState(message = "Dismiss"))
                        }
                    },
                )

                Spacer(Modifier.height(30.dp))

                SwipeableItem(
                    anchors = SwipeToDismissAnchor.swipeBothDirectionsAnchors(),
                    text = "<- Swipe both ways ->",
                    onSwipeToStart = {
                        coroutineScope.launch {
                            snackbarState.showSnackbar(SnackbarState(message = "Dismiss"))
                        }
                    },
                    onSwipeToEnd = {
                        coroutineScope.launch {
                            snackbarState.showSnackbar(SnackbarState(message = "Dismiss"))
                        }
                    },
                )
            }

            SnackbarHost(
                snackbarHostState = snackbarState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SwipeableItem(
    text: String,
    anchors: List<SwipeToDismissAnchor>,
    onSwipeToStart: () -> Unit = {},
    onSwipeToEnd: () -> Unit = {},
) {
    val density = LocalDensity.current
    val decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()

    val swipeState = rememberSwipeToDismissState(
            density = density,
            decayAnimationSpec = decayAnimationSpec,
        )

    Box(
        modifier = Modifier
            .height(30.dp)
            .fillMaxWidth(),
    ) {
        SwipeToDismissBox(
            state = swipeState,
            onItemDismiss = {
                if (swipeState.anchoredDraggableState.currentValue == SwipeToDismissAnchor.Start) {
                    onSwipeToStart()
                } else {
                    onSwipeToEnd()
                }
            },
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FirefoxTheme.colors.layerAccent),
                )
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FirefoxTheme.colors.layer1),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text)
            }
        }
    }
}

package com.flipp.impressionsandbox.impression

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun <T : Any> Modifier.impression2(
    key: T,
    onImpression: (key: T) -> Unit
): Modifier = with(this) {
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val impressionState = remember { ImpressionState2(lifecycleOwner.lifecycle) }

    LaunchedEffect(key) {
        impressionState.impressionFlow.collect {
            onImpression(key)
        }
    }
    DisposableEffect(key1 = key) {
        onDispose {
            impressionState.onDisposed()
        }
    }

    onGloballyPositioned { globalPosition: LayoutCoordinates ->
        val visibleRect = android.graphics.Rect()
            .apply { view.getGlobalVisibleRect(this) }
            .toComposeRect()

        impressionState.onGlobalPositionChanged(
            globalPosition,
            visibleRect
        )
    }
}

private class ImpressionState2(lifecycle: Lifecycle) {
    companion object {
        private val now: Long get() = System.currentTimeMillis()
    }

    //region fields
    private val impressionChannel = Channel<Any>()
    val impressionFlow: Flow<Any> = impressionChannel.receiveAsFlow()

    private var impressionReported: Boolean = false
    private var impressionStartTime: Long? = null

    private val impressionElapsed: Boolean
        get() = impressionStartTime?.let { startTime ->
            now - startTime >= DefaultImpressionState.DEFAULT_IMPRESSION_DURATION_MS
        } == true
    //endregion fields

    init {
        lifecycle.coroutineScope.launch(Dispatchers.Default) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    // check during each iteration if this impression
                    if (!impressionReported && impressionElapsed) {
                        impressionReported = true
                        impressionChannel.send(Any())
                    }
                    delay(DefaultImpressionState.DEFAULT_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Handles the event where the global position of the corresponding Composable has changed.
     *
     * @param viewGlobalCoordinates the [LayoutCoordinates] of the tracked Composable.
     * @param viewGlobalVisibleRect the [Rect] describing the bounds of the container for the Composable.
     */
    fun onGlobalPositionChanged(
        viewGlobalCoordinates: LayoutCoordinates,
        viewGlobalVisibleRect: Rect
    ) {
        if (impressionReported) {
            // impression already reported - skip calculating impression
            return
        }

        val viewBoundsInWindow = viewGlobalCoordinates.boundsInWindow()

        val visibleTop = maxOf(viewBoundsInWindow.top, viewGlobalVisibleRect.top)
        val visibleBottom = minOf(viewBoundsInWindow.bottom, viewGlobalVisibleRect.bottom)
        val visibleHeight = visibleBottom - visibleTop
        if (visibleHeight < 0) {
            // vertical component of view is off-screen
            return onDisposed()
        }

        val visibleLeft = maxOf(viewBoundsInWindow.left, viewGlobalVisibleRect.left)
        val visibleRight = minOf(viewBoundsInWindow.right, viewGlobalVisibleRect.right)
        val visibleWidth = visibleRight - visibleLeft
        if (visibleWidth < 0) {
            // horizontal component of view is off-screen
            return onDisposed()
        }

        val visibleArea = visibleWidth * visibleHeight
        val componentArea = viewGlobalCoordinates.size.run { width * height }
        val visiblePercentage = visibleArea / componentArea

        impressionStartTime =
            if (visiblePercentage >= DefaultImpressionState.DEFAULT_MINUMUM_VISIBLE_PERCENTAGE) impressionStartTime
                ?: now
            else null
    }

    /**
     * Handles the event where the tracked Composable is no longer on screen.
     */
    fun onDisposed() {
        impressionReported = false
        impressionStartTime = null
    }
}
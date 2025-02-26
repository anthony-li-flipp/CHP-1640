package com.flipp.impressionsandbox.impression

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Minimum amount of time a Composable must remain on screen before an impression is reported.
 */
private const val DEFAULT_IMPRESSION_DURATION_MS: Long = 500L

/**
 * Polling frequency for which impressions are checked/reported.
 */
private const val DEFAULT_CHECK_INTERVAL_MS: Long = 500L

/**
 * Adopted from https://github.com/abema/compose-impression-tracker
 */
@Composable
fun <T : Any> Modifier.impression(
  qualifier: ImpressionQualifier,
  key: T,
  onImpression: (key: T) -> Unit,
): Modifier = with(this) {
  val view = LocalView.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val impressionState = remember { ImpressionState(qualifier, lifecycleOwner.lifecycle) }

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

private class ImpressionState(
  private val qualifier: ImpressionQualifier,
  lifecycle: Lifecycle,
) {
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
      now - startTime >= DEFAULT_IMPRESSION_DURATION_MS
    } == true
  //endregion fields

  init {
    lifecycle.coroutineScope.launch(Dispatchers.Default) {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) {
          // check if Composable is not yet reported an elapsed the minimum time on screen
          if (!impressionReported && impressionElapsed) {
            impressionReported = true
            impressionChannel.send(Any())
          }
          delay(DEFAULT_CHECK_INTERVAL_MS)
        }
      }
    }
  }

  /**
   * Handles the event where the global position of the tracked Composable has changed.
   *
   * @param viewGlobalCoordinates the [LayoutCoordinates] of the tracked Composable.
   * @param viewGlobalVisibleRect the [Rect] describing the container bounds of the tracked Composable.
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
    val visibleHeightPx = (visibleBottom - visibleTop).toInt()
    if (visibleHeightPx < 0) {
      // vertical component of view is off-screen
      return onDisposed()
    }

    val visibleLeft = maxOf(viewBoundsInWindow.left, viewGlobalVisibleRect.left)
    val visibleRight = minOf(viewBoundsInWindow.right, viewGlobalVisibleRect.right)
    val visibleWidthPx = (visibleRight - visibleLeft).toInt()
    if (visibleWidthPx < 0) {
      // horizontal component of view is off-screen
      return onDisposed()
    }

    val globalWidthPx = viewGlobalCoordinates.size.width
    val globalHeightPx = viewGlobalCoordinates.size.height

    impressionStartTime = qualifier.isImpression(
      visibleWidthPx = visibleWidthPx,
      visibleHeightPx = visibleHeightPx,
      globalWidthPx = globalWidthPx,
      globalHeightPx = globalHeightPx
    ).let { isImpression -> if (isImpression) impressionStartTime ?: now else null }
  }

  /**
   * Handles the event where the tracked Composable is no longer on screen.
   */
  fun onDisposed() {
    impressionReported = false
    impressionStartTime = null
  }
}
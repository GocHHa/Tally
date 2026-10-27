package com.tally.counter.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val THRESHOLD_FRACTION = 0.32f

/**
 * Swipe either way to reset. Spring physics on release, a haptic tick when the threshold is
 * crossed, and an accessibility action so it also works without gestures.
 */
@Composable
fun SwipeToReset(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val colors = LocalTallyColors.current
    val offset = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    var pastThreshold by remember { mutableStateOf(false) }
    val currentOnReset by rememberUpdatedState(onReset)

    val threshold = (widthPx * THRESHOLD_FRACTION).coerceAtLeast(1f)
    val progress = (abs(offset.value) / threshold).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Reset count") {
                        currentOnReset()
                        true
                    }
                )
            }
    ) {
        // Revealed behind the card while swiping.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(colors.badgeBottom.copy(alpha = 0.10f + 0.75f * progress))
                .padding(horizontal = 28.dp),
            contentAlignment = if (offset.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    alpha = progress
                    val s = 0.75f + 0.25f * progress
                    scaleX = s
                    scaleY = s
                }
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = colors.onBadge)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Reset",
                    color = colors.onBadge,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { pastThreshold = false },
                        onDragEnd = {
                            val width = size.width.toFloat()
                            if (abs(offset.value) > width * THRESHOLD_FRACTION) {
                                scope.launch {
                                    val target = if (offset.value > 0f) width else -width
                                    offset.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                                    Haptics.confirm(view)
                                    currentOnReset()
                                    // If the row is still here (it normally animates away), slide it back.
                                    delay(450)
                                    offset.snapTo(0f)
                                }
                            } else {
                                scope.launch {
                                    offset.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val next = offset.value + dragAmount
                            scope.launch { offset.snapTo(next) }
                            val crossed = abs(next) > size.width * THRESHOLD_FRACTION
                            if (crossed != pastThreshold) {
                                pastThreshold = crossed
                                Haptics.threshold(view)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

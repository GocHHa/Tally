package com.tally.counter.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.ui.theme.TabularNumbers

enum class BadgeSize { Small, Medium, Large }

/**
 * The star of the show: a bold red pill with tabular digits. The number counts up/down smoothly
 * and the pill does a springy bounce whenever the count increases.
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.Medium
) {
    val colors = LocalTallyColors.current
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(count) }

    LaunchedEffect(count) {
        val before = previous
        previous = count
        if (count > before) {
            scale.animateTo(1.28f, tween(durationMillis = 90))
            scale.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMedium))
        } else {
            scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
        }
    }

    val shown by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "badgeCount"
    )

    val text = when {
        shown > 999 -> "999+"
        else -> shown.toString()
    }
    val style = when (size) {
        BadgeSize.Small -> MaterialTheme.typography.labelMedium
        BadgeSize.Medium -> MaterialTheme.typography.titleMedium
        BadgeSize.Large -> MaterialTheme.typography.headlineSmall
    }.merge(TabularNumbers).copy(fontWeight = FontWeight.ExtraBold)
    val minHeight = when (size) {
        BadgeSize.Small -> 20.dp
        BadgeSize.Medium -> 30.dp
        BadgeSize.Large -> 40.dp
    }
    val horizontal = when (size) {
        BadgeSize.Small -> 6.dp
        BadgeSize.Medium -> 11.dp
        BadgeSize.Large -> 15.dp
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .defaultMinSize(minWidth = minHeight, minHeight = minHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(Brush.verticalGradient(listOf(colors.badgeTop, colors.badgeBottom)))
            .clearAndSetSemantics {
                contentDescription = if (count == 1) "1 unread" else "$count unread"
            }
            .padding(horizontal = horizontal, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = style, color = colors.onBadge, maxLines = 1)
    }
}

package com.tally.counter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics

/**
 * One app on the Home screen: icon with a soft glow tinted by the icon's own colour, name,
 * time of the last notification, and the animated count pill.
 */
@Composable
fun AppCountCard(
    label: String,
    icon: ImageBitmap?,
    accent: Color,
    count: Int,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTallyColors.current
    val view = LocalView.current
    val shape = RoundedCornerShape(24.dp)
    val glowAlpha = colors.glowAlpha

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (colors.isDark) 18.dp else 10.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.55f),
                spotColor = accent.copy(alpha = 0.65f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(x = 48.dp.toPx(), y = size.height / 2f),
                        radius = size.width * 0.6f
                    )
                )
            }
            .border(1.dp, colors.cardStroke, shape)
            .clickable(role = Role.Button, onClickLabel = "Show history") {
                Haptics.tap(view)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Soft halo directly behind the icon.
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.radialGradient(listOf(accent.copy(alpha = glowAlpha * 1.4f), Color.Transparent))
                    )
            )
            AppIcon(icon = icon, label = label, size = 46.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        CountBadge(count = count, size = BadgeSize.Medium, modifier = Modifier.padding(end = 2.dp))
    }
}

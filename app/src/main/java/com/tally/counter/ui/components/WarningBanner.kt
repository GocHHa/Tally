package com.tally.counter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics

/** Clear warning with a fix button. Uses an icon and text, not just colour. */
@Composable
fun WarningBanner(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalTallyColors.current.warning,
    icon: ImageVector = Icons.Filled.Warning
) {
    val shape = RoundedCornerShape(22.dp)
    val view = LocalView.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), shape)
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = "Warning", tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.size(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = LocalTallyColors.current.muted)
        Spacer(Modifier.size(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = {
                    Haptics.tap(view)
                    onAction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Color.Black)
            ) {
                Text(actionLabel)
            }
        }
    }
}

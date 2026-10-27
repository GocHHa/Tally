package com.tally.counter.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics
import kotlinx.coroutines.delay

/** Runs [action] every time the screen resumes (e.g. coming back from system Settings). */
@Composable
fun OnResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(action)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) current()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/** Wall-clock time that refreshes periodically, for "5 min ago" labels. */
@Composable
fun rememberNow(intervalMs: Long = 30_000L): Long {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(intervalMs)
            value = System.currentTimeMillis()
        }
    }
    return now
}

fun relativeTime(time: Long, now: Long): String {
    if (time <= 0L) return "—"
    val diff = now - time
    return when {
        diff < 60_000L -> "Just now"
        else -> DateUtils.getRelativeTimeSpanString(
            time, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}

/** Top bar used by inner screens: back button + large title. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            val view = LocalView.current
            IconButton(onClick = {
                Haptics.tap(view)
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LocalTallyColors.current.muted,
        modifier = modifier
            .padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() }
    )
}

/** Rounded surface with a hairline stroke; the base of every card in the app. */
@Composable
fun TallyCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LocalTallyColors.current.cardStroke, shape)
            .padding(contentPadding),
        content = content
    )
}

/** A settings row with a title, optional description and a switch. The whole row toggles. */
@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Switch) {
                Haptics.toggle(view, !checked)
                onCheckedChange(!checked)
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else LocalTallyColors.current.muted
            )
            if (description != null) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalTallyColors.current.muted
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}

/** Status shown with an icon AND words, never colour alone. */
@Composable
fun StatusPill(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** App icon bitmap, or a lettered circle while loading / when unavailable. */
@Composable
fun AppIcon(icon: ImageBitmap?, label: String, size: Dp, modifier: Modifier = Modifier) {
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

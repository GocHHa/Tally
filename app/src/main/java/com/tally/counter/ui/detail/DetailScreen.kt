package com.tally.counter.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.counter.data.db.HistoryEntity
import com.tally.counter.ui.components.AppIcon
import com.tally.counter.ui.components.BadgeSize
import com.tally.counter.ui.components.CountBadge
import com.tally.counter.ui.components.ScreenHeader
import com.tally.counter.ui.components.SectionTitle
import com.tally.counter.ui.components.SettingSwitchRow
import com.tally.counter.ui.components.TallyCard
import com.tally.counter.ui.components.relativeTime
import com.tally.counter.ui.components.rememberNow
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics

@Composable
fun DetailScreen(viewModel: DetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val colors = LocalTallyColors.current
    val now = rememberNow()
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val visual = state.visual
    val label = visual?.label ?: viewModel.packageName
    val accent = visual?.accent ?: MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = insets.calculateTopPadding() + 4.dp,
            bottom = insets.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") { ScreenHeader(title = "", onBack = onBack) }

        item(key = "hero") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(accent.copy(alpha = colors.glowAlpha * 1.5f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(icon = visual?.icon, label = label, size = 76.dp)
                    }
                    if (state.count > 0) {
                        CountBadge(count = state.count, size = BadgeSize.Large)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        state.count == 0 -> "No unread notifications"
                        state.count == 1 -> "1 unread · last ${relativeTime(state.lastNotificationAt, now).lowercase()}"
                        else -> "${state.count} unread · last ${relativeTime(state.lastNotificationAt, now).lowercase()}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.canLaunch) {
                        Button(onClick = {
                            Haptics.tap(view)
                            if (!viewModel.openApp(context)) {
                                Toast.makeText(context, "Couldn't open $label", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open app")
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            Haptics.confirm(view)
                            viewModel.reset()
                        },
                        enabled = state.count > 0
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset count")
                    }
                }
            }
        }

        item(key = "options") {
            SectionTitle("Counting for this app")
            TallyCard(contentPadding = 12.dp) {
                SettingSwitchRow(
                    title = "Count this app",
                    description = "Turn off to ignore this app completely.",
                    checked = state.counted,
                    onCheckedChange = viewModel::setCounted
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Count",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                CountModeSelector(
                    countMessages = state.countMessages,
                    onChange = {
                        Haptics.tap(view)
                        viewModel.setCountMessages(it)
                    }
                )
                Text(
                    text = "Messages counts each chat message (WhatsApp, Telegram, Messages…). " +
                        "Notifications counts 1 per notification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                SettingSwitchRow(
                    title = "Reset when the app clears its notification",
                    description = if (state.globalAppCancelEnabled) {
                        "Usually means you read it in the app. Turn off if this app clears notifications for other reasons."
                    } else {
                        "Turned off for all apps in Settings."
                    },
                    checked = state.resetOnAppCancel && state.globalAppCancelEnabled,
                    enabled = state.globalAppCancelEnabled,
                    onCheckedChange = viewModel::setResetOnAppCancel
                )
            }
        }

        item(key = "historyTitle") {
            SectionTitle("Recent notifications")
            if (!state.storeText) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Notification text isn't stored. Turn on \"Store notification text\" in Settings to see it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }
        }

        if (state.history.isEmpty()) {
            item(key = "historyEmpty") {
                Text(
                    text = "Nothing in the last 7 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(4.dp)
                )
            }
        } else {
            items(state.history, key = { it.id }) { row ->
                HistoryRow(row = row, now = now)
            }
        }
    }
}

@Composable
fun CountModeSelector(countMessages: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = countMessages,
            onClick = { onChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) { Text("Messages") }
        SegmentedButton(
            selected = !countMessages,
            onClick = { onChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) { Text("Notifications") }
    }
}

@Composable
private fun HistoryRow(row: HistoryEntity, now: Long) {
    val colors = LocalTallyColors.current
    TallyCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.title ?: "Notification",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!row.text.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = row.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = relativeTime(row.postedAt, now),
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted
            )
        }
    }
}

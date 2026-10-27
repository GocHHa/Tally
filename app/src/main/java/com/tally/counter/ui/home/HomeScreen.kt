package com.tally.counter.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.counter.ui.components.AppCountCard
import com.tally.counter.ui.components.OnResume
import com.tally.counter.ui.components.SwipeToReset
import com.tally.counter.ui.components.TallyCard
import com.tally.counter.ui.components.WarningBanner
import com.tally.counter.ui.components.relativeTime
import com.tally.counter.ui.components.rememberNow
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.ui.theme.TabularNumbers
import com.tally.counter.util.Haptics
import com.tally.counter.util.SystemSettings
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val colors = LocalTallyColors.current
    val now = rememberNow()
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    var confirmClearAll by remember { mutableStateOf(false) }

    // Give the listener a moment to connect before warning about it.
    var graceOver by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(3_000)
        graceOver = true
    }

    OnResume { viewModel.onResume() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = insets.calculateTopPadding() + 8.dp,
                bottom = insets.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tally",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )
                    IconButton(onClick = {
                        Haptics.tap(view)
                        onOpenSettings()
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }

            item(key = "summary") {
                SummaryCard(
                    total = state.total,
                    appCount = state.apps.size,
                    onClearAll = {
                        Haptics.tap(view)
                        confirmClearAll = true
                    }
                )
            }

            item(key = "banners") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedVisibility(
                        visible = !state.permissions.notificationAccess,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        WarningBanner(
                            title = "Notification access is off",
                            body = "Tally can't count anything until you allow notification access.",
                            actionLabel = "Turn on",
                            onAction = { SystemSettings.openNotificationAccess(context) }
                        )
                    }
                    AnimatedVisibility(
                        visible = state.permissions.notificationAccess && !state.listenerConnected && graceOver,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        WarningBanner(
                            title = "Tally isn't listening right now",
                            body = "The system stopped Tally's notification listener, so new notifications " +
                                "aren't being counted. Tap Fix to restart it. If this keeps happening, set " +
                                "Tally's battery usage to Unrestricted.",
                            actionLabel = "Fix",
                            onAction = { viewModel.fixListener() }
                        )
                    }
                    AnimatedVisibility(
                        visible = state.permissions.notificationAccess && !state.permissions.batteryUnrestricted,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        WarningBanner(
                            title = "Battery isn't Unrestricted",
                            body = "Samsung may stop Tally in the background. Set battery usage to " +
                                "Unrestricted so counts stay accurate.",
                            actionLabel = "Fix",
                            onAction = { SystemSettings.openBatterySettings(context) },
                            tint = colors.muted
                        )
                    }
                }
            }

            if (state.apps.isEmpty() && !state.loading) {
                item(key = "empty") {
                    EmptyState(
                        hasAccess = state.permissions.notificationAccess,
                        onOpenPermissions = onOpenPermissions
                    )
                }
            }

            items(state.apps, key = { it.visual.packageName }) { row ->
                SwipeToReset(
                    onReset = { viewModel.reset(row.visual.packageName) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(220),
                        placementSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        ),
                        fadeOutSpec = tween(180)
                    )
                ) {
                    AppCountCard(
                        label = row.visual.label,
                        icon = row.visual.icon,
                        accent = row.visual.accent,
                        count = row.count,
                        subtitle = "Last notification ${relativeTime(row.lastNotificationAt, now).replaceFirstChar { it.lowercaseChar() }}",
                        onClick = { onOpenDetail(row.visual.packageName) }
                    )
                }
            }

            if (state.apps.isNotEmpty()) {
                item(key = "hint") {
                    Text(
                        text = "Swipe a card to reset it · Tap for history",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear all counts?") },
            text = { Text("Every app's unread count goes back to zero. Your notification history is kept.") },
            confirmButton = {
                TextButton(onClick = {
                    Haptics.confirm(view)
                    viewModel.clearAll()
                    confirmClearAll = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SummaryCard(total: Int, appCount: Int, onClearAll: () -> Unit) {
    val colors = LocalTallyColors.current
    val shownTotal by animateIntAsState(
        targetValue = total,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "total"
    )
    TallyCard(contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = shownTotal.toString(),
                    style = MaterialTheme.typography.displaySmall.merge(TabularNumbers),
                    fontWeight = FontWeight.Black,
                    color = if (total > 0) colors.badgeTop else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                )
                Text(
                    text = when {
                        total == 0 -> "Nothing unread"
                        appCount == 1 -> "unread in 1 app"
                        else -> "unread across $appCount apps"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            }
            if (total > 0) {
                FilledTonalButton(onClick = onClearAll) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear all")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasAccess: Boolean, onOpenPermissions: () -> Unit) {
    val colors = LocalTallyColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(colors.success.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasAccess) Icons.Filled.Check else Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (hasAccess) colors.success else colors.muted,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (hasAccess) "All caught up" else "Waiting for permission",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hasAccess) {
                "New notifications will show up here — and stay, even after you clear the panel."
            } else {
                "Give Tally notification access so it can start counting."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        if (!hasAccess) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onOpenPermissions) { Text("Set up permissions") }
        }
    }
}

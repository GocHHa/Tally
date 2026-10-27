package com.tally.counter.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.counter.ui.components.OnResume
import com.tally.counter.ui.components.ScreenHeader
import com.tally.counter.ui.components.StatusPill
import com.tally.counter.ui.components.TallyCard
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics
import com.tally.counter.util.SystemSettings

/** Live permission status, reachable from Settings. */
@Composable
fun PermissionsScreen(viewModel: PermissionsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    OnResume { viewModel.refresh() }

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader(title = "Permissions", onBack = onBack) }
        item {
            PermissionList(
                state = state,
                onFixListener = viewModel::fixListener
            )
        }
        item { PrivacyCard() }
    }
}

/** The permission cards, shared with onboarding. */
@Composable
fun PermissionList(state: SettingsUiState, onFixListener: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalTallyColors.current
    val p = state.permissions
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PermissionCard(
            icon = Icons.Filled.Notifications,
            title = "Notification access",
            tag = "Required",
            description = "Lets Tally see notifications as they arrive so it can count them. " +
                "This is the only way an app can know about other apps' notifications.",
            granted = p.notificationAccess,
            grantedLabel = "Allowed",
            missingLabel = "Not allowed",
            actionLabel = if (p.notificationAccess) "Open settings" else "Allow access",
            onAction = { SystemSettings.openNotificationAccess(context) }
        )
        if (p.notificationAccess) {
            PermissionCard(
                icon = Icons.Filled.Refresh,
                title = "Listener status",
                tag = "Live",
                description = if (state.listenerConnected) {
                    "Tally is connected and counting new notifications."
                } else {
                    "The listener isn't connected. Tap Restart; if it keeps disconnecting, set battery to Unrestricted."
                },
                granted = state.listenerConnected,
                grantedLabel = "Running",
                missingLabel = "Not running",
                actionLabel = if (state.listenerConnected) null else "Restart",
                onAction = onFixListener
            )
        }
        PermissionCard(
            icon = Icons.Filled.DateRange,
            title = "Usage access",
            tag = "Optional",
            description = "Lets Tally notice when you open an app, so its count resets even if you " +
                "never touched the notification. Without it, this feature is simply off.",
            granted = p.usageAccess,
            grantedLabel = "Allowed",
            missingLabel = "Off",
            actionLabel = if (p.usageAccess) "Open settings" else "Allow access",
            onAction = { SystemSettings.openUsageAccess(context) },
            missingTint = colors.muted
        )
        PermissionCard(
            icon = Icons.Filled.Settings,
            title = "Battery: Unrestricted",
            tag = "Recommended",
            description = "Samsung puts apps to sleep to save battery, which can stop Tally from " +
                "counting. Unrestricted keeps the listener alive. Tally uses almost no battery.",
            granted = p.batteryUnrestricted,
            grantedLabel = "Unrestricted",
            missingLabel = "Optimised",
            actionLabel = if (p.batteryUnrestricted) "Open app info" else "Set Unrestricted",
            onAction = { SystemSettings.openBatterySettings(context) }
        )
        PermissionCard(
            icon = Icons.Filled.Info,
            title = "Posting notifications",
            tag = "Not needed",
            description = "Tally never posts notifications of its own, so it doesn't ask for this permission.",
            granted = true,
            grantedLabel = "Nothing to do",
            missingLabel = "",
            actionLabel = null,
            onAction = {}
        )
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    tag: String,
    description: String,
    granted: Boolean,
    grantedLabel: String,
    missingLabel: String,
    actionLabel: String?,
    onAction: () -> Unit,
    missingTint: Color = LocalTallyColors.current.warning
) {
    val colors = LocalTallyColors.current
    val view = LocalView.current
    TallyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(tag, style = MaterialTheme.typography.labelMedium, color = colors.muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (granted) {
                StatusPill(text = grantedLabel, icon = Icons.Filled.CheckCircle, color = colors.success)
            } else {
                StatusPill(text = missingLabel, icon = Icons.Filled.Warning, color = missingTint)
            }
            Spacer(Modifier.weight(1f))
            if (actionLabel != null) {
                FilledTonalButton(onClick = {
                    Haptics.tap(view)
                    onAction()
                }) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun PrivacyCard(modifier: Modifier = Modifier) {
    val colors = LocalTallyColors.current
    TallyCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.success)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Your notifications never leave this phone",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Tally has no internet permission. Counts, and history only if you turn it on, are stored on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

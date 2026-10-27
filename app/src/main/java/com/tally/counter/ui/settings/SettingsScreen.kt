package com.tally.counter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.counter.data.settings.ThemeMode
import com.tally.counter.ui.components.OnResume
import com.tally.counter.ui.components.ScreenHeader
import com.tally.counter.ui.components.SectionTitle
import com.tally.counter.ui.components.SettingSwitchRow
import com.tally.counter.ui.components.StatusPill
import com.tally.counter.ui.components.TallyCard
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics
import com.tally.counter.util.SystemSettings

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current
    val view = LocalView.current
    val colors = LocalTallyColors.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    var confirmClear by remember { mutableStateOf(false) }

    OnResume { viewModel.refreshPermissions() }

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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { ScreenHeader(title = "Settings", onBack = onBack) }

        item {
            SectionTitle("Appearance")
            TallyCard {
                Text("Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                val modes = listOf(ThemeMode.DARK to "Dark", ThemeMode.LIGHT to "Light", ThemeMode.SYSTEM to "System")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = settings.theme == mode,
                            onClick = {
                                Haptics.tap(view)
                                viewModel.setTheme(mode)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) { Text(label) }
                    }
                }
            }
        }

        item {
            SectionTitle("Resetting counts")
            TallyCard(contentPadding = 12.dp) {
                SettingSwitchRow(
                    title = "When I tap a notification",
                    description = "Opening a notification resets that app's count.",
                    checked = settings.resetOnClick,
                    onCheckedChange = viewModel::setResetOnClick
                )
                SettingSwitchRow(
                    title = "When an app clears its own notification",
                    description = "Usually means you read it. Can be turned off per app.",
                    checked = settings.resetOnAppCancel,
                    onCheckedChange = viewModel::setResetOnAppCancel
                )
                SettingSwitchRow(
                    title = "When I open the app",
                    description = if (state.permissions.usageAccess) {
                        "Uses Usage access to see that you opened the app after its last notification."
                    } else {
                        "Needs Usage access (optional). Off until you allow it."
                    },
                    checked = settings.resetOnOpen && state.permissions.usageAccess,
                    enabled = state.permissions.usageAccess,
                    onCheckedChange = viewModel::setResetOnOpen
                )
                if (!state.permissions.usageAccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { SystemSettings.openUsageAccess(context) }) {
                            Text("Allow Usage access")
                        }
                    }
                }
                Text(
                    text = "Swiping notifications away or tapping \"Clear\" in the panel never resets a count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }

        item {
            SectionTitle("Apps")
            TallyCard(contentPadding = 4.dp) {
                NavRow(
                    title = "Choose apps",
                    description = "Include or exclude apps, and pick messages vs notifications counting.",
                    onClick = onOpenApps
                )
            }
        }

        item {
            SectionTitle("History")
            TallyCard(contentPadding = 12.dp) {
                SettingSwitchRow(
                    title = "Store notification text",
                    description = "Keeps the title and text of the last 50 notifications per app for 7 days. " +
                        "When off, Tally only stores counts and times.",
                    checked = settings.storeText,
                    onCheckedChange = viewModel::setStoreText
                )
            }
        }

        item {
            SectionTitle("Permissions")
            TallyCard(contentPadding = 4.dp) {
                val p = state.permissions
                val allGood = p.notificationAccess && state.listenerConnected && p.batteryUnrestricted
                NavRow(
                    title = "Permission status",
                    description = when {
                        !p.notificationAccess -> "Notification access is off"
                        !state.listenerConnected -> "Listener not running"
                        !p.batteryUnrestricted -> "Battery isn't Unrestricted"
                        else -> "Everything's set up"
                    },
                    onClick = onOpenPermissions,
                    trailing = {
                        if (allGood) {
                            StatusPill("OK", Icons.Filled.CheckCircle, colors.success)
                        } else {
                            StatusPill("Check", Icons.Filled.Warning, colors.warning)
                        }
                    }
                )
            }
        }

        item {
            SectionTitle("Data")
            TallyCard(contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reset all counts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Sets every app back to zero.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    OutlinedButton(onClick = {
                        Haptics.tap(view)
                        confirmClear = true
                    }) { Text("Reset") }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            PrivacyCard()
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Reset all counts?") },
            text = { Text("Every app's unread count goes back to zero.") },
            confirmButton = {
                TextButton(onClick = {
                    Haptics.confirm(view)
                    viewModel.clearAll()
                    confirmClear = false
                }) { Text("Reset all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NavRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button) {
                Haptics.tap(view)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = LocalTallyColors.current.muted)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
